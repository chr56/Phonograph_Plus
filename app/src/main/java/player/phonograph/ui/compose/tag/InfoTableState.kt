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

open class InfoTableState(info: SongInfoModel, defaultColor: Color) {
    private val _info: MutableStateFlow<SongInfoModel> = MutableStateFlow(info)
    val info get() = _info as StateFlow<SongInfoModel>

    private val _titleColor: MutableStateFlow<Color> = MutableStateFlow(defaultColor)
    val titleColor get() = _titleColor as StateFlow<Color>
    fun updateTitleColor(color: Color) {_titleColor.update { color }}
}

class EditableInfoTableState(
    info: SongInfoModel,
    color: Color
) : InfoTableState(info, color) {

    private val _allEditRequest: MutableMap<FieldKey, String?> = EnumMap(FieldKey::class.java)
    val allEditRequests: Map<FieldKey, String?> get() = _allEditRequest

    fun editRequest(key: FieldKey, newValue: String?) {
        if (info.value.tagValue(key).value() != newValue) // keep only difference
            _allEditRequest[key] = newValue
    }

}

typealias EditRequest = (FieldKey, String?) -> Unit