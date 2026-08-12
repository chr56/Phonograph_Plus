/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ListLayoutStyle(
    val height: Dp,
    val contentStartPadding: Dp,
    val contentEndPadding: Dp,
    val menuWidth: Dp,
    val titleLines: Int,
    val secondaryLines: Int,
)

@Immutable
data class ItemColors(
    val title: Color,
    val secondaryText: Color,
    val icon: Color,
    val divider: Color,
    val selection: Color,
    val background: Color,
)

@Composable
fun Modifier.itemInteraction(
    selected: Boolean,
    selectionColor: Color,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val clicking = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(color = selectionColor.copy(alpha = 1f)),
            role = if (onClick != null) Role.Button else null,
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )
    } else Modifier
    return this
        .semantics { this.selected = selected }
        .then(clicking)
        .drawWithContent {
            drawContent()
            if (selected) drawRect(selectionColor)
        }
}

@Composable
fun ListItemMenu(width: Dp, iconColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(width)
            .height(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert_white_24dp),
            contentDescription = stringResource(R.string.action_more),
            modifier = Modifier.size(24.dp),
            tint = iconColor,
        )
    }
}


@Composable
fun BoxScope.ItemArtwork(
    painter: Painter,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentScale = ContentScale.Crop,
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}

@Composable
fun BoxScope.ItemOrdinal(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier
            .align(Alignment.Center)
            .fillMaxSize()
            .wrapContentHeight(),
        style = ItemDefaults.titleStyle,
        color = textColor,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Clip,
    )
}

