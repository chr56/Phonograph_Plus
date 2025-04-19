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
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import player.phonograph.ui.modules.setting.elements.PathFilterSettings
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
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
        val mode: Boolean by model.mode.flow.collectAsState(false)
        val paths: List<String> by model.paths.collectAsState(emptyList())
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        val switchMode: () -> Unit = {
            coroutineScope.launch {
                model.mode.edit { !mode }
                model.refresh()
            }
        }
        val modeText: String = stringResource(if (mode) R.string.excluded_paths else R.string.included_paths)
        val confirmDialogState = rememberMaterialDialogState(false)
        val actionAdd = {
            chooseFile(context) { path ->
                model.select(path)
                confirmDialogState.show()
            }
        }
        val actionRefresh = { model.refresh() }
        val actionClear = { model.clear() }
        val actionRemove = { path: String -> model.remove(path) }
        PhonographTheme {
            MaterialDialog(
                dialogState = rememberMaterialDialogState(true),
                onCloseRequest = { dismiss() },
            ) {
                title(stringResource(R.string.path_filter))
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PathFilterSettings(
                        mode = mode,
                        switchMode = switchMode,
                        paths = paths,
                        actionAdd = actionAdd,
                        actionRefresh = actionRefresh,
                        actionClear = actionClear,
                        actionRemove = actionRemove,
                    )
                } // Column
            } // Dialog
            ConfirmDialog(confirmDialogState, modeText, model)
        }
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