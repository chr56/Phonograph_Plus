/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.actions

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter


@Composable
fun ActionIconButton(item: ActionItem, modifier: Modifier = Modifier) {
    ActionIconButton(
        icon = item.iconPainter(),
        tint = item.tint ?: MaterialTheme.colors.onPrimary,
        modifier = modifier,
        text = item.text(),
        onClick = item.onClick,
    )
}

@Composable
fun ActionIconButton(
    icon: Painter,
    tint: Color,
    modifier: Modifier = Modifier,
    text: String? = null,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = text,
            tint = tint,
            modifier = modifier,
        )
    }
}


@Composable
fun optionalActionIconButton(
    painter: Painter?,
    tint: Color,
    modifier: Modifier = Modifier,
    text: String? = null,
    onClick: () -> Unit,
): (@Composable () -> Unit)? =
    if (painter != null) {
        @Composable {
            ActionIconButton(
                painter,
                tint = tint,
                modifier = modifier,
                text = text,
                onClick = onClick
            )
        }
    } else {
        null
    }