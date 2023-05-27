/*
 *  Copyright (c) 2023 chr_56
 *
 */

package player.phonograph.ui.compose.settings

import player.phonograph.R
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

class SettingsActivity : ComposeToolbarActivity() {

    @Composable
    override fun SetUpContent() {
        PhonographPreferenceScreen()
    }

    override val title: String
        get() = getString(R.string.action_settings)

    override val toolbarActions: @Composable RowScope.() -> Unit = @Composable {
        IconButton(
            content = {
                Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_actions))
            },
            onClick = {
                //todo
            }
        )
    }
}