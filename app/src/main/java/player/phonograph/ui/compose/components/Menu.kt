/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun DropDownMenuContent(
    list: List<Pair<String, Function0<Unit>>>,
) {
    Column {
        for ((str, block) in list) {
            TextMenuItem(text = str, onClick = block)
        }
    }
}


@Composable
fun DropDownMenuContent(
    actions: List<ActionItem>,
    withIcon: Boolean = false,
) {
    Column {
        for (action in actions) {
            MenuItem(item = action, withIcon = withIcon)
        }
    }
}

@Composable
private fun MenuItem(item: ActionItem, withIcon: Boolean) {
    val text =
        if (item.textRes > 0) {
            stringResource(item.textRes)
        } else {
            item.text!!
        }
    if (withIcon) {
        val icon =
            if (item.imageVector != null) {
                rememberVectorPainter(item.imageVector)
            } else {
                painterResource(item.imageRes)
            }
        IconTextMenuItem(icon = icon, text = text, tint = item.tint, onClick = item.onClick)
    } else {
        TextMenuItem(text = text, onClick = item.onClick)
    }
}

@Composable
private fun TextMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Box {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun IconTextMenuItem(
    icon: Painter,
    text: String,
    tint: Color?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = tint ?: MaterialTheme.colors.onSurface,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterVertically)
        )
    }
}
