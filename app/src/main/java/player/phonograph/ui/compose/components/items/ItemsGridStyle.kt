/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

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
