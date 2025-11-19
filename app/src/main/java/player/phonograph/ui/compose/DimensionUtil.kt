/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.util.ui.isTablet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * Max height of a dialog based on screen size
 */
@Composable
fun dialogMaxHeight(container: WindowInfo): Dp {
    val containerSize = container.containerSize
    val maxHeight = if (!isTablet()) {
        containerSize.height.dp - DIALOG_VERTICAL_MARGIN.dp
    } else {
        DIALOG_MAX_HEIGHT_TABLET.dp
    }
    return maxHeight
}

/**
 * Horizontal padding of a dialog based on screen size
 */
@Composable
fun dialogHorizontalPadding(container: WindowInfo, force: Boolean = false): Dp {
    val containerSize = container.containerSize
    val remains = containerSize.width - DIALOG_SUPPOSED_WIDTH
    if (remains <= 0) {
        return if (force) DIALOG_HORIZONTAL_PADDING.dp else 0.dp
    }
    val ratio: Int = DIALOG_SUPPOSED_WIDTH / remains
    return if (ratio > 8) {
        (4 * DIALOG_HORIZONTAL_PADDING).dp
    } else if (ratio > 6) {
        (3 * DIALOG_HORIZONTAL_PADDING).dp
    } else if (ratio > 4) {
        (2 * DIALOG_HORIZONTAL_PADDING).dp
    } else {
        DIALOG_HORIZONTAL_PADDING.dp
    }
}

@Composable
fun isTablet(): Boolean = isTablet(LocalResources.current)

private const val DIALOG_MAX_HEIGHT_TABLET = 640
private const val DIALOG_VERTICAL_MARGIN = 96

private const val DIALOG_HORIZONTAL_PADDING = 16
private const val DIALOG_SUPPOSED_WIDTH = 360