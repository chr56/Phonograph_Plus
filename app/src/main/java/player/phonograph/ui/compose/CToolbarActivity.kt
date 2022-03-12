/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.ui.compose.theme.PhonographTheme

abstract class ToolbarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkenColor(ThemeColor.statusBarColor(this))
        window.navigationBarColor = ThemeColor.navigationBarColor(this)

        setContent {
            PhonographTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar(title = title, backClick = backClick, actions = toolbarActions)
                    Surface(color = MaterialTheme.colors.background) {
                        Content()
                    }
                }
            }
        }
    }

    @Composable
    protected abstract fun Content()

    protected open val backClick: (() -> (Unit)) = { onBackPressed() }
    protected open val toolbarActions: @Composable (RowScope.() -> Unit) = {}
    protected abstract val title: String
}

@Composable
private fun PhonographAppBar(title: String, backClick: (() -> Unit) = { /* Empty*/ }, actions: @Composable (RowScope.() -> Unit) = { /* Empty*/ }) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = backClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        actions = actions,
    )
}
