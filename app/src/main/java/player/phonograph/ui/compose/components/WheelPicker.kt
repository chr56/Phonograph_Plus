/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:OptIn(ExperimentalFoundationApi::class)

package player.phonograph.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 64.dp,
    visibleItemCount: Float = 3f,
    textStyle: TextStyle = MaterialTheme.typography.body1,
    onSelected: (Int) -> Unit,
) {
    val state = rememberPagerState { items.size }
    val current = snapshotFlow { state.currentPage }
    LaunchedEffect(state) {
        state.scrollToPage(initialIndex)
        current.collectLatest { onSelected(it) }
    }
    BoxWithConstraints(modifier) {

        val paddingValues: PaddingValues = remember(itemHeight, visibleItemCount) {
            val half = (visibleItemCount - 1) / 2
            PaddingValues(vertical = itemHeight * half)
        }

        val flingBehavior =
            PagerDefaults.flingBehavior(
                state = state,
                pagerSnapDistance = PagerSnapDistance.atMost(items.size / 2)
            )


        VerticalPager(
            state = state,
            pageSize = PageSize.Fixed(itemHeight),
            contentPadding = paddingValues,
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .height(itemHeight * visibleItemCount)
                .scrim(defaultScrimMask)
                .align(Alignment.Center),
        ) { index ->
            Box(Modifier.fillMaxSize()) {
                Text(
                    text = items[index],
                    textAlign = TextAlign.Center,
                    style = textStyle,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                )
            }
        }
        Divider(
            Modifier
                .align(Alignment.Center)
                .offset(y = -itemHeight / 2),
            thickness = 2.dp
        )
        Divider(
            Modifier
                .align(Alignment.Center)
                .offset(y = itemHeight / 2),
            thickness = 2.dp
        )
    }
}


private fun Modifier.scrim(scrimMask: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(scrimMask, blendMode = BlendMode.DstIn)
    }

private val defaultScrimMask =
    Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.5f to Color.Black,
        1.0f to Color.Transparent,
    )