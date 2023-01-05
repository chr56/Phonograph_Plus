/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import android.graphics.Bitmap

@Composable
internal fun CoverImage(bitmap: Bitmap, backgroundColor: Color, showCover: Boolean) {
    if (showCover) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(backgroundColor)
        ) {
            // Cover Artwork
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = "Cover",
                modifier = Modifier
                    .align(Alignment.Center)
                    .sizeIn(
                        maxWidth = maxWidth,
                        maxHeight = maxWidth,
                        minHeight = maxWidth.div(3)
                    )
            )
        }
    }
}