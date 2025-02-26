/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.storage.launcher.ICreateFileStorageAccessible
import player.phonograph.R
import player.phonograph.mechanism.metadata.DefaultMetadataExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toFieldKey
import player.phonograph.mechanism.metadata.edit.JAudioTaggerAudioMetadataEditor
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.EditAction
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.ui.modules.tag.MetadataUIEvent.Edit
import player.phonograph.ui.modules.tag.MetadataUIEvent.ExtractArtwork
import player.phonograph.ui.modules.tag.MetadataUIEvent.Save
import player.phonograph.ui.modules.tag.util.fileName
import player.phonograph.ui.modules.tag.util.loadCover
import player.phonograph.util.lifecycleScopeOrNewOne
import player.phonograph.util.permissions.navigateToStorageSetting
import player.phonograph.util.warning
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TagBrowserActivityViewModel : AbsMetadataViewModel() {

    private var originalState: State? = null
    private val _state: MutableStateFlow<State?> = MutableStateFlow(null)
    val state get() = _state.asStateFlow()

    data class State(
        val song: Song,
        val metadata: AudioMetadata,
        val image: Bitmap?,
        val color: Color?,
    ) {
        companion object {
            suspend fun from(context: Context, song: Song): State {
                val info = JAudioTaggerExtractor.extractSongMetadata(context, song)
                    ?: DefaultMetadataExtractor.extractSongMetadata(context, song)
                val (bitmap, paletteColor) = loadCover(context, song)
                return State(song, info, bitmap, paletteColor)
            }
        }

        /**
         * create new state by [event]
         */
        suspend fun modify(context: Context, event: Edit): State = when (event) {
            is Edit.AddNewTag     -> {
                val audioMetadata = modifyMusicMetadataField { musicMetadata ->
                    musicMetadata.genericTagFields + (event.fieldKey to Metadata.PlainStringField(""))
                }
                copy(metadata = audioMetadata)
            }

            is Edit.UpdateTag     -> {
                val audioMetadata = modifyMusicMetadataField { musicMetadata ->
                    musicMetadata.genericTagFields.toMutableMap().also { genericTagFields ->
                        genericTagFields[event.fieldKey] = Metadata.PlainStringField(event.newValue)
                    }
                }
                copy(metadata = audioMetadata)
            }

            is Edit.RemoveTag     -> {
                val audioMetadata = modifyMusicMetadataField { musicMetadata ->
                    musicMetadata.genericTagFields.toMutableMap().also { genericTagFields ->
                        genericTagFields.remove(event.fieldKey)
                    }
                }
                copy(metadata = audioMetadata)
            }

            is Edit.UpdateArtwork -> {
                val (bitmap, _) = loadCover(context, event.file)
                copy(image = bitmap)
            }

            is Edit.RemoveArtwork -> {
                copy(image = null)
            }

        }

        private fun modifyMusicMetadataField(
            block: (MusicMetadata) -> Map<ConventionalMusicMetadataKey, Metadata.Field>,
        ): AudioMetadata = modifyMusicMetadata { musicMetadata ->
            val fields = block(musicMetadata)
            if (musicMetadata is JAudioTaggerMetadata) {
                musicMetadata.copy(_genericTagFields = fields.mapKeys { it.key.toFieldKey() })
            } else {
                musicMetadata
            }
        }

        private fun modifyMusicMetadata(block: (MusicMetadata) -> MusicMetadata): AudioMetadata =
            metadata.copy(musicMetadata = block(metadata.musicMetadata))
    }

    fun load(context: Context, song: Song, asOriginal: Boolean) {
        viewModelScope.launch {
            val data = State.from(context, song)
            if (asOriginal) originalState = data
            _state.emit(data)
        }
    }

    private fun modifyContent(context: Context, event: Edit) {
        viewModelScope.launch { _state.emit(_state.value?.modify(context, event)) }
    }

    override fun submitEvent(context: Context, event: MetadataUIEvent) {
        viewModelScope.launch {
            if (!editable.value) return@launch
            when (event) {
                Save           -> save(context)
                ExtractArtwork -> extractArtwork(context)
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

    override fun generateMetadataDifference(): MetadataChanges {
        val original = originalState?.metadata?.musicMetadata
        val tagDiff = pendingEditRequests.map { action ->
            val text = if (original != null) original[action.key]?.text().toString() else ""
            Pair(action, text)
        }
        return MetadataChanges(tagDiff)
    }

    override fun save(context: Context) {
        val song = _state.value?.song ?: return
        val songFile = File(song.data)
        if (songFile.canWrite()) {
            val editRequests = pendingEditRequests
            _pendingEditRequests.clear()
            saveJob?.cancel()
            saveJob = CoroutineScope(Dispatchers.Unconfined).launch {
                JAudioTaggerAudioMetadataEditor(listOf(songFile), editRequests).execute(context)
                load(context, song, true)
                exitEditMode()
            }
        } else {
            navigateToStorageSetting(context)
            Toast.makeText(
                context, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun extractArtwork(activity: Context) {
        val currentState = _state.value ?: return
        val image = currentState.image ?: return
        val fileName = fileName(currentState.song)
        if (activity is ICreateFileStorageAccessible) {
            val delegate = activity.createFileStorageAccessDelegate
            delegate.launch("$fileName.jpg") { uri ->
                if (uri != null) {
                    activity.lifecycleScopeOrNewOne().launch {
                        withContext(Dispatchers.IO) {
                            val stream = activity.contentResolver.openOutputStream(uri, "wt")
                            if (stream != null) {
                                stream.buffered(4096).use { outputStream ->
                                    image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                }
                            } else {
                                warning(TAG, "Failed to open File")
                            }
                        }
                    }
                } else {
                    warning(TAG, "Failed to create File")
                }
            }
        }
    }

    private val _prefillsMap: MutableMap<ConventionalMusicMetadataKey, List<String>> = mutableMapOf()
    val prefillsMap get() = _prefillsMap.toMap()
    private val _prefillUpdateKey: MutableState<Int> = mutableIntStateOf(0)
    val prefillUpdateKey get() = _prefillUpdateKey as androidx.compose.runtime.State<Int>

    fun insertPrefill(key: ConventionalMusicMetadataKey, value: String) {
        val newList = (_prefillsMap[key] ?: listOf()) + value
        _prefillsMap.also { it[key] = newList }
        _prefillUpdateKey.value += 1
    }

    fun insertPrefill(key: ConventionalMusicMetadataKey, values: List<String>) {
        val newList = (_prefillsMap[key] ?: listOf()) + values
        _prefillsMap.also { it[key] = newList }
        _prefillUpdateKey.value += 1
    }

    companion object {
        private const val TAG = "AudioMetadataViewModel"
    }

}