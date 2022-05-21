/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.compose.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import lib.phonograph.activity.ThemeActivity
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import mt.util.color.isColorLight
import player.phonograph.ui.compose.theme.PhonographTheme

abstract class ComposeToolbarActivity : ThemeActivity() {
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
                        backClick = backClick,
                        actions = toolbarActions,
                        backgroundColor = backgroundColor,
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

    protected open val backClick: (() -> (Unit)) = { onBackPressed() }
    protected open val toolbarActions: @Composable (RowScope.() -> Unit) = {}

    protected open lateinit var appbarColor: MutableState<Color>
}

@Composable
private fun PhonographAppBar(
    title: @Composable () -> Unit,
    backgroundColor: Color,
    backClick: (() -> Unit) = { /* Empty*/ },
    actions: @Composable (RowScope.() -> Unit) = { /* Empty*/ },
) {
    val contentColor = remember {
        mutableStateOf(
            if (isColorLight(backgroundColor.value.toInt())) Color.Black
            else Color.White
        )
    }
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = backClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = contentColor.value
    )
}
