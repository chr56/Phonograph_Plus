/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import player.phonograph.R
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap

@Composable
fun AudioImage(bitmap: Bitmap?, backgroundColor: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor)
    ) {
        val painter = if (bitmap != null) {
            BitmapPainter(bitmap.asImageBitmap())
        } else {
            painterResource(id = R.drawable.default_album_art)
        }
        // Cover Artwork
        Image(
            painter = painter,
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

@Composable
fun ImageActionMenu(
    artworkExist: Boolean,
    editMode: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 48.dp)
            .wrapContentWidth()
    ) {
        if (artworkExist) {
            MenuItem(textRes = R.string.save, onSave)
            if (editMode) {
                MenuItem(textRes = R.string.remove_cover, onDelete)
            }
        }
        if (editMode) {
            MenuItem(textRes = R.string.update_image, onUpdate)
        }
    }
}

@Composable
private fun MenuItem(@StringRes textRes: Int, onClick: () -> Unit) =
    Text(
        text = stringResource(textRes),
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(56.dp)
            .clickable(onClick = onClick),
    )