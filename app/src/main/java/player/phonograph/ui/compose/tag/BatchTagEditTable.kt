/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag


import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.FieldKey.ALBUM
import org.jaudiotagger.tag.FieldKey.ALBUM_ARTIST
import org.jaudiotagger.tag.FieldKey.ARTIST
import org.jaudiotagger.tag.FieldKey.COMMENT
import org.jaudiotagger.tag.FieldKey.COMPOSER
import org.jaudiotagger.tag.FieldKey.GENRE
import org.jaudiotagger.tag.FieldKey.LYRICIST
import org.jaudiotagger.tag.FieldKey.TITLE
import org.jaudiotagger.tag.FieldKey.TRACK
import org.jaudiotagger.tag.FieldKey.YEAR
import player.phonograph.R
import player.phonograph.model.SongInfoModel
import player.phonograph.model.songTagNameRes
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun BatchTagEditTable(stateHolder: BatchTagEditTableState) {
    val titleColor = stateHolder.titleColor.collectAsState().value
    val infoModels = stateHolder.info.collectAsState().value

    val editRequest: EditRequest = remember {
        { key, newValue -> stateHolder.editRequest(key, newValue) }
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // files
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.files), color = titleColor)
        FileList(infoModels = infoModels)
        // tags
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)

        //MultipleTag(TITLE, infoModels, editRequest)
        MultipleTag(ARTIST, infoModels, editRequest)
        MultipleTag(ALBUM, infoModels, editRequest)
        MultipleTag(ALBUM_ARTIST, infoModels, editRequest)
        MultipleTag(COMPOSER, infoModels, editRequest)
        MultipleTag(LYRICIST, infoModels, editRequest)
        MultipleTag(YEAR, infoModels, editRequest)
        MultipleTag(GENRE, infoModels, editRequest)
        //MultipleTag(TRACK, infoModels, editRequest)
        MultipleTag(COMMENT, infoModels, editRequest)
    }
}

@Composable
private fun FileList(infoModels: List<SongInfoModel>) {
    for ((index, info) in infoModels.withIndex()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "$index", modifier = Modifier.align(Alignment.Top))
            Column {
                VerticalTextItem(stringResource(R.string.label_file_name), info.fileName.value())
                VerticalTextItem(stringResource(R.string.label_file_path), info.filePath.value())
            }
        }
    }
}


@Composable
private fun MultipleTag(
    key: FieldKey,
    from: List<SongInfoModel>,
    editRequest: EditRequest
) {
    MultipleTagImpl(key, from.reduceTags(key), editRequest)
}


@Composable
private fun MultipleTagImpl(
    key: FieldKey,
    prefills: List<String>,
    editRequest: EditRequest
) {
    val tagNameRes = remember { songTagNameRes(key) }
    val tagName = stringResource(id = tagNameRes)

    var currentValue by remember { mutableStateOf("") }

    var showDropdownMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        EditableItem(
            title = tagName,
            value = currentValue,
            onTextChanged = { newValue -> editRequest.invoke(key, newValue) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(id = R.string.more_actions),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { showDropdownMenu = !showDropdownMenu }
                )
            }
        )

        DropdownMenu(
            expanded = showDropdownMenu,
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = {
                showDropdownMenu = false
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollable(rememberScrollState(), Vertical)
            ) {
                for (prefill in prefills) {
                    DropdownMenuItem(
                        onClick = {
                            showDropdownMenu = false
                            currentValue = prefill
                            editRequest.invoke(key, prefill)
                        }
                    ) {
                        Text(text = prefill)
                    }
                }
            }
        }
    }
}