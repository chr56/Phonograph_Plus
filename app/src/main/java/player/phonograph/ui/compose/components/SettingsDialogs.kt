/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp



@Composable
fun SettingsDialogFrame(
    modifier: Modifier,
    title: String,
    onDismissRequest: () -> Unit,
    actions: List<ActionItem>,
    innerShadow: Boolean = false,
    scrollable: Boolean = false,
    content: @Composable () -> Unit,
) {
    AdvancedDialogFrame(
        modifier = modifier,
        title = stringResource(R.string.action_settings),
        onDismissRequest = onDismissRequest,
        actions = actions,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 24.dp, end = 16.dp)
        )
        MainContent(innerShadow = innerShadow, scrollable = scrollable, content = content)
    }
}


@Composable
private fun MainContent(
    innerShadow: Boolean,
    scrollable: Boolean,
    content: @Composable (() -> Unit),
) {
    when {
        innerShadow && scrollable -> {
            InnerShadowBox(modifier = Modifier.heightIn(min = 120.dp, max = 480.dp)) {
                ScrollableContent(content = content)
            }
        }

        !innerShadow && scrollable -> {
            Box(modifier = Modifier.heightIn(min = 120.dp, max = 480.dp)) {
                ScrollableContent(content = content)
            }
        }

        innerShadow && !scrollable -> {
            InnerShadowBox(content = content)
        }

        else -> {
            content()
        }
    }
}

@Composable
private fun ScrollableContent(content: @Composable (() -> Unit)) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        content()
    }
}

@Composable
fun InnerShadowBox(
    modifier: Modifier = Modifier,
    innerShadow: Dp = 4.dp,
    topPadding: Dp = 8.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable (() -> Unit),
) {
    val density = LocalDensity.current
    val shadowRadius = remember(density, innerShadow) {
        with(density) { innerShadow.toPx() }
    }
    val shadowBrush = remember {
        Brush.verticalGradient(listOf(Color.DarkGray, Color.Transparent, Color.DarkGray))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding)
            .innerShadow(RectangleShape) {
                alpha = 0.7f
                radius = shadowRadius
                brush = shadowBrush
            }
    ) {
        content()
    }
}