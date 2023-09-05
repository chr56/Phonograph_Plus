/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun BaseSearchBox(
    modifier: Modifier,
    title: String,
    target: @Composable () -> Unit,
    onSearch: () -> Unit,
    fields: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.h5
        )
        Line(name = "Target") {
            target()
        }
        fields()
        TextButton(
            onClick = {
                onSearch()
            },
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .align(Alignment.End)
        ) {
            Text(stringResource(R.string.action_search))
        }
    }
}

@Composable
fun SearchTextBox(current: String, modifier: Modifier = Modifier, onUpdate: (String) -> Unit) {
    TextField(
        value = current,
        onValueChange = onUpdate,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.background,
            textColor = MaterialTheme.colors.onBackground,
        ),
        trailingIcon = {
            Icon(
                Icons.Default.Clear,
                contentDescription = stringResource(id = R.string.reset_action),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        onUpdate.invoke("")
                    }
            )
        }
    )
}

@Composable
fun <T> Target(
    all: Collection<T>,
    text: (T) -> String,
    current: T,
    modifier: Modifier = Modifier,
    onUpdate: (T) -> Unit,
) {
    Row(modifier) {
        for (target in all) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable { onUpdate(target) }
            ) {
                RadioButton(selected = target == current, {})
                Text(
                    text(target),
                    Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}