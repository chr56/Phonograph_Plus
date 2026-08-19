/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

class ActionItem(
    val imageVector: ImageVector? = null,
    val imageRes: Int = -1,
    val text: String? = null,
    val textRes: Int = -1,
    val tint: Color? = null,
    val onClick: () -> Unit,
) {
    @Composable
    fun iconPainter(): Painter = if (imageVector != null) {
        rememberVectorPainter(imageVector)
    } else {
        painterResource(imageRes)
    }

    @Composable
    fun text(): String? = if (textRes > 0) stringResource(textRes) else text
}