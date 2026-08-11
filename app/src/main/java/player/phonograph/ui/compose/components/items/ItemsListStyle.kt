/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ListItemWithLeading(
    style: ListLayoutStyle,
    title: String,
    secondary: List<String?>,
    modifier: Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)?,
    colors: ItemColors,
    titleTextStyle: TextStyle,
    secondaryTextStyle: TextStyle,
    selected: Boolean,
    contentAlpha: Float,
    divider: Dp?,
    showDragHandle: Boolean,
    leadingModifier: Modifier,
    dragHandleModifier: Modifier,
    textOverflows: List<TextOverflow>,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onMenuClick: (() -> Unit)?,
) {
    ListItemFrame(
        modifier, style.height, colors, selected, divider,
        showDragHandle, dragHandleModifier, onClick, onLongClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .then(leadingModifier)
                    .alpha(contentAlpha.coerceIn(0f, 1f)),
            ) {
                leadingContent?.invoke(this)
            }
            ListItemText(
                title, secondary, style, colors, textOverflows, titleTextStyle, secondaryTextStyle, contentAlpha,
                Modifier
                    .weight(1f)
                    .padding(start = style.contentStartPadding, end = style.contentEndPadding),
            )
            if (onMenuClick != null) {
                ListItemMenu(style.menuWidth, colors.icon, onMenuClick)
            }
        }
    }
}

@Composable
fun ListItemWithoutLeading(
    style: ListLayoutStyle,
    title: String,
    secondary: List<String?>,
    modifier: Modifier,
    colors: ItemColors,
    titleTextStyle: TextStyle,
    secondaryTextStyle: TextStyle,
    selected: Boolean,
    contentAlpha: Float,
    divider: Dp?,
    showDragHandle: Boolean,
    dragHandleModifier: Modifier,
    textOverflows: List<TextOverflow>,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onMenuClick: (() -> Unit)?,
) {
    ListItemFrame(
        modifier, style.height, colors, selected, divider,
        showDragHandle, dragHandleModifier, onClick, onLongClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListItemText(
                title, secondary, style, colors, textOverflows, titleTextStyle, secondaryTextStyle, contentAlpha,
                Modifier
                    .weight(1f)
                    .padding(start = style.contentStartPadding, end = style.contentEndPadding),
            )
            if (onMenuClick != null) {
                ListItemMenu(style.menuWidth, colors.icon, onMenuClick)
            }
        }
    }
}

@Composable
private fun ListItemFrame(
    modifier: Modifier,
    height: Dp,
    colors: ItemColors,
    selected: Boolean,
    divider: Dp?,
    showDragHandle: Boolean,
    dragHandleModifier: Modifier,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .itemInteraction(selected, colors.selection, onClick, onLongClick),
    ) {
        content()
        if (showDragHandle) {
            DragHandle(
                dragHandleModifier,
                iconPainter = painterResource(R.drawable.ic_drag_vertical_white_24dp),
                iconColor = colors.icon
            )
        }
        if (divider != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = divider)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
        }
    }
}

@Composable
private fun DragHandle(
    modifier: Modifier,
    iconPainter: Painter,
    iconColor: Color,
) {
    Box(
        modifier = Modifier
            .offset(x = (-8).dp)
            .width(24.dp)
            .fillMaxHeight()
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            iconPainter,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor,
        )
    }
}

@Composable
private fun ListItemText(
    title: String,
    secondary: List<String?>,
    style: ListLayoutStyle,
    colors: ItemColors,
    textOverflows: List<TextOverflow>,
    titleTextStyle: TextStyle,
    secondaryTextStyle: TextStyle,
    contentAlpha: Float,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .wrapContentHeight(unbounded = true)
            .alpha(contentAlpha.coerceIn(0f, 1f)),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth(),
            style = titleTextStyle,
            color = colors.title,
            maxLines = style.titleLines,
            softWrap = style.titleLines > 1,
            overflow = textOverflows[0],
        )
        for ((index, text) in secondary.withIndex()) {
            Text(
                text.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
                style = secondaryTextStyle,
                color = colors.secondaryText,
                maxLines = style.secondaryLines,
                softWrap = style.secondaryLines > 1,
                overflow = textOverflows[index + 1],
            )
        }
    }
}