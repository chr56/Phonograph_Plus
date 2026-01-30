/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.settings.TagSeparatorsSetting
import player.phonograph.settings.TagSeparatorsSetting.Companion.TARGET_ABBR_FEATURES_ARTISTS
import player.phonograph.settings.TagSeparatorsSetting.Companion.TARGET_SEPARATORS_ARTISTS
import player.phonograph.settings.TagSeparatorsSetting.Companion.TARGET_SEPARATORS_GENRES
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.LimitedDialog
import player.phonograph.ui.compose.components.StringValuesEditor
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class TagSeparatorsEditorDialog : ComposeViewDialogFragment() {

    class FeaturesArtistsAbbrEditor : TagSeparatorsEditorDialog() {
        override val viewModel: TagSeparatorsEditorViewModel by viewModels {
            TagSeparatorsEditorViewModel.Factory(requireActivity().application, TARGET_ABBR_FEATURES_ARTISTS)
        }
    }

    class ArtistsSeparatorsEditor : TagSeparatorsEditorDialog() {
        override val viewModel: TagSeparatorsEditorViewModel by viewModels {
            TagSeparatorsEditorViewModel.Factory(requireActivity().application, TARGET_SEPARATORS_ARTISTS)
        }
    }

    class GenreSeparatorsEditor : TagSeparatorsEditorDialog() {
        override val viewModel: TagSeparatorsEditorViewModel by viewModels {
            TagSeparatorsEditorViewModel.Factory(requireActivity().application, TARGET_SEPARATORS_GENRES)
        }
    }

    protected abstract val viewModel: TagSeparatorsEditorViewModel

    @Composable
    override fun Content() {

        val paths: List<String> by viewModel.separators.collectAsState()

        val title = when (viewModel.target) {
            TARGET_ABBR_FEATURES_ARTISTS -> "Features Artists Abbreviations"
            TARGET_SEPARATORS_ARTISTS    -> "Artists Separators"
            TARGET_SEPARATORS_GENRES     -> "Genres Separators"
            else                         -> "N/A"
        }

        val description = when (viewModel.target) {
            TARGET_ABBR_FEATURES_ARTISTS -> "TARGET_ABBR_FEATURES_ARTISTS"
            TARGET_SEPARATORS_ARTISTS    -> "TARGET_SEPARATORS_ARTISTS"
            TARGET_SEPARATORS_GENRES     -> "TARGET_SEPARATORS_GENRES"
            else                         -> ""
        }

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
                        actionAdd = { viewModel.add(requireActivity(), "#") },
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

    class TagSeparatorsEditorViewModel(context: Context, val target: Char) : ViewModel() {

        private val separatorsSetting = TagSeparatorsSetting(target)

        private val _separators = MutableStateFlow<List<String>>(emptyList())
        val separators get() = _separators.asStateFlow()

        init {
            refresh(context)
        }

        fun refresh(context: Context) {
            viewModelScope.launch(Dispatchers.IO) {
                _separators.value = separatorsSetting.target(target).read(context).toList()
            }
        }

        fun add(context: Context, item: String) {
            viewModelScope.launch(Dispatchers.IO) {
                separatorsSetting.target(target).add(context, item)
                refresh(context)
            }
        }

        fun remove(context: Context, item: String) {
            viewModelScope.launch(Dispatchers.IO) {
                separatorsSetting.target(target).remove(context, item)
                refresh(context)
            }
        }

        fun edit(context: Context, from: String, to: String) {
            viewModelScope.launch(Dispatchers.IO) {
                separatorsSetting.target(target).edit(context, from, to)
                refresh(context)
            }
        }

        fun clear(context: Context) {
            viewModelScope.launch(Dispatchers.IO) {
                separatorsSetting.target(target).clear(context)
                refresh(context)
            }
        }

        fun reset(context: Context) {
            viewModelScope.launch(Dispatchers.IO) {
                separatorsSetting.target(target).reset(context)
                refresh(context)
            }
        }

        class Factory(
            private val application: Application,
            private val target: Char,
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TagSeparatorsEditorViewModel::class.java)) {
                    return TagSeparatorsEditorViewModel(application, target) as T
                }
                throw IllegalArgumentException()
            }
        }
    }


}