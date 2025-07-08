/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.settings.PathFilterSetting
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class PathFilterEditorDialog : ComposeViewDialogFragment() {

    class ExcludedMode : PathFilterEditorDialog() {
        override val mode: Boolean = true
    }

    class IncludedMode : PathFilterEditorDialog() {
        override val mode: Boolean = false
    }

    protected abstract val mode: Boolean

    @Composable
    override fun Content() {

        val context = LocalContext.current
        var paths: List<String> by remember { mutableStateOf(emptyList()) }

        var version by remember { mutableIntStateOf(0) }
        LaunchedEffect(version) {
            paths = read(context, mode)
        }

        val actionAdd: () -> Unit = {
            chooseFile(context) { path ->
                add(mode, path)
                version++
            }
        }
        val actionRefresh: () -> Unit = {
            version++
        }
        val actionClear: () -> Unit = {
            clear(mode)
            version++
        }
        val actionRemove: (String) -> Unit = { path: String ->
            remove(mode, path)
            version++
        }

        val description = stringResource(
            if (mode) R.string.pref_summary_path_filter_excluded_mode
            else R.string.pref_summary_path_filter_included_mode
        )

        PhonographTheme {
            MaterialDialog(
                dialogState = rememberMaterialDialogState(true),
                onCloseRequest = { dismiss() },
                buttons = {
                    positiveButton(
                        res = android.R.string.ok, textStyle = accentColoredButtonStyle()
                    ) { dismiss() }
                }
            ) {
                title(stringResource(R.string.path_filter))
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PathFilterSettings(
                        textDescription = description,
                        paths = paths,
                        actionAdd = actionAdd,
                        actionRefresh = actionRefresh,
                        actionClear = actionClear,
                        actionRemove = actionRemove,
                    )
                }
            }
        }
    }

    private suspend fun read(context: Context, mode: Boolean): List<String> =
        PathFilterSetting.read(context, mode)

    private fun add(mode: Boolean, path: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            PathFilterSetting.add(requireContext(), mode, path)
        }

    private fun remove(mode: Boolean, path: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            PathFilterSetting.remove(requireContext(), mode, path)
        }

    private fun edit(mode: Boolean, from: String, to: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            PathFilterSetting.edit(requireContext(), mode, from, to)
        }

    private fun clear(mode: Boolean) =
        lifecycleScope.launch(Dispatchers.IO) {
            PathFilterSetting.clear(requireContext(), mode)
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
                    Toast.makeText(context, R.string.msg_empty, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
        }
    }
}