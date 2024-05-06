/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import lib.phonograph.activity.MultiLanguageActivity
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.updateAllSystemUIColors
import android.os.Bundle
import android.os.PersistableBundle

abstract class ComposeThemeActivity : MultiLanguageActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateAllSystemUIColors(this, primaryColor())
    }
}