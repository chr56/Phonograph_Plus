/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.SongInfoModel
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.EnumMap

open class InfoTableViewModel(info: SongInfoModel, defaultColor: Color) : ViewModel() {
    private val _info: MutableStateFlow<SongInfoModel> = MutableStateFlow(info)
    val info get() = _info as StateFlow<SongInfoModel>

    private val _titleColor: MutableStateFlow<Color> = MutableStateFlow(defaultColor)
    val titleColor get() = _titleColor as StateFlow<Color>
    fun updateTitleColor(color: Color) {_titleColor.update { color }}
}

class EditableInfoTableViewModel(
    info: SongInfoModel,
    color: Color
) : InfoTableViewModel(info, color) {

    private val _allEditRequest: MutableMap<FieldKey, String?> = EnumMap(FieldKey::class.java)
    val allEditRequests: Map<FieldKey, String?> get() = _allEditRequest

    fun editRequest(key: FieldKey, newValue: String?) {
        if (info.value.tagValue(key).value() != newValue) // keep only difference
            _allEditRequest[key] = newValue
    }

    /**
     * generate diff with [oldInfo]
     * @return <TagFieldKey, oldValue, newValue> triple
     */
    fun generateDiff(): List<Triple<FieldKey, String?, String?>> {
        val current = info.value
        return allEditRequests.map { (key, new) ->
            val old = current.tagValue(key).value()
            Triple(key, old, new)
        }
    }

}

typealias EditRequest = (FieldKey, String?) -> Unit