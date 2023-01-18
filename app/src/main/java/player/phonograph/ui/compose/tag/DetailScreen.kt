/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.ui.compose.ColorTools
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.util.SongDetailUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context

@Composable
internal fun DetailActivityContent(viewModel: AbsDetailScreenViewModel, context: Context?) {
    val wrapper by remember { viewModel.artwork }
    val paletteColor =
        ColorTools.makeSureContrastWith(MaterialTheme.colors.surface) {
            if (wrapper != null) {
                Color(wrapper!!.paletteColor)
            } else {
                MaterialTheme.colors.primaryVariant
            }
        }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        if (viewModel.artworkLoaded.value) {
            CoverImage(
                bitmap = wrapper!!.bitmap,
                backgroundColor = paletteColor,
                modifier = Modifier.clickable {
                    viewModel.coverImageDetailDialogState.show()
                }
            )
        }
        InfoTable(viewModel.infoTableViewModel)
    }
    CoverImageDetailDialog(
        state = viewModel.coverImageDetailDialogState,
        artwork = viewModel.artwork.value,
        onSave = { viewModel.saveArtwork(context!!) }
    )
}
@Composable
internal fun CoverImageDetailDialog(
    state: MaterialDialogState,
    artwork: SongDetailUtil.BitmapPaletteWrapper?,
    onSave: () -> Unit
) = MaterialDialog(
    dialogState = state,
    buttons = {
        positiveButton(res = android.R.string.ok) { state.hide() }
    }
) {
    title(res = player.phonograph.R.string.label_details)
    Column(
        modifier = Modifier
            .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
            .wrapContentWidth()
    ) {
        if (artwork != null) {
            Text(
                text = stringResource(player.phonograph.R.string.save),
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(32.dp)
                    .clickable { onSave() },
                textAlign = TextAlign.Start
            )
        }
    }
}