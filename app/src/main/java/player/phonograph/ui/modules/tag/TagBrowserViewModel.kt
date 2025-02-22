/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.metadata.DefaultMetadataExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toMusicMetadataKey
import player.phonograph.mechanism.metadata.edit.EditAction
import player.phonograph.mechanism.metadata.edit.applyEdit
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.Metadata
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class TagBrowserViewModel : ViewModel() {

    private val _editable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val editable get() = _editable.asStateFlow()
    fun updateEditable(editable: Boolean) {
        _editable.update { editable }
    }

    private val _song: MutableStateFlow<Song?> = MutableStateFlow(null)
    val song get() = _song.asStateFlow()
    fun updateSong(context: Context, song: Song?) {
        if (song != null && song.data.isNotEmpty()) {
            _song.update { song }
            viewModelScope.launch(Dispatchers.IO) {
                val info = JAudioTaggerExtractor.extractSongMetadata(context, song)
                    ?: DefaultMetadataExtractor.extractSongMetadata(context, song)
                val (bitmap, paletteColor) = loadCover(context, song)
                _originalSongMetadata.emit(info)
                _currentSongMetadata.emit(info)
                _songBitmap.emit(bitmap)
                _color.emit(paletteColor)
            }
        }
    }

    private val _originalSongMetadata: MutableStateFlow<AudioMetadata> = MutableStateFlow(AudioMetadata.EMPTY)
    val originalSongMetadata get() = _originalSongMetadata.asStateFlow()
    private val _currentSongMetadata: MutableStateFlow<AudioMetadata> = MutableStateFlow(AudioMetadata.EMPTY)
    val currentSongMetadata get() = _currentSongMetadata.asStateFlow()

    private val _songBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val songBitmap get() = _songBitmap.asStateFlow()

    private val _color: MutableStateFlow<Color?> = MutableStateFlow(null)
    val color get() = _color.asStateFlow()

    fun saveArtwork(activity: Context) {
        val bitmap = songBitmap.value ?: return
        val song = song.value ?: return
        saveArtwork(viewModelScope, activity, bitmap, fileName(song))
    }


    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)


    private var _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    val pendingEditRequests: List<EditAction> get() = _pendingEditRequests.toList()

    fun process(context: Context, event: TagEditEvent) {
        viewModelScope.launch {
            when (event) {
                is TagEditEvent.UpdateTag     -> {
                    modifyView { old ->
                        old.toMutableMap().apply { put(event.fieldKey, Metadata.PlainStringField(event.newValue)) }
                    }
                    modifyEditRequest(EditAction.Update(event.fieldKey, event.newValue))
                }

                is TagEditEvent.AddNewTag     -> {
                    modifyView { old ->
                        old + (event.fieldKey to Metadata.PlainStringField(""))
                    }
                    modifyEditRequest(EditAction.Update(event.fieldKey, ""))
                }

                is TagEditEvent.RemoveTag     -> {
                    modifyView { old ->
                        old.toMutableMap().apply { remove(event.fieldKey) }
                    }
                    modifyEditRequest(EditAction.Delete(event.fieldKey))
                }

                is TagEditEvent.UpdateArtwork -> {
                    val (bitmap, _) = loadCover(context, event.file)
                    _songBitmap.emit(bitmap)
                    modifyEditRequest(EditAction.ImageReplace(event.file))
                }

                TagEditEvent.RemoveArtwork    -> {
                    _songBitmap.emit(null)
                    modifyEditRequest(EditAction.ImageDelete)
                }
            }
        }
    }


    fun mergeActions() {
        _pendingEditRequests = EditAction.merge(_pendingEditRequests)
    }

    private fun modifyEditRequest(action: EditAction): Boolean {
        return if (editable.value) {
            _pendingEditRequests.add(action)
            true
        } else {
            false
        }
    }

    private fun modifyView(action: (old: Map<FieldKey, Metadata.Field>) -> Map<FieldKey, Metadata.Field>) {
        viewModelScope.launch(Dispatchers.Default) {
            val oldAudioMetadata = currentSongMetadata.value
            val oldMusicMetadata = oldAudioMetadata.musicMetadata as JAudioTaggerMetadata
            val newFields = action(oldMusicMetadata.genericTagFields)
            val newMusicMetadata = JAudioTaggerMetadata(newFields, oldMusicMetadata.allTagFields)
            val new = oldAudioMetadata.copy(musicMetadata = newMusicMetadata)
            _currentSongMetadata.emit(new)
        }
    }


    private val _prefillsMap: MutableMap<FieldKey, List<String>> = mutableMapOf()
    val prefillsMap get() = _prefillsMap.toMap()
    private val _prefillUpdateKey: MutableState<Int> = mutableStateOf(0)
    val prefillUpdateKey get() = _prefillUpdateKey as State<Int>

    fun insertPrefill(key: FieldKey, value: String) {
        val newList = (_prefillsMap[key] ?: listOf()) + value
        _prefillsMap.also { it[key] = newList }
        _prefillUpdateKey.value += 1
    }

    fun insertPrefill(key: FieldKey, values: List<String>) {
        val newList = (_prefillsMap[key] ?: listOf()) + values
        _prefillsMap.also { it[key] = newList }
        _prefillUpdateKey.value += 1
    }

    internal fun diff(): TagDiff {
        mergeActions()
        val original = originalSongMetadata.value
        val tagDiff = pendingEditRequests.map { action ->
            val text: CharSequence? = original.musicMetadata[action.key.toMusicMetadataKey()]?.text()
            Pair(action, text?.toString())
        }
        return TagDiff(tagDiff)
    }

    internal fun save(context: Context) {
        val song = song.value ?: return
        val songFile = File(song.data)
        if (songFile.canWrite()) {
            mergeActions()
            applyEdit(
                scope = CoroutineScope(Dispatchers.Unconfined),
                context = context,
                songFile = songFile,
                editRequests = pendingEditRequests,
                needDeleteCover = false,
                needReplaceCover = false,
                newCoverUri = null
            ) {
                updateEditable(false)
                updateSong(context, song)
                _pendingEditRequests.clear()
            }
        } else {
            navigateToStorageSetting(context)
            Toast.makeText(
                context, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT
            ).show()
        }
    }

}