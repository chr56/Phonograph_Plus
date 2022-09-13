/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun BaseTextFieldItem(tag: String = "KeyName", value: String = "KeyValue", readonly: Boolean = false) {
    TextField(
        readOnly = readonly,
        leadingIcon = {
            Text(
                text = tag,
                style = TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .defaultMinSize(minWidth = 64.dp),
            )
        },
        value = value,
        placeholder = { Text("-") },
        onValueChange = {},
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.background,
            textColor = MaterialTheme.colors.onSurface,
            focusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
            unfocusedIndicatorColor = Color.Transparent,

            ),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth()
    )
}