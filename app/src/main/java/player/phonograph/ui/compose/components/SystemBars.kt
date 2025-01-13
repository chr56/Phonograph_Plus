/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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