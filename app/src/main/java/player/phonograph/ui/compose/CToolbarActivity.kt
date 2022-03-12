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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import mt.util.color.isColorLight
import player.phonograph.ui.compose.theme.PhonographTheme

abstract class ToolbarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkenColor(ThemeColor.statusBarColor(this))
        window.navigationBarColor = ThemeColor.navigationBarColor(this)

        setContent {
            PhonographTheme {
                _appbarColor = remember {
                    mutableStateOf(Color(ThemeColor.primaryColor(this)))
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar(
                        title = title,
                        backClick = backClick,
                        actions = toolbarActions,
                        backgroundColor = appBarColor,
                    )
                    Surface(color = MaterialTheme.colors.background) {
                        Content()
                    }
                }
            }
        }
    }

    @Composable
    protected abstract fun Content()
    protected abstract val title: String

    protected open val backClick: (() -> (Unit)) = { onBackPressed() }
    protected open val toolbarActions: @Composable (RowScope.() -> Unit) = {}

    private lateinit var _appbarColor: MutableState<Color>
    protected open var appBarColor: MutableState<Color>
        get() = _appbarColor
        set(value) {
            _appbarColor = value
        }
}

@Composable
private fun PhonographAppBar(
    title: String,
    backgroundColor: MutableState<Color>,
    backClick: (() -> Unit) = { /* Empty*/ },
    actions: @Composable (RowScope.() -> Unit) = { /* Empty*/ },
) {
    val contentColor = remember {
        mutableStateOf(
            if (isColorLight(backgroundColor.value.value.toInt())) Color.Black
            else Color.White
        )
    }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = backClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        actions = actions,
        backgroundColor = backgroundColor.value,
        contentColor = contentColor.value
    )
}
