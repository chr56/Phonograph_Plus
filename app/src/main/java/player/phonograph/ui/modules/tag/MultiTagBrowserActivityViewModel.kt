/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.R
import player.phonograph.mechanism.metadata.edit.JAudioTaggerAudioMetadataEditor
import player.phonograph.mechanism.metadata.read.MetadataExtractors
import player.phonograph.mechanism.metadata.read.SongDetailCollection
import player.phonograph.model.Song
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.EditAction
import player.phonograph.model.metadata.InteractiveAction
import player.phonograph.model.metadata.InteractiveAction.Edit
import player.phonograph.model.metadata.InteractiveAction.ExtractArtwork
import player.phonograph.model.metadata.InteractiveAction.Save
import player.phonograph.model.metadata.Metadata
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
        val details: SongDetailCollection,
        val displayed: Map<ConventionalMusicMetadataKey, String>,
    ) {
        val songs get() = details.raw.keys
        val metadata get() = details.raw.values
        val keys get() = details.fields.keys

        companion object {
            fun from(context: Context, songs: List<Song>): State {
                val details = MetadataExtractors.extractMetadata(context, songs)
                val displayed = reduceToDisplayable(context, details.fields)
                return State(details, displayed)
            }

            private fun reduceToDisplayable(
                context: Context,
                fields: Map<ConventionalMusicMetadataKey, List<Metadata.Field>>,
            ): Map<ConventionalMusicMetadataKey, String> = fields.mapValues { (_, values) ->
                val set = values.toSet()
                if (set.size == 1) display(context, values.first()) else ""
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

    override fun submitEvent(context: Context, event: InteractiveAction) {
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
                            is Edit.UpdateArtwork -> EditAction.ImageReplace(event.path)
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