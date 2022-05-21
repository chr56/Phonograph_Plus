/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import player.phonograph.App
import player.phonograph.ui.compose.theme.PhonographTheme
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor

abstract class ToolbarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = ColorUtil.darkenColor(ThemeColor.statusBarColor(this))
        window.navigationBarColor = ThemeColor.navigationBarColor(this)

        setContent {
            PhonographTheme {
                val model: ToolbarViewModel by viewModels()
                val backgroundColor by remember {
                    model.appbarColor
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar(
                        title = title,
                        backClick = backClick,
                        actions = toolbarActions,
                        backgroundColor = backgroundColor,
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

    protected open var appBarColor: Color
        get() = ViewModelProvider(this).get(ToolbarViewModel::class.java).appbarColor.value
        set(value) { ViewModelProvider(this).get(ToolbarViewModel::class.java).appbarColor.value = value }
}

class ToolbarViewModel : ViewModel() {
    var appbarColor: MutableState<Color> = mutableStateOf(Color(ThemeColor.primaryColor(App.instance)))
}

@Composable
private fun PhonographAppBar(
    title: String,
    backgroundColor: Color,
    backClick: (() -> Unit) = { /* Empty*/ },
    actions: @Composable (RowScope.() -> Unit) = { /* Empty*/ }
) {
    val contentColor = remember {
        mutableStateOf(
            if (ColorUtil.isColorLight(backgroundColor.value.toInt()))
                Color.Black else Color.White
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
        backgroundColor = backgroundColor,
        contentColor = contentColor.value
    )
}
