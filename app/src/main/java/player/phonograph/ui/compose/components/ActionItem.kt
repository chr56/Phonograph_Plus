/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource


class ActionItem(
    val imageVector: ImageVector? = null,
    val imageRes: Int = -1,
    val text: String? = null,
    val textRes: Int = -1,
    val tint: Color? = null,
    val onClick: () -> Unit,
)


@Composable
fun ActionIconButton(
    item: ActionItem,
    modifier: Modifier = Modifier,
) {
    val icon =
        if (item.imageVector != null) {
            rememberVectorPainter(item.imageVector)
        } else {
            painterResource(item.imageRes)
        }
    val text =
        if (item.textRes > 0) {
            stringResource(item.textRes)
        } else {
            item.text
        }
    val tint = item.tint ?: MaterialTheme.colors.onPrimary
    IconButton(onClick = item.onClick) {
        Icon(
            icon,
            contentDescription = text,
            tint = tint,
            modifier = modifier,
        )
    }
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
fun ActionIconButton(
    imageVector: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    text: String? = null,
    onClick: () -> Unit,
) = ActionIconButton(
    icon = rememberVectorPainter(imageVector),
    tint = tint,
    modifier = modifier,
    text = text,
    onClick = onClick,
)

