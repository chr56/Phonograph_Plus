/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.display

import player.phonograph.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    painter: Painter? = null,
    colorFilter: ColorFilter? = null,
) {
    ListItemFrame(
        modifier = modifier.wrapContentHeight(Alignment.Top),
        mainContent = {
            TwoLinesContent(title, subtitle, onClick)
        },
        left = {
            if (painter != null) Icon(painter, colorFilter)
        },
        right = if (onMenuClick != null) {
            { Menu(onMenuClick) }
        } else {
            null
        },
    )
}

@Composable
private fun TwoLinesContent(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(16.dp, 8.dp)
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.body1)
        Text(subtitle, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun BoxScope.Icon(
    painter: Painter?,
    colorFilter: ColorFilter?,
) {
    Box(
        Modifier
            .padding(8.dp)
            .fillMaxSize()
    ) {
        if (painter != null)
            Image(
                painter, null,
                Modifier.align(Alignment.Center),
                colorFilter = colorFilter
            )
    }

}


@Composable
private fun Menu(onMenuClick: () -> Unit) {
    Icon(
        Icons.Outlined.MoreVert,
        stringResource(R.string.action_more),
        Modifier
            .fillMaxSize(0.7f)
            .clickable(onClick = onMenuClick)
            .padding(8.dp)
    )
}


@Composable
private fun ListItemFrame(
    modifier: Modifier = Modifier,
    mainContent: @Composable BoxScope.() -> Unit,
    left: (@Composable BoxScope.() -> Unit)? = null,
    right: (@Composable BoxScope.() -> Unit)? = null,
) {
    ItemRowLayout(modifier) {
        if (left != null) Box(Modifier.maxPercentage(0.15f)) { left() }
        Box(
            Modifier
                .maxPercentage(if (right != null) 0.7f else 0.85f)
                .layoutId(MAIN_CONTENT)
        ) {
            mainContent()
        }
        if (right != null) Box(Modifier.maxPercentage(0.15f)) { right() }
    }
}