/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.R
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//region List

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
    menuContent: (@Composable () -> Unit)? = null,
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
                ItemMenuButton(style.menuWidth, colors.icon, onMenuClick, menuContent)
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
    menuContent: (@Composable () -> Unit)? = null,
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
                ItemMenuButton(style.menuWidth, colors.icon, onMenuClick, menuContent)
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
//endregion


//region Grid

@Composable
fun ItemGrid(
    title: String,
    description: String?,
    modifier: Modifier,
    artwork: @Composable BoxScope.() -> Unit,
    colors: ItemColors,
    selected: Boolean,
    contentAlpha: Float,
    titleOverflow: TextOverflow,
    descriptionOverflow: TextOverflow,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    footerHeight: Dp,
    textPadding: Dp,
    titleStyle: TextStyle,
    descriptionTextStyle: TextStyle,
) {
    Column(
        modifier
            .clipToBounds()
            .itemInteraction(selected, colors.selection, onClick, onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .alpha(contentAlpha),
        ) {
            artwork()
        }
        GridItemFooter(
            title, description, colors, contentAlpha, titleOverflow, descriptionOverflow,
            footerHeight, textPadding, titleStyle, descriptionTextStyle
        )
    }
}

@Composable
private fun GridItemFooter(
    title: String,
    description: String?,
    colors: ItemColors,
    contentAlpha: Float,
    titleOverflow: TextOverflow,
    descriptionOverflow: TextOverflow,
    footerHeight: Dp,
    textPadding: Dp,
    titleTextStyle: TextStyle,
    descriptionTextStyle: TextStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(footerHeight)
            .background(colors.background)
            .padding(horizontal = textPadding)
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                title,
                style = titleTextStyle,
                color = colors.title,
                maxLines = 1,
                softWrap = false,
                overflow = titleOverflow,
            )
            Text(
                description.orEmpty(),
                style = descriptionTextStyle,
                color = colors.secondaryText,
                maxLines = 1,
                softWrap = false,
                overflow = descriptionOverflow,
            )
        }
    }
}

//endregion

//region Util

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
//endregion