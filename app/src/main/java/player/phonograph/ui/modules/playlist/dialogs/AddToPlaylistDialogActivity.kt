/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import player.phonograph.ui.compose.ComposeThemeActivity
import player.phonograph.ui.compose.PhonographTheme
import androidx.activity.compose.setContent
import android.os.Bundle

class AddToPlaylistDialogActivity : ComposeThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhonographTheme {

            }
        }
    }
}