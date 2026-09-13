/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.R
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.resource.interpreter
import player.phonograph.ui.resource.text
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.collections.plus
import kotlinx.coroutines.launch

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

@Composable
fun DragHandle(
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
fun ItemMenuButton(
    width: Dp,
    iconColor: Color,
    onClick: () -> Unit,
    menuContent: (@Composable () -> Unit)? = null,
) {
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
        menuContent?.invoke()
    }
}


@Composable
fun ItemMenu(
    expanded: Boolean,
    entries: List<ActionEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var groups by remember(expanded, entries) { mutableStateOf<List<ActionEntry.GroupedEntries>>(emptyList()) }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val group = groups.lastOrNull()
        if (group != null) {
            DropdownMenuItem(onClick = { groups = groups.dropLast(1) }, modifier = modifier) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                Text(group.semantic.interpreter().text(), modifier = textModifier)
            }
        }
        for (entry in group?.entries ?: entries) {
            DropdownMenuItem(
                onClick = {
                    when (entry) {
                        is ActionEntry.GroupedEntries -> groups = groups + entry
                        is ActionEntry.ToolbarEntry   -> {
                            onDismiss()
                            scope.launch { entry.action() }
                        }
                    }
                },
                modifier = modifier,
            ) {
                Text(
                    entry.semantic.interpreter().text(),
                    modifier = textModifier,
                )
            }
        }
    }
}
