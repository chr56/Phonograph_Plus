/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.ui.theme.accentColoredButtonStyle
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import kotlin.math.min

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
fun ImageActionMenuDialog(
    state: MaterialDialogState,
    artworkExist: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onView: (() -> Unit)?,
    editMode: Boolean,
) = MaterialDialog(
    dialogState = state,
    buttons = {
        positiveButton(res = android.R.string.ok, textStyle = accentColoredButtonStyle()) { state.hide() }
    }
) {
    title(res = R.string.label_details)
    ImageActionMenu(artworkExist, editMode, onSave, onDelete, onUpdate, onView)
}

@Composable
fun ImageActionMenu(
    artworkExist: Boolean,
    editMode: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onView: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .wrapContentWidth()
    ) {
        if (artworkExist) {
            if (onView != null) {
                MenuItem(textRes = R.string.action_view_image, onView)
            }
            MenuItem(textRes = R.string.action_save, onSave)
            if (editMode) {
                MenuItem(textRes = R.string.action_remove_cover, onDelete)
            }
        }
        if (editMode) {
            MenuItem(textRes = R.string.action_update_image, onUpdate)
        }
    }
}

@Composable
fun ArtworkImageViewer(bitmap: Bitmap, onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        BackHandler(onBack = onDismissRequest)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val density = LocalDensity.current
            val viewportWidth = with(density) { maxWidth.toPx() }
            val viewportHeight = with(density) { maxHeight.toPx() }
            val imageScale = min(viewportWidth / bitmap.width, viewportHeight / bitmap.height)
            val imageWidth = bitmap.width * imageScale
            val imageHeight = bitmap.height * imageScale
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
                val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                val maxOffsetX = ((imageWidth * nextScale - viewportWidth) / 2f).coerceAtLeast(0f)
                val maxOffsetY = ((imageHeight * nextScale - viewportHeight) / 2f).coerceAtLeast(0f)
                scale = nextScale
                offset = if (nextScale == 1f) {
                    Offset.Zero
                } else {
                    Offset(
                        x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                        y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .transformable(transformableState)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_exit),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun MenuItem(@StringRes textRes: Int, onClick: () -> Unit) =
    Text(
        text = stringResource(textRes),
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )