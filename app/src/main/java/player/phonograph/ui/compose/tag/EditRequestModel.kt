/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.SongInfoModel
import java.util.EnumMap

typealias EditRequest = (FieldKey, String?) -> Unit

class EditRequestModel {

    private val _allRequest: MutableMap<FieldKey, String?> = EnumMap(FieldKey::class.java)
    val allRequests: Map<FieldKey, String?> get() = _allRequest

    fun request(songInfoModel: SongInfoModel, key: FieldKey, newValue: String?) {
        if (songInfoModel.tagValue(key).value() != newValue) // keep only difference
            _allRequest[key] = newValue
    }

}