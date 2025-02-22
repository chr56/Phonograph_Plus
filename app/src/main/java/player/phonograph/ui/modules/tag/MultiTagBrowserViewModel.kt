/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import player.phonograph.R
import player.phonograph.mechanism.metadata.DefaultMetadataExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toMusicMetadataKey
import player.phonograph.mechanism.metadata.edit.AudioMetadataEditor
import player.phonograph.mechanism.metadata.edit.EditAction
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.Metadata
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
                val infos = mutableListOf<AudioMetadata>()
                for (song in songs) {
                    val info = JAudioTaggerExtractor.extractSongMetadata(context, song)
                        ?: DefaultMetadataExtractor.extractSongMetadata(context, song)
                    infos.add(info)
                }
                _originalSongInfos.emit(infos)
                val default = reducedOriginalTagsImpl(infos).mapValues { "" }
                _displayTags.emit(default)
            }
        }
    }

    private val _originalSongInfos: MutableStateFlow<List<AudioMetadata>> = MutableStateFlow(emptyList())
    val originalSongInfos get() = _originalSongInfos.asStateFlow()


    fun reducedOriginalTags(): Flow<Map<ConventionalMusicMetadataKey, List<Metadata.Field>>> =
        originalSongInfos.map {
            reducedOriginalTagsImpl(it)
        }

    private fun reducedOriginalTagsImpl(list: List<AudioMetadata>): MutableMap<ConventionalMusicMetadataKey, List<Metadata.Field>> =
        list.fold(mutableMapOf()) { acc, model ->
            val musicMetadata = model.musicMetadata
            if (musicMetadata is JAudioTaggerMetadata)
                for ((key, value) in musicMetadata.textOnlyTagFields) {
                    val oldValue = acc[key.toMusicMetadataKey()]
                    val newValue = if (oldValue != null) {
                        oldValue + listOf(value)
                    } else {
                        listOf(value)
                    }
                    acc[key.toMusicMetadataKey()] = newValue
                }
            acc
        }



    private val _displayTags: MutableStateFlow<Map<ConventionalMusicMetadataKey, String?>> = MutableStateFlow(emptyMap())
    val displayTags get() = _displayTags.asStateFlow()

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    val coverImageDetailDialogState = MaterialDialogState(false)


    private var _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    val pendingEditRequests: List<EditAction> get() = _pendingEditRequests.toList()

    fun process(@Suppress("unused") context: Context, event: TagEditEvent) {
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

    private fun modifyView(action: (old: MutableMap<ConventionalMusicMetadataKey, String?>) -> Map<ConventionalMusicMetadataKey, String?>) {
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
                original.mapNotNull { metadata ->
                   val field = (metadata.musicMetadata as JAudioTaggerMetadata)[action.key]
                    field?.text()?.toString()
                }.filterNot { it.isEmpty() }.reduce { a, b -> "$a,$b" }
            Pair(action, oldValues)
        }
        return TagDiff(tagDiff)
    }

    internal fun save(context: Context) {
        val songFiles = songs.value.map { File(it.data) }
        if (songFiles.isNotEmpty()) {
            if (songFiles.first().canWrite()) {
                mergeActions()
                CoroutineScope(Dispatchers.Unconfined).launch {
                    AudioMetadataEditor(songFiles, pendingEditRequests).execute(context)
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