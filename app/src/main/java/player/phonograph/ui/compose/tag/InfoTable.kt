/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.model.TagField
import player.phonograph.model.getFileSizeString
import player.phonograph.model.res
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextFieldItem
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Text infomation
 */
@Composable
internal fun InfoTable(audioDetailState: AudioDetailState) {

    val stateHolder = remember { audioDetailState }
    val titleColor = stateHolder.titleColor.collectAsState().value
    val info = stateHolder.info.collectAsState().value

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        //
        // File info
        //
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.file), color = titleColor)
        Item(R.string.label_file_name, info.fileName.value())
        Item(R.string.label_file_path, info.filePath.value())
        Item(R.string.label_file_size, getFileSizeString(info.fileSize.value()))

        for ((key, field) in info.audioPropertyFields) {
            Item(stringResource(key.res), value = field.value().toString())
        }

        //
        // Music Tag
        //
        TagInfoTable(model = audioDetailState.tagInfoTableViewModel, titleColor = titleColor)
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
private fun Item(@StringRes tagStringRes: Int, value: String) =
    Item(stringResource(tagStringRes), value)

@Composable
internal fun Item(tag: String, value: String) = VerticalTextItem(title = tag, value = value)