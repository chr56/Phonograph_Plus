/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

@Composable
fun ItemList(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 72.dp,
    showDragHandle: Boolean = false,
    leadingModifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    descriptionOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithLeading(
    style = ItemDefaults.listLayoutStyle(height = 72.dp),
    title = title,
    secondary = listOf(description),
    modifier = modifier,
    leadingContent = leadingContent,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    leadingModifier = leadingModifier,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow, descriptionOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemListExtended(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 72.dp,
    showDragHandle: Boolean = false,
    leadingModifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.MiddleEllipsis,
    descriptionOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithLeading(
    style = ItemDefaults.listLayoutStyle(
        height = 84.dp, titleLines = 3, secondaryLines = 2,
        menuWidth = 24.dp, contentEndPadding = 8.dp,
    ),
    title = title,
    secondary = listOf(description),
    modifier = modifier,
    leadingContent = leadingContent,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    leadingModifier = leadingModifier,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow, descriptionOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemListSingleRow(
    title: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 72.dp,
    showDragHandle: Boolean = false,
    leadingModifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithLeading(
    style = ItemDefaults.listLayoutStyle(height = 64.dp),
    title = title,
    secondary = emptyList(),
    modifier = modifier,
    leadingContent = leadingContent,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    leadingModifier = leadingModifier,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemListNoImage(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 0.dp,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    descriptionOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithoutLeading(
    style = ItemDefaults.listLayoutStyle(
        height = 72.dp,
        contentStartPadding = 8.dp, contentEndPadding = 8.dp,
    ),
    title = title,
    secondary = listOf(description),
    modifier = modifier,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow, descriptionOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemList3L(
    title: String,
    secondaryText: String?,
    tertiaryText: String?,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 72.dp,
    showDragHandle: Boolean = false,
    leadingModifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    secondaryTextOverflow: TextOverflow = TextOverflow.Clip,
    tertiaryTextOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithLeading(
    style = ItemDefaults.listLayoutStyle(height = 72.dp),
    title = title,
    secondary = listOf(secondaryText, tertiaryText),
    modifier = modifier,
    leadingContent = leadingContent,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    leadingModifier = leadingModifier,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow, secondaryTextOverflow, tertiaryTextOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemList3LExtended(
    title: String,
    secondaryText: String?,
    tertiaryText: String?,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 72.dp,
    showDragHandle: Boolean = false,
    leadingModifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.MiddleEllipsis,
    secondaryTextOverflow: TextOverflow = TextOverflow.Clip,
    tertiaryTextOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithLeading(
    style = ItemDefaults.listLayoutStyle(
        height = 84.dp, titleLines = 3, secondaryLines = 2,
        menuWidth = 24.dp, contentEndPadding = 8.dp,
    ),
    title = title,
    secondary = listOf(secondaryText, tertiaryText),
    modifier = modifier,
    leadingContent = leadingContent,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    leadingModifier = leadingModifier,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow, secondaryTextOverflow, tertiaryTextOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemList3LNoImage(
    title: String,
    secondaryText: String?,
    tertiaryText: String?,
    modifier: Modifier = Modifier,
    colors: ItemColors = ItemDefaults.listColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    divider: Dp? = 0.dp,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    titleOverflow: TextOverflow = TextOverflow.MiddleEllipsis,
    secondaryTextOverflow: TextOverflow = TextOverflow.Clip,
    tertiaryTextOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) = ListItemWithoutLeading(
    style = ItemDefaults.listLayoutStyle(
        height = 84.dp, titleLines = 3, secondaryLines = 2,
        menuWidth = 36.dp, contentEndPadding = 0.dp,
    ),
    title = title,
    secondary = listOf(secondaryText, tertiaryText),
    modifier = modifier,
    colors = colors,
    titleTextStyle = ItemDefaults.titleStyle,
    secondaryTextStyle = ItemDefaults.secondaryStyle,
    selected = selected,
    contentAlpha = contentAlpha,
    divider = divider,
    showDragHandle = showDragHandle,
    dragHandleModifier = dragHandleModifier,
    textOverflows = listOf(titleOverflow, secondaryTextOverflow, tertiaryTextOverflow),
    onClick = onClick,
    onLongClick = onLongClick,
    onMenuClick = onMenuClick,
)

@Composable
fun ItemGrid(
    title: String,
    description: String?,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    colors: ItemColors = ItemDefaults.gridColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    descriptionOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) = ItemGrid(
    title = title,
    description = description,
    modifier = modifier
        .padding(2.dp)
        .fillMaxWidth(),
    artwork = artwork,
    colors = colors,
    selected = selected,
    contentAlpha = contentAlpha.coerceIn(0f, 1f),
    titleOverflow = titleOverflow,
    descriptionOverflow = descriptionOverflow,
    onClick = onClick,
    onLongClick = onLongClick,
    footerHeight = 68.dp,
    textPadding = 16.dp,
    titleStyle = ItemDefaults.gridTitleStyle,
    descriptionTextStyle = ItemDefaults.captionStyle,
)

@Composable
fun ItemGridCardHorizontal(
    title: String,
    description: String?,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    colors: ItemColors = ItemDefaults.cardColors(),
    selected: Boolean = false,
    contentAlpha: Float = 1f,
    titleOverflow: TextOverflow = TextOverflow.Clip,
    descriptionOverflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(2.dp)
    ItemGrid(
        title = title,
        description = description,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .width(104.dp)
            .height(148.dp)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(colors.background),
        artwork = artwork,
        colors = colors,
        selected = selected,
        contentAlpha = contentAlpha.coerceIn(0f, 1f),
        titleOverflow = titleOverflow,
        descriptionOverflow = descriptionOverflow,
        onClick = onClick,
        onLongClick = onLongClick,
        footerHeight = 44.dp,
        textPadding = 8.dp,
        titleStyle = ItemDefaults.cardTitleStyle,
        descriptionTextStyle = ItemDefaults.captionStyle,
    )
}


