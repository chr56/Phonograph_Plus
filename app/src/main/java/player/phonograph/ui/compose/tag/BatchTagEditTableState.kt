/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.SongInfoModel
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
}

internal fun List<SongInfoModel>.reduceTags(key: FieldKey) =
    map { it.tagValue(key).value() }.filterNot { it.isEmpty() }.toSet()