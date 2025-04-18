/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.message
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.repo.database.store.PathFilterStore
import player.phonograph.settings.Keys
import player.phonograph.settings.Preference
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.TempPopupContent
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
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
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
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

    val mode: Preference<Boolean> = Setting(context)[Keys.pathFilterExcludeMode]

    private val _paths: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val paths = _paths.asStateFlow()

    fun add(path: String) = addImpl(mode.data, path).also { update(mode) }
    fun remove(path: String) = removeImpl(mode.data, path).also { update(mode) }
    fun clear() = clearImpl(mode.data).also { update(mode) }
    fun refresh() {
        update(mode)
    }

    private val _selectedPath: MutableStateFlow<String?> = MutableStateFlow(null)
    val selectedPath = _selectedPath.asStateFlow()

    fun select(path: String) {
        _selectedPath.value = path
    }


    private val pathFilterStore: PathFilterStore by GlobalContext.get().inject()

    private fun update(preference: Preference<Boolean>) {
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
        val confirmDialogState = rememberMaterialDialogState(false)
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
                    chooseFile(context) { path ->
                        model.select(path)
                        confirmDialogState.show()
                    }
                }
                val actionRefresh = { model.refresh() }
                val actionClear = { model.clear() }
                Row(
                    Modifier
                        .clickable { switchMode() }
                        .padding(vertical = 16.dp)
                ) {
                    Column(Modifier.weight(4f)) {
                        Text(modeText)
                        Text(
                            stringResource(
                                if (mode) R.string.pref_summary_path_filter_excluded_mode
                                else R.string.pref_summary_path_filter_included_mode
                            ),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        mode,
                        null,
                        Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    )
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
        ConfirmDialog(confirmDialogState, modeText, model)
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
        TempPopupContent(dismissPopup = dismissPopup, onClick = onClick) {
            Text(
                text = confirmationText?.invoke() ?: stringResource(contentDescription),
                style = MaterialTheme.typography.button,
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    dialogState: MaterialDialogState,
    modeText: String,
    model: PathFilterPreferenceModel,
) {
    val selectedPath by model.selectedPath.collectAsState()
    MaterialDialog(
        dialogState = dialogState,
        onCloseRequest = { dialogState.hide() },
        buttons = {
            val style = accentColoredButtonStyle()
            positiveButton(stringResource(android.R.string.ok), textStyle = style) {
                val path = selectedPath
                if (!path.isNullOrEmpty()) model.add(path)
            }
            negativeButton(stringResource(android.R.string.cancel), textStyle = style)
        }
    ) {
        title(stringResource(R.string.path_filter_confirmation, modeText))
        message(selectedPath)
    }
}


private fun chooseFile(
    context: Context,
    onSelect: (String) -> Unit,
) {
    val contractTool = (context as? PathSelectorRequester)?.pathSelectorContractTool
    if (contractTool != null) {
        contractTool.launch(null) {
            if (it != null) {
                onSelect(it)
            } else {
                Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
    }
}