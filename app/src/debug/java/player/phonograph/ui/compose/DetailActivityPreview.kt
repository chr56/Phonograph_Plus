/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.ui.compose.tag.DetailActivityContent
import player.phonograph.ui.compose.tag.DetailModel
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.SongDetailUtil
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    PhonographTheme(previewMode = true) {
        DetailActivityContent(DetailModel().apply { info = SongDetailUtil.SongInfo("name") })
    }
}