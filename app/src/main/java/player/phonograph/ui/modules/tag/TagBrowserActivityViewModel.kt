/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.storage.launcher.ICreateFileStorageAccessible
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.metadata.edit.JAudioTaggerAudioMetadataEditor
import player.phonograph.mechanism.metadata.read.MetadataExtractors
import player.phonograph.mechanism.metadata.read.SongDetail
import player.phonograph.model.Song
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.EditAction
import player.phonograph.model.metadata.InteractiveAction
import player.phonograph.model.metadata.InteractiveAction.Edit
import player.phonograph.model.metadata.InteractiveAction.ExtractArtwork
import player.phonograph.model.metadata.InteractiveAction.Save
import player.phonograph.ui.modules.tag.util.display
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TagBrowserActivityViewModel : AbsMetadataViewModel() {

    private var originalState: SongDetail? = null
    private val _state: MutableStateFlow<SongDetail?> = MutableStateFlow(null)
    val state get() = _state.asStateFlow()

    fun load(context: Context, song: Song, asOriginal: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val detail = MetadataExtractors.extractMetadata(context, song)
            if (asOriginal) originalState = detail
            _state.emit(detail)
        }
    }

    private fun modifyContent(context: Context, event: Edit) {
        viewModelScope.launch(Dispatchers.IO) { _state.emit(_state.value?.edit(context, event)) }
    }

    override fun submitEvent(context: Context, event: InteractiveAction) {
        viewModelScope.launch {
            when (event) {
                ExtractArtwork -> extractArtwork(context)

                Save           -> {
                    if (editable.value) save(context)
                }

                is Edit        -> {
                    if (editable.value) {
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
    }

    override fun generateMetadataDifference(context: Context): MetadataChanges {
        val original = originalState?.metadata?.musicMetadata
        val tagDiff = pendingEditRequests.map { action ->
            val field = original?.get(action.key)
            val text = if (field != null) display(context, field) else ""
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
                context, R.string.err_permission_manage_external_storage_denied, Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun extractArtwork(activity: Context) {
        val currentState = _state.value ?: return

        val path = currentState.song.data
        val fileName = path.substringAfterLast('/').substringBeforeLast('.')
        val imageBytes = withContext(Dispatchers.IO) { MetadataExtractors.extractRawImage(path) } ?: return
        val image = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, null)
        }
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
                                warning(activity, TAG, "Failed to open File")
                            }
                        }
                    }
                } else {
                    warning(activity, TAG, "Failed to create File")
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