/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.repo.database.PathFilterStore
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitivePreference
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PathFilterPreferenceDialog : ComposeViewDialogFragment() {

    private lateinit var model: PathFilterPreferenceModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model = PathFilterPreferenceModel(requireContext())
        model.refresh()
        super.onViewCreated(view, savedInstanceState)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        MainContent(context, model, ::dismiss)
    }
}

private class PathFilterPreferenceModel(context: Context) {

    val mode: PrimitivePreference<Boolean> = Setting(context)[Keys.pathFilterExcludeMode]

    private val _paths: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val paths = _paths.asStateFlow()

    fun add(path: String) = addImpl(mode.data, path).also { update(mode) }
    fun remove(path: String) = removeImpl(mode.data, path).also { update(mode) }
    fun clear() = clearImpl(mode.data).also { update(mode) }
    fun refresh() {
        update(mode)
    }

    private val pathFilterStore: PathFilterStore by GlobalContext.get().inject()

    private fun update(preference: PrimitivePreference<Boolean>) {
        _paths.tryEmit(with(pathFilterStore) { if (preference.data) blacklistPaths else whitelistPaths })
    }

    private fun addImpl(mode: Boolean, path: String) =
        with(pathFilterStore) { if (mode) addBlacklistPath(path) else addWhitelistPath(path) }

    private fun removeImpl(mode: Boolean, path: String) =
        with(pathFilterStore) { if (mode) removeBlacklistPath(path) else removeWhitelistPath(path) }

    private fun clearImpl(mode: Boolean) =
        with(pathFilterStore) { if (mode) clearBlacklist() else clearWhitelist() }

}

@Composable
private fun MainContent(context: Context, model: PathFilterPreferenceModel, dismiss: () -> Unit) {
    PhonographTheme {
        val mode: Boolean by model.mode.flow.collectAsState(false)
        val paths: List<String> by model.paths.collectAsState(emptyList())
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        val switchMode: () -> Job = {
            coroutineScope.launch {
                model.mode.edit { !mode }
                model.refresh()
            }
        }
        val modeText: String = stringResource(if (mode) R.string.excluded_paths else R.string.included_paths)
        MaterialDialog(
            dialogState = rememberMaterialDialogState(true),
            onCloseRequest = { dismiss() },
        ) {
            title(stringResource(R.string.path_filter))
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                val actionAdd = {
                    PathFilterFolderChooserDialog().show(
                        (context as FragmentActivity).supportFragmentManager,
                        "FOLDER_CHOOSER"
                    )
                    dismiss()
                }
                val actionRefresh = { model.refresh() }
                val actionClear = { model.clear() }
                Row(
                    Modifier
                        .clickable { switchMode() }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        if (mode) stringResource(R.string.excluded_paths) else stringResource(R.string.included_paths),
                        Modifier.weight(4f)
                    )
                    Switch(mode, null, Modifier.weight(1f))
                }
                Row {
                    ActionButton(
                        contentDescription = R.string.add_action,
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(2f),
                        onClick = actionAdd
                    )
                    ActionButton(
                        contentDescription = R.string.refresh,
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.weight(2f),
                        onClick = actionRefresh
                    )
                    Spacer(modifier = Modifier.weight(3f))
                    ActionButton(
                        contentDescription = R.string.clear_action,
                        icon = Icons.Default.Delete,
                        modifier = Modifier.weight(2f),
                        confirmationText = {
                            "${stringResource(R.string.clear_action)}\n$modeText"
                        },
                        onClick = actionClear
                    )
                }
                for (path in paths) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            path,
                            Modifier
                                .weight(4f)
                                .align(Alignment.CenterVertically),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        ActionButton(
                            contentDescription = R.string.delete_action,
                            icon = Icons.Default.Close,
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            confirmationText = {
                                "${stringResource(R.string.delete_action)}($modeText)\n$path"
                            }
                        ) {
                            model.remove(path)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } // Column
        } // Dialog
    }
}

/**
 * @param confirmationText tips for confirmation, null if disable
 */
@Composable
private fun ActionButton(
    contentDescription: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    confirmationText: (@Composable () -> String)? = null,
    onClick: () -> Unit,
) {
    var showPopup: Boolean by remember { mutableStateOf(false) }
    val dismissPopup = { showPopup = false }
    TextButton(
        onClick = {
            if (confirmationText != null) showPopup = !showPopup else onClick()
        },
        modifier
    ) {
        Icon(icon, stringResource(contentDescription), tint = MaterialTheme.colors.secondary)
    }
    if (showPopup) Popup(onDismissRequest = dismissPopup) {
        Surface(elevation = 2.dp) {
            Column(Modifier.width(IntrinsicSize.Max)) {
                Text(
                    text = confirmationText?.invoke() ?: stringResource(contentDescription),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.button,
                )
                Row(
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    TextButton(dismissPopup) {
                        Text(
                            stringResource(android.R.string.cancel),
                            style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton({ dismissPopup(); onClick() }) {
                        Text(
                            stringResource(android.R.string.ok),
                            style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                        )
                    }
                }
            }
        }
    }
}