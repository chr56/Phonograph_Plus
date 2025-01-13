/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

/**
 * Added colored paddings of SystemBars insets
 * @param color bar color
 */
@Composable
fun SystemBarsPadded(
    color: Color = MaterialTheme.colors.primaryVariant,
    withNavigationBar: Boolean = false,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {

        Configuration.ORIENTATION_LANDSCAPE -> {
            // LandscapeLeftBarStub(color)
            // LandscapeRightBarStub(color)
            Box(Modifier.systemBarsPadding()) {
                content()
            }
        }

        else                                -> {
            StatusBarStub(color)
            if (withNavigationBar) {
                // NavigationBarStub(color)
                Box(Modifier.systemBarsPadding()) {
                    content()
                }
            } else {
                Box(Modifier.statusBarsPadding()) {
                    content()
                }
            }
        }
    }
}

@Composable
fun StatusBarStub(color: Color = MaterialTheme.colors.primaryVariant) {
    Box(
        Modifier
            .background(color)
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .fillMaxWidth()
    )
}

@Composable
fun NavigationBarStub(color: Color = MaterialTheme.colors.primaryVariant) {
    Box(
        Modifier
            .background(color)
            .windowInsetsBottomHeight(WindowInsets.navigationBars)
            .fillMaxWidth()
    )
}

@Composable
fun LandscapeLeftBarStub(color: Color = MaterialTheme.colors.primaryVariant) {
    Box(
        Modifier
            .background(color)
            .windowInsetsStartWidth(WindowInsets.systemBars)
            .fillMaxHeight()
    )
}

@Composable
fun LandscapeRightBarStub(color: Color = MaterialTheme.colors.primaryVariant) {
    Box(
        Modifier
            .background(color)
            .windowInsetsEndWidth(WindowInsets.systemBars)
            .fillMaxHeight()
    )
}