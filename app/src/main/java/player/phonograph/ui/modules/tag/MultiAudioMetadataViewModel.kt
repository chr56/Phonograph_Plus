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

class MultiAudioMetadataViewModel : AbsMetadataViewModel() {

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
                val displayed = fields.mapValues { "" }
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
        fun modify(context: Context, event: TagEditEvent): State = when (event) {
            is TagEditEvent.AddNewTag     -> {
                val data = displayed + (event.fieldKey to "")
                copy(displayed = data)
            }

            is TagEditEvent.UpdateTag     -> {
                val data = displayed.toMutableMap().also { map ->
                    map[event.fieldKey] = event.newValue
                }
                copy(displayed = data)
            }

            is TagEditEvent.RemoveTag     -> {
                val data = displayed.toMutableMap().also { map ->
                    map.remove(event.fieldKey)
                }
                copy(displayed = data)
            }

            is TagEditEvent.RemoveArtwork -> this
            is TagEditEvent.UpdateArtwork -> this
        }
    }

    fun load(context: Context, songs: Collection<Song>, asOriginal: Boolean) {
        viewModelScope.launch {
            val data = State.from(context, songs.toList())
            if (asOriginal) originalState = data
            _state.emit(data)
        }
    }

    private fun modifyContent(context: Context, event: TagEditEvent) {
        viewModelScope.launch { _state.emit(_state.value?.modify(context, event)) }
    }

    override fun submitEditEvent(context: Context, event: TagEditEvent) {
        viewModelScope.launch {
            if (!editable.value) return@launch
            modifyContent(context, event)
            enqueueEditRequest(
                when (event) {
                    is TagEditEvent.AddNewTag     -> EditAction.Update(event.fieldKey, "")
                    is TagEditEvent.UpdateTag     -> EditAction.Update(event.fieldKey, event.newValue)
                    is TagEditEvent.RemoveTag     -> EditAction.Delete(event.fieldKey)
                    is TagEditEvent.RemoveArtwork -> EditAction.ImageDelete
                    is TagEditEvent.UpdateArtwork -> EditAction.ImageReplace(event.file)
                }
            )
        }
    }

    override fun generateTagDiff(): TagDiff {
        val original = originalState?.metadata
        val tagDiff = pendingEditRequests.map { action ->
            val text =
                original?.mapNotNull { metadata -> metadata.musicMetadata[action.key]?.text() }
                    ?.reduce { a, b -> "$a,$b" } ?: ""
            Pair(action, text.toString())
        }
        return TagDiff(tagDiff)
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
                context, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT
            ).show()
        }

    }

    companion object {
        private const val TAG = "MultiAudioMetadataViewModel"
    }
}