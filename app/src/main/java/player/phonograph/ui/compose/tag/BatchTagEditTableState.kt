/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import org.jaudiotagger.tag.FieldKey
import player.phonograph.mechanism.tag.edit.selectNewArtwork
import player.phonograph.model.SongInfoModel
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class BatchTagEditTableState(info: List<SongInfoModel>, defaultColor: Color) {
    private val _info: MutableStateFlow<List<SongInfoModel>> = MutableStateFlow(info)
    val info get() = _info as StateFlow<List<SongInfoModel>>

    private val _titleColor: MutableStateFlow<Color> = MutableStateFlow(defaultColor)
    val titleColor get() = _titleColor as StateFlow<Color>
    fun updateTitleColor(color: Color) {
        _titleColor.update { color }
    }

    private val _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    val pendingEditRequests: List<EditAction> get() = _pendingEditRequests.toList()

    val hasEdited: Boolean get() = pendingEditRequests.isNotEmpty()

    fun changeField(key: FieldKey, newValue: String) {
        _pendingEditRequests.add(
            EditAction.Update(key, newValue)
        )
    }

    fun removeField(key: FieldKey) {
        _pendingEditRequests.add(
            EditAction.Delete(key)
        )
    }

    fun undoChanges(key: FieldKey) {
        val actions = _pendingEditRequests.filter { it.key == key }
        _pendingEditRequests.removeAll(actions)
    }

    val coverImageDetailDialogState = MaterialDialogState(false)

    fun updateCover(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val newArtwork = selectNewArtwork(context)
            while (newArtwork.value == null) yield()
            val uri = newArtwork.value ?: throw Exception("Coroutine Error")
            needReplaceCover = true
            needDeleteCover = false
            newCover = uri
        }
    }

    fun removeCover() {
        needDeleteCover = true
        needReplaceCover = false
    }

    var needDeleteCover = false
        private set
    var needReplaceCover = false
        private set

    var newCover: Uri? = null
        private set
}

internal fun List<SongInfoModel>.reduceTags(key: FieldKey) =
    mapNotNull { it.tagFields[key]?.value() }.filterNot { it.isEmpty() }.toSet()