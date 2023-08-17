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

class AudioDetailState(info: SongInfoModel, defaultColor: Color, val editable: Boolean) {

    private val _info: MutableStateFlow<SongInfoModel> = MutableStateFlow(info)
    val info get() = _info as StateFlow<SongInfoModel>

    private val _titleColor: MutableStateFlow<Color> = MutableStateFlow(defaultColor)
    val titleColor get() = _titleColor as StateFlow<Color>
    fun updateTitleColor(color: Color) {
        _titleColor.update { color }
    }

    private val _allEditRequest: MutableMap<FieldKey, String?> = EnumMap(FieldKey::class.java)
    val allEditRequests: Map<FieldKey, String?> get() = _allEditRequest

    val hasEdited: Boolean get() = _allEditRequest.isNotEmpty()

    fun editTag(key: FieldKey, newValue: String?): Boolean {
        return if (editable) {
            val tagFields = info.value.tagFields
            val field = tagFields[key]
            if (field != null && field.value() != newValue) { // keep only difference
                _allEditRequest[key] = newValue
                true
            } else {
                false
            }
        } else {
            false // not editable
        }
    }
}

typealias EditRequest = (FieldKey, String?) -> Boolean