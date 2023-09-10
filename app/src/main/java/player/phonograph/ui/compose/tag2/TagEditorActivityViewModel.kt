/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import com.vanpra.composematerialdialogs.MaterialDialogState
import org.jaudiotagger.tag.FieldKey
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.mechanism.tag.EditAction
import player.phonograph.mechanism.tag.edit.applyEdit
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.TagData
import player.phonograph.model.TagField
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.io.File

class TagEditorActivityViewModel : ViewModel() {

    private val _editable: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val editable get() = _editable.asStateFlow()

    private val _song: MutableStateFlow<Song> = MutableStateFlow(Song.EMPTY_SONG)
    val song get() = _song.asStateFlow()
    fun updateSong(context: Context, song: Song) {
        if (song != Song.EMPTY_SONG)
            readSongInfo(context, _song.updateAndGet { song })
    }

    private val _originalSongInfo: MutableStateFlow<SongInfoModel> = MutableStateFlow(SongInfoModel.EMPTY())
    val originalSongInfo get() = _originalSongInfo.asStateFlow()
    private val _currentSongInfo: MutableStateFlow<SongInfoModel> = MutableStateFlow(SongInfoModel.EMPTY())
    val currentSongInfo get() = _currentSongInfo.asStateFlow()

    private val _songBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val songBitmap get() = _songBitmap.asStateFlow()

    private val _color: MutableStateFlow<Color?> = MutableStateFlow(null)
    val color get() = _color.asStateFlow()

    private fun readSongInfo(context: Context, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = loadSongInfo(song)
            readBitmap(context, song)
            _originalSongInfo.emit(info)
            _currentSongInfo.emit(info)
        }
    }

    private fun readBitmap(context: Context, data: Any) {
        loadImage(context) {
            data(data)
            parameters(PARAMETERS_RAW)
            target(
                PaletteTargetBuilder(context)
                    .onResourceReady { result: Drawable, paletteColor: Int ->
                        _songBitmap.tryEmit(result.toBitmap())
                        _color.tryEmit(Color(paletteColor))
                    }
                    .build()
            )
        }
    }

    fun saveArtwork(activity: Context) {
        val bitmap = songBitmap.value ?: return
        val fileName = fileName(song.value)
        saveArtwork(viewModelScope, activity, bitmap, fileName)
    }

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)


    private var _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    val pendingEditRequests: List<EditAction> get() = _pendingEditRequests.toList()

    fun process(event: TagInfoTableEvent) {
        viewModelScope.launch {
            when (event) {
                is TagInfoTableEvent.UpdateTag -> {
                    modifyView { old ->
                        old.apply { put(event.fieldKey, TagField(event.fieldKey, TagData.TextData(event.newValue))) }
                    }
                    modifyEditRequest(EditAction.Update(event.fieldKey, event.newValue))
                }

                is TagInfoTableEvent.AddNewTag -> {
                    modifyView { old ->
                        old + (event.fieldKey to TagField(event.fieldKey, TagData.TextData("")))
                    }
                    modifyEditRequest(EditAction.Update(event.fieldKey, ""))
                }

                is TagInfoTableEvent.RemoveTag -> {
                    modifyView { old ->
                        old.apply { remove(event.fieldKey) }
                    }
                    modifyEditRequest(EditAction.Delete(event.fieldKey))
                }

                is TagInfoTableEvent.UpdateArtwork -> {
                    readBitmap(App.instance, event.file)
                    modifyEditRequest(EditAction.ImageReplace(event.file))

                }

                TagInfoTableEvent.RemoveArtwork -> {
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

    private fun modifyView(action: (old: MutableMap<FieldKey, TagField>) -> Map<FieldKey, TagField>) {
        viewModelScope.launch(Dispatchers.Default) {
            val old = currentSongInfo.value
            val newTagFields: Map<FieldKey, TagField> = action(old.tagFields.toMutableMap())
            val new = old.copy(tagFields = newTagFields)
            _currentSongInfo.emit(new)
        }
    }

    internal fun diff(): TagDiff {
        mergeActions()
        val original = originalSongInfo.value
        val tagDiff = pendingEditRequests.map { action ->
            Pair(action, original.tagFields[action.key]?.value())
        }
        return TagDiff(tagDiff)
    }

    internal fun save(context: Context) {
        val songFile = File(song.value.data)
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
            )
        } else {
            navigateToStorageSetting(context)
            Toast.makeText(
                App.instance, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT
            ).show()
        }
    }

}