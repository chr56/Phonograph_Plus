/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.model.Song
import player.phonograph.ui.compose.tag.TagBrowserScreen
import player.phonograph.ui.compose.tag.DetailScreenViewModel
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    PhonographTheme(previewMode = true) {
        TagBrowserScreen(
            DetailScreenViewModel(Song.EMPTY_SONG, Color.Black), null
        )
    }
}