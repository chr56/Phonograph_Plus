/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.model.SongInfoModel
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AudioDetailState(info: SongInfoModel, defaultColor: Color, val editable: Boolean) {

    private val _info: MutableStateFlow<SongInfoModel> = MutableStateFlow(info)
    val info get() = _info as StateFlow<SongInfoModel>

    private val _titleColor: MutableStateFlow<Color> = MutableStateFlow(defaultColor)
    val titleColor get() = _titleColor as StateFlow<Color>
    fun updateTitleColor(color: Color) {
        _titleColor.update { color }
    }

    val tagInfoTableViewModel: TagInfoTableViewModel = TagInfoTableViewModel(
        run {//todo
            val songInfoModel = _info.value
            val tagFields =
                songInfoModel.tagFields.filter { it.value.value().isNotEmpty() }.mapValues { TextTag(it.value.value()) }
            val allFields =
                songInfoModel.allTags?.mapValues { TextTag(it.value) } ?: emptyMap()
            TagInfoTableState(editable, songInfoModel.tagFormat, tagFields, allFields)
        }
    )

    val pendingEditRequests: List<EditAction> get() = tagInfoTableViewModel.pendingEditRequests
    val hasEdited: Boolean get() = pendingEditRequests.isNotEmpty()

    fun mergeActions() = tagInfoTableViewModel.mergeActions()
}