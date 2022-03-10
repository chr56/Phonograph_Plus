/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import player.phonograph.ui.compose.theme.PhonographTheme

abstract class ToolbarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhonographTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar(title = title, backClick = backClick)
                    Content()
                }
            }
        }
    }

    @Composable
    protected abstract fun Content()

    protected abstract val title: String
    protected abstract val backClick: (() -> (Unit))?
}

@Composable
fun PhonographAppBar(title: String, backClick: (() -> Unit)? = { /* Empty*/ }) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = backClick ?: {}) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}
