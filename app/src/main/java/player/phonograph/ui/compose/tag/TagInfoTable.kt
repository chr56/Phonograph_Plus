/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.model.RawTag
import player.phonograph.model.TagData
import player.phonograph.model.allFieldKey
import player.phonograph.model.res
import player.phonograph.model.text
import player.phonograph.ui.compose.components.Title
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagInfoTable(model: TagInfoTableViewModel, titleColor: Color) {
    val state: TagInfoTableState by model.viewState.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Title(stringResource(R.string.music_tags), color = titleColor)
        Item(stringResource(R.string.tag_format), state.tagFormat.id)
        Spacer(modifier = Modifier.height(8.dp))
        CommonTagTable(model)
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.raw_tags))
        AllTagTable(model)
    }
}

@Composable
private fun CommonTagTable(model: TagInfoTableViewModel) {
    val state: TagInfoTableState by model.viewState.collectAsState()

    val tagFields = state.tagFields
    for ((key, field) in tagFields) {
        CommonTag(key, field, state.editable, model::process)
    }
    if (state.editable) AddMoreButton(model)
}

@Composable
private fun AddMoreButton(model: TagInfoTableViewModel) {
    Box(Modifier.fillMaxWidth()) {
        var showed by remember {
            mutableStateOf(false)
        }
        val fieldKeys =
            allFieldKey.filterNot { it in model.viewState.value.tagFields.keys }

        DropdownMenu(expanded = showed, onDismissRequest = { showed = false }) {
            val context = LocalContext.current
            for (fieldKey in fieldKeys) {
                val text = fieldKey.text(context.resources)
                val res = fieldKey.res()
                val name = if (res > 0) "$text (${fieldKey.name})" else fieldKey.name
                Text(
                    name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showed = false
                            model.process(TagInfoTableEvent.AddNewTag(fieldKey))
                        }
                        .padding(8.dp, 16.dp),
                )
            }
        }
        TextButton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(0.dp, 16.dp),
            onClick = {
                showed = true
            }
        ) {
            Row(
                Modifier.fillMaxWidth()
            )
            {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_white_24dp),
                    contentDescription = stringResource(id = R.string.add_action),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterVertically),
                    tint = MaterialTheme.colors.onSurface
                )
                Text(
                    text = stringResource(id = R.string.add_action),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth(),
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

@Composable
private fun CommonTag(
    key: FieldKey,
    field: TagData,
    editable: Boolean,
    onEdit: (TagInfoTableEvent) -> Unit,
) {
    val context = LocalContext.current

    val tagName = key.text(context.resources)

    val tagValue = field.text()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableItem(key, tagName, tagValue, onEdit)
        } else {
            if (tagValue.isNotEmpty()) Item(tagName, tagValue)
        }
    }
}

@Composable
private fun AllTagTable(model: TagInfoTableViewModel) {
    val state: TagInfoTableState by model.viewState.collectAsState()
    val tagFields = state.allTags
    for ((key, rawTag) in tagFields) {
        RawTag(key, rawTag)
    }
}


@Composable
private fun EditableItem(
    key: FieldKey,
    tagName: String,
    original: String,
    onEdit: (TagInfoTableEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // tag name
        Text(
            text = tagName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .align(Alignment.Start),
        )
        // content
        var currentValue by remember(key) { mutableStateOf(original) }
        var hasEdited by remember(key) { mutableStateOf(false) }


        var indicatorColor by remember(key, original) {
            mutableStateOf(Color(0xFF707070))
        }


        fun updateValue(newValue: String) {
            currentValue = newValue
            hasEdited = newValue != original
            indicatorColor = if (hasEdited) Color(0xFFF59C01) else Color(0xFF707070)
        }

        fun submit() {
            onEdit.invoke(
                TagInfoTableEvent.UpdateTag(key, currentValue)
            )
            hasEdited = false
            indicatorColor = Color(0xFF00C72C)
        }

        TextField(
            value = currentValue,
            // placeholder = { Text(text = currentValue) },
            onValueChange = ::updateValue,
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                textColor = MaterialTheme.colors.onSurface,
                focusedIndicatorColor = indicatorColor.copy(alpha = TextFieldDefaults.IconOpacity),
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = TextStyle(
                //color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                fontSize = 14.sp,
            ),
            trailingIcon = {
                Row {
                    if (hasEdited) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(id = android.R.string.ok),
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { submit() }
                        )
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.reset_action),
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { updateValue(original) }
                        )
                    }
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.delete_action),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                onEdit.invoke(TagInfoTableEvent.RemoveTag(key))
                            }
                    )
                }
            }
        )
    }

}


@Composable
private fun RawTag(key: String, rawTag: RawTag) {
    val (
        id: String,
        name: String,
        value: TagData,
        description: String?,
    ) = rawTag

    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // name and id
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                text = name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                textAlign = TextAlign.Left,
                modifier = Modifier.weight(8f),
            )
            Text(
                text = id,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(2f),
            )
        }
        // description
        if (description != null) {
            Text(
                text = description,
                style = TextStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 9.sp,
                ),
                modifier = Modifier.align(Alignment.Start),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // content
        SelectionContainer {
            Text(
                text = value.text(),
                style = TextStyle(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}