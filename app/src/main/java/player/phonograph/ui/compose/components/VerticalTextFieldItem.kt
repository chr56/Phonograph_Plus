/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalTextFieldItem(
    title: String,
    value: String?,
    hint: String,
    onTextChanged: (String) -> Unit,
    extraTrailingIcon: @Composable (() -> Unit)? = null,
    allowReset: Boolean = false,
    allowClear: Boolean = true
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // title
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .align(Alignment.Start),
        )
        // content
        var currentValue by remember { mutableStateOf(value ?: "") }
        val changeCurrentValue = { str: String ->
            currentValue = str
            onTextChanged(str)
        }
        TextField(
            value = currentValue,
            placeholder = { Text(text = hint) },
            onValueChange = changeCurrentValue,
            modifier = Modifier.align(Alignment.Start),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                textColor = MaterialTheme.colors.onSurface,
                focusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = TextStyle(
                //color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                fontSize = 14.sp,
            ),
            trailingIcon = {
                if (extraTrailingIcon != null) extraTrailingIcon()
                TrailingIcon(
                    onReset = { changeCurrentValue(value ?: "") },
                    allowReset = allowReset,
                    onClear = { changeCurrentValue("") },
                    allowClear = allowClear
                )
            }
        )
    }
}

@Composable
private fun TrailingIcon(
    onReset: () -> Unit,
    allowReset: Boolean,
    onClear: () -> Unit,
    allowClear: Boolean
) {
    Row {
        if (allowReset)
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(id = R.string.reset_action),
                modifier = Modifier.clickable { onReset() })
        if (allowClear)
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(id = R.string.clear_action),
                modifier = Modifier.clickable { onClear() })
    }
}