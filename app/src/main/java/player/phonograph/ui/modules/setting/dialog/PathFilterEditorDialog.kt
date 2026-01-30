/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.settings.PathFilterSetting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.LimitedDialog
import player.phonograph.ui.compose.components.StringValuesEditor
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Application
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class PathFilterEditorDialog : ComposeViewDialogFragment() {

    class ExcludedMode : PathFilterEditorDialog() {
        override val viewModel: PathFilterViewModel by viewModels {
            PathFilterViewModel.Factory(requireActivity().application, true)
        }
    }

    class IncludedMode : PathFilterEditorDialog() {
        override val viewModel: PathFilterViewModel by viewModels {
            PathFilterViewModel.Factory(requireActivity().application, false)
        }
    }

    protected abstract val viewModel: PathFilterViewModel

    @Composable
    override fun Content() {

        val paths: List<String> by viewModel.paths.collectAsState()

        val title = stringResource(
            if (viewModel.excludeMode) R.string.label_excluded_paths
            else R.string.label_included_paths
        )

        val description = stringResource(
            if (viewModel.excludeMode) R.string.pref_summary_path_filter_excluded_mode
            else R.string.pref_summary_path_filter_included_mode
        )

        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                BoxWithConstraints {
                    StringValuesEditor(
                        modifier = Modifier
                            .heightIn(min = this.maxHeight * 0.6666f)
                            .verticalScroll(rememberScrollState()),
                        title = title,
                        textDescription = description,
                        values = paths,
                        onDismissRequest = ::dismiss,
                        actionAdd = { viewModel.add(requireActivity()) },
                        actionRefresh = { viewModel.refresh(requireActivity()) },
                        actionClear = { viewModel.clear(requireActivity()) },
                        actionRemove = { target -> viewModel.remove(requireActivity(), target) },
                        actionReset = { viewModel.reset(requireActivity()) },
                        actionEdit = { from, to -> viewModel.edit(requireActivity(), from, to) },
                    )
                }
            }
        }
    }

    class PathFilterViewModel(context: Context, val excludeMode: Boolean) : ViewModel() {

        private val pathFilterSetting = PathFilterSetting(excludeMode)

        private val _paths = MutableStateFlow<List<String>>(emptyList())
        val paths get() = _paths.asStateFlow()

        init {
            refresh(context)
        }

        fun refresh(context: Context) {
            viewModelScope.launch(Dispatchers.IO) {
                _paths.value = pathFilterSetting.mode(excludeMode).read(context).toList()
            }
        }

        fun add(context: Context) {
            chooseFile(context) { selected ->
                add(context, selected)
            }
        }


        fun add(context: Context, path: String) {
            viewModelScope.launch(Dispatchers.IO) {
                pathFilterSetting.mode(excludeMode).add(context, path)
                refresh(context)
            }
        }

        fun remove(context: Context, path: String) {
            viewModelScope.launch(Dispatchers.IO) {
                pathFilterSetting.mode(excludeMode).remove(context, path)
                refresh(context)
            }
        }

        fun edit(context: Context, from: String, to: String) {
            viewModelScope.launch(Dispatchers.IO) {
                pathFilterSetting.mode(excludeMode).edit(context, from, to)
                refresh(context)
            }
        }

        fun clear(context: Context) {
            viewModelScope.launch(Dispatchers.IO) {
                pathFilterSetting.mode(excludeMode).clear(context)
                refresh(context)
            }
        }

        fun reset(context: Context) {
            viewModelScope.launch(Dispatchers.IO) {
                pathFilterSetting.mode(excludeMode).reset(context)
                refresh(context)
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
                        Toast.makeText(context, R.string.msg_empty, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
            }
        }

        class Factory(
            private val application: Application,
            private val excludeMode: Boolean,
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PathFilterViewModel::class.java)) {
                    return PathFilterViewModel(application, excludeMode) as T
                }
                throw IllegalArgumentException()
            }
        }
    }


}