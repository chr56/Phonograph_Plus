/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.R
import player.phonograph.mechanism.metadata.DefaultMetadataExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.mechanism.metadata.edit.JAudioTaggerAudioMetadataEditor
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.EditAction
import player.phonograph.model.metadata.Metadata
import player.phonograph.ui.modules.tag.MetadataUIEvent.Edit
import player.phonograph.ui.modules.tag.MetadataUIEvent.ExtractArtwork
import player.phonograph.ui.modules.tag.MetadataUIEvent.Save
import player.phonograph.ui.modules.tag.util.display
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MultiTagBrowserActivityViewModel : AbsMetadataViewModel() {

    private var originalState: State? = null
    private val _state: MutableStateFlow<State?> = MutableStateFlow(null)
    val state get() = _state.asStateFlow()

    data class State(
        val raw: Map<Song, AudioMetadata>,
        val fields: Map<ConventionalMusicMetadataKey, List<Metadata.Field>>,
        val displayed: Map<ConventionalMusicMetadataKey, String>,
    ) {
        val songs get() = raw.keys
        val metadata get() = raw.values

        companion object {
            fun from(context: Context, songs: List<Song>): State {
                val items = songs.associateWith { song ->
                    JAudioTaggerExtractor.extractSongMetadata(context, song)
                        ?: DefaultMetadataExtractor.extractSongMetadata(context, song)
                }
                val fields = reducedTagFields(items.values)
                val displayed = fields.mapValues { (_, values) ->
                    val set = values.toSet()
                    if (set.size == 1) display(context, values.first()) else ""
                }
                return State(items, fields, displayed)
            }

            private fun reducedTagFields(all: Collection<AudioMetadata>)
                    : Map<ConventionalMusicMetadataKey, List<Metadata.Field>> =
                all.fold(mutableMapOf()) { acc, model ->
                    val musicMetadata = model.musicMetadata
                    if (musicMetadata is JAudioTaggerMetadata)
                        for ((key, value) in musicMetadata.textTagFields) {
                            val oldValue = acc[key]
                            val newValue = if (oldValue != null) {
                                oldValue + listOf(value)
                            } else {
                                listOf(value)
                            }
                            acc[key] = newValue
                        }
                    acc
                }
        }

        @Suppress("UNUSED_PARAMETER")
        fun modify(context: Context, event: Edit): State = when (event) {
            is Edit.AddNewTag     -> {
                val data = displayed + (event.fieldKey to "")
                copy(displayed = data)
            }

            is Edit.UpdateTag     -> {
                val data = displayed.toMutableMap().also { map ->
                    map[event.fieldKey] = event.newValue
                }
                copy(displayed = data)
            }

            is Edit.RemoveTag     -> {
                val data = displayed.toMutableMap().also { map ->
                    map.remove(event.fieldKey)
                }
                copy(displayed = data)
            }

            is Edit.RemoveArtwork -> this
            is Edit.UpdateArtwork -> this
        }
    }

    fun load(context: Context, songs: Collection<Song>, asOriginal: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = State.from(context, songs.toList())
            if (asOriginal) originalState = data
            _state.emit(data)
        }
    }

    private fun modifyContent(context: Context, event: Edit) {
        viewModelScope.launch(Dispatchers.IO) { _state.emit(_state.value?.modify(context, event)) }
    }

    override fun submitEvent(context: Context, event: MetadataUIEvent) {
        viewModelScope.launch {
            if (!editable.value) return@launch
            when (event) {
                Save           -> save(context)
                ExtractArtwork -> {}
                is Edit        -> {
                    modifyContent(context, event)
                    enqueueEditRequest(
                        when (event) {
                            is Edit.AddNewTag     -> EditAction.Update(event.fieldKey, "")
                            is Edit.UpdateTag     -> EditAction.Update(event.fieldKey, event.newValue)
                            is Edit.RemoveTag     -> EditAction.Delete(event.fieldKey)
                            is Edit.RemoveArtwork -> EditAction.ImageDelete
                            is Edit.UpdateArtwork -> EditAction.ImageReplace(event.file)
                        }
                    )
                }
            }
        }
    }

    override fun generateMetadataDifference(context: Context): MetadataChanges {
        val original = originalState?.metadata
        val tagDiff = pendingEditRequests.map { action ->
            val text =
                original?.mapNotNull { metadata ->
                    metadata.musicMetadata[action.key]?.let { display(context, it) }
                }?.fold("") { a, b -> "$a\n$b" } ?: ""
            Pair(action, text)
        }
        return MetadataChanges(tagDiff)
    }

    override fun save(context: Context) {
        val songs = state.value?.songs ?: return
        if (songs.isEmpty()) return
        val songFiles = songs.map { File(it.data) }
        if (songFiles.first().canWrite()) {
            val editRequests = pendingEditRequests
            _pendingEditRequests.clear()
            saveJob?.cancel()
            saveJob = CoroutineScope(Dispatchers.Unconfined).launch {
                JAudioTaggerAudioMetadataEditor(songFiles, editRequests).execute(context)
                load(context, songs, true)
                exitEditMode()
            }
        } else {
            navigateToStorageSetting(context)
            Toast.makeText(
                context, R.string.err_permission_manage_external_storage_denied, Toast.LENGTH_SHORT
            ).show()
        }

    }

    companion object {
        private const val TAG = "MultiAudioMetadataViewModel"
    }
}