/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import org.jaudiotagger.tag.FieldKey

sealed interface TagInfoTableEvent {
    data class UpdateTag(val fieldKey: FieldKey, val newValue: String) : TagInfoTableEvent
    data class AddNewTag(val fieldKey: FieldKey) : TagInfoTableEvent
    data class RemoveTag(val fieldKey: FieldKey) : TagInfoTableEvent
}