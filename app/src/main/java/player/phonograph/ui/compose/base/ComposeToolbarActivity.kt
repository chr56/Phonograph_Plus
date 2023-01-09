/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.base

import lib.phonograph.activity.ThemeActivity
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.ui.compose.components.DefaultNavigationIcon
import player.phonograph.ui.compose.components.PhonographAppBar
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.os.Bundle

abstract class ComposeToolbarActivity : ThemeActivity() {
    protected open lateinit var appbarColor: MutableState<Color>
    override fun onCreate(savedInstanceState: Bundle?) {
        useCustomStatusBar = false
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkenColor(ThemeColor.statusBarColor(this))
        window.navigationBarColor = ThemeColor.navigationBarColor(this)
        appbarColor = mutableStateOf(Color(ThemeColor.primaryColor(this)))

        setContent {
            PhonographTheme {
                val backgroundColor by remember { appbarColor }
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar(
                        title = { Text(text = title) },
                        backgroundColor = backgroundColor,
                        navigationIcon = { DefaultNavigationIcon(toolbarBackPressed) },
                        actions = toolbarActions
                    )
                    Surface(color = MaterialTheme.colors.background) {
                        SetUpContent()
                    }
                }
            }
        }
    }

    /**
     * Compose the main user interface (except Toolbar)
     */
    @Composable
    protected abstract fun SetUpContent()
    protected abstract val title: String

    protected open val toolbarBackPressed: () -> (Unit) = {
        onBackPressedDispatcher.onBackPressed()
    }
    protected open val toolbarActions: @Composable (RowScope.() -> Unit) = {}
}