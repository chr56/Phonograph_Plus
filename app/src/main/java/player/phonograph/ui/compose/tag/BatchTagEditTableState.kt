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
import java.util.EnumMap

class BatchTagEditTableState(info: List<SongInfoModel>, defaultColor: Color) {
    private val _info: MutableStateFlow<List<SongInfoModel>> = MutableStateFlow(info)
    val info get() = _info as StateFlow<List<SongInfoModel>>

    private val _titleColor: MutableStateFlow<Color> = MutableStateFlow(defaultColor)
    val titleColor get() = _titleColor as StateFlow<Color>
    fun updateTitleColor(color: Color) {
        _titleColor.update { color }
    }

    private val _allEditRequest: MutableMap<FieldKey, String?> = EnumMap(FieldKey::class.java)
    val allEditRequests: Map<FieldKey, String?> get() = _allEditRequest

    fun changeField(key: FieldKey, newValue: String) {
        _allEditRequest[key] = newValue
    }
    fun removeField(key: FieldKey) {
        _allEditRequest[key] = null
    }
    fun undoChanges(key: FieldKey) {
        _allEditRequest.remove(key)
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
    map { it.tagValue(key).value() }.filterNot { it.isEmpty() }.toSet()