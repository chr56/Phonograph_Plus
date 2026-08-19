/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.menu

import player.phonograph.ui.compose.components.actions.ActionItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp

@Composable
fun MenuItem(item: ActionItem, withIcon: Boolean) {
    if (withIcon) {
        IconTextMenuItem(icon = item.iconPainter(), text = item.text()!!, tint = item.tint, onClick = item.onClick)
    } else {
        TextMenuItem(text = item.text()!!, onClick = item.onClick)
    }
}

@Composable
fun TextMenuItem(
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
fun IconTextMenuItem(
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