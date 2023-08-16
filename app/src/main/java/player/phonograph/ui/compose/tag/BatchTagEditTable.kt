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
import org.jaudiotagger.tag.FieldKey.YEAR
import player.phonograph.R
import player.phonograph.model.res
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextFieldItem
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun BatchTagEditTable(stateHolder: BatchTagEditTableState, context: Context) {
    val titleColor = stateHolder.titleColor.collectAsState().value

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // files
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.files), color = titleColor)
        FileList(stateHolder)
        // tags
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.update_image),
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable {
                    stateHolder.coverImageDetailDialogState.show()
                }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)

        //MultipleTag(TITLE, stateHolder)
        MultipleTag(ARTIST, stateHolder)
        MultipleTag(ALBUM, stateHolder)
        MultipleTag(ALBUM_ARTIST, stateHolder)
        MultipleTag(COMPOSER, stateHolder)
        MultipleTag(LYRICIST, stateHolder)
        MultipleTag(YEAR, stateHolder)
        MultipleTag(GENRE, stateHolder)
        //MultipleTag(TRACK, stateHolder)
        MultipleTag(COMMENT, stateHolder)
    }
    CoverImageDetailDialog(stateHolder, context)
}

@Composable
private fun FileList(stateHolder: BatchTagEditTableState) {
    val infoModels = stateHolder.info.collectAsState().value
    LazyColumn(
        Modifier
            .scrollable(rememberScrollState(), Vertical)
            .heightIn(max = 300.dp)
    ) {
        for ((index, info) in infoModels.withIndex()) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "$index", modifier = Modifier.align(Alignment.Top))
                    Column {
                        VerticalTextItem(
                            stringResource(R.string.label_file_name),
                            info.fileName.value()
                        )
                        VerticalTextItem(
                            stringResource(R.string.label_file_path),
                            info.filePath.value()
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun MultipleTag(
    key: FieldKey,
    stateHolder: BatchTagEditTableState
) {
    val prefills = stateHolder.info.collectAsState().value.reduceTags(key)

    val tagNameRes = remember { key.res() }
    val tagName = stringResource(id = tagNameRes)

    val currentValue = remember { MutableStateFlow("") }

    var showDropdownMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {

        VerticalTextFieldItem(
            title = tagName,
            value = currentValue,
            hint = "",
            trailingIcon = {
                Row {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(id = R.string.more_actions),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showDropdownMenu = !showDropdownMenu }
                    )
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(id = R.string.reset_action),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                stateHolder.undoChanges(key)
                                currentValue.tryEmit("")
                            }
                    )
                }
            }
        )

        LaunchedEffect(currentValue) {
            currentValue.collect { newValue ->
                if (newValue.isNotBlank())
                    stateHolder.changeField(key, newValue)
            }
        }

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
                            currentValue.tryEmit(prefill)
                            stateHolder.changeField(key, prefill)
                        }
                    ) {
                        Text(text = prefill)
                    }
                }
                val textClear = "  [${stringResource(id = R.string.clear_action)}]  "
                DropdownMenuItem(
                    onClick = {
                        showDropdownMenu = false
                        currentValue.tryEmit("")
                        stateHolder.removeField(key)
                    }
                ) {
                    Text(text = textClear)
                }
            }
        }
    }
}

@Composable
private fun CoverImageDetailDialog(stateHolder: BatchTagEditTableState, context: Context) =
    CoverImageDetailDialog(
        state = stateHolder.coverImageDetailDialogState,
        artworkExist = true,
        onSave = {},
        onDelete = { stateHolder.removeCover() },
        onUpdate = { stateHolder.updateCover(context) },
        editMode = true
    )