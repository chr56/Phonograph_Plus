/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.tag.EditAction
import player.phonograph.mechanism.tag.edit.applyEdit
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.TagField
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MultiTagBrowserViewModel : ViewModel() {

    private val _editable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val editable get() = _editable.asStateFlow()
    fun updateEditable(editable: Boolean) {
        _editable.update { editable }
    }

    private val _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()
    fun updateSong(context: Context, songs: List<Song>) {
        if (songs.isNotEmpty()) {
            _songs.update { songs }
            viewModelScope.launch(Dispatchers.IO) {
                val infos = mutableListOf<SongInfoModel>()
                for (song in songs) {
                    val info = loadSongInfo(context, song)
                    infos.add(info)
                }
                _originalSongInfos.emit(infos)
                val default = reducedOriginalTagsImpl(infos).mapValues { "" }
                _displayTags.emit(default)
            }
        }
    }

    private val _originalSongInfos: MutableStateFlow<List<SongInfoModel>> = MutableStateFlow(emptyList())
    val originalSongInfos get() = _originalSongInfos.asStateFlow()


    fun reducedOriginalTags(): Flow<Map<FieldKey, List<TagField>>> =
        originalSongInfos.map {
            reducedOriginalTagsImpl(it)
        }

    private fun reducedOriginalTagsImpl(list: List<SongInfoModel>): MutableMap<FieldKey, List<TagField>> =
        list.fold(mutableMapOf()) { acc, model ->
            for ((key, value) in model.tagTextOnlyFields) {
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



    private val _displayTags: MutableStateFlow<Map<FieldKey, String?>> = MutableStateFlow(emptyMap())
    val displayTags get() = _displayTags.asStateFlow()

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    val coverImageDetailDialogState = MaterialDialogState(false)


    private var _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    val pendingEditRequests: List<EditAction> get() = _pendingEditRequests.toList()

    fun process(@Suppress("UNUSED_PARAMETER") context: Context, event: TagEditEvent) {
        viewModelScope.launch {
            when (event) {
                is TagEditEvent.UpdateTag     -> {
                    modifyView { old ->
                        old.apply { put(event.fieldKey, event.newValue) }
                    }
                    modifyEditRequest(EditAction.Update(event.fieldKey, event.newValue))
                }

                is TagEditEvent.AddNewTag     -> {
                    modifyView { old ->
                        old + (event.fieldKey to "")
                    }
                    modifyEditRequest(EditAction.Update(event.fieldKey, ""))
                }

                is TagEditEvent.RemoveTag     -> {
                    modifyView { old ->
                        old.apply { remove(event.fieldKey) }
                    }
                    modifyEditRequest(EditAction.Delete(event.fieldKey))
                }

                is TagEditEvent.UpdateArtwork -> {
                    modifyEditRequest(EditAction.ImageReplace(event.file))
                }

                TagEditEvent.RemoveArtwork    -> {
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

    private fun modifyView(action: (old: MutableMap<FieldKey, String?>) -> Map<FieldKey, String?>) {
        viewModelScope.launch(Dispatchers.Default) {
            val old = _displayTags.value.toMutableMap()
            val new = action(old)
            _displayTags.emit(new)
        }
    }

    internal fun diff(): TagDiff {
        mergeActions()
        val original = originalSongInfos.value
        val tagDiff = pendingEditRequests.map { action ->
            val oldValues =
                original.mapNotNull { it.tagFields[action.key]?.value() }
                    .filterNot { it.isEmpty() }
                    .reduce { a, b -> "$a,$b" }
            Pair(action, oldValues)
        }
        return TagDiff(tagDiff)
    }

    internal fun save(context: Context) {
        val songFiles = songs.value.map { File(it.data) }
        if (songFiles.isNotEmpty()) {
            if (songFiles.first().canWrite()) {
                mergeActions()
                applyEdit(
                    scope = CoroutineScope(Dispatchers.Unconfined),
                    context = context,
                    songFiles = songFiles,
                    allEditRequest = pendingEditRequests,
                    needDeleteCover = false,
                    needReplaceCover = false,
                    newCoverUri = null
                ) {
                    updateEditable(false)
                    updateSong(context, _songs.value)
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

}