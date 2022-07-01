/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.base

import android.os.Bundle
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
import lib.phonograph.activity.ThemeActivity
import player.phonograph.App
import player.phonograph.ui.compose.theme.PhonographTheme
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor

abstract class ComposeToolbarActivity : ThemeActivity() {
    private val toolbarViewModel: ToolbarViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        useCustomStatusBar = false
        super.onCreate(savedInstanceState)

        setContent {
            PhonographTheme {
                val backgroundColor by remember { toolbarViewModel.appbarColor }
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar(
                        title = { Text(text = title) },
                        backClick = backClick,
                        actions = {
                            ToolbarActions(this)
                        },
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
    @Composable protected open fun ToolbarActions(rowScope: RowScope) {}

    protected open var appBarColor: Color
        get() = toolbarViewModel.appbarColor.value
        set(value) { toolbarViewModel.appbarColor.value = value }
}

class ToolbarViewModel : ViewModel() {
    var appbarColor: MutableState<Color> = mutableStateOf(Color(ThemeColor.primaryColor(App.instance)))
}

@Composable
private fun PhonographAppBar(
    title: @Composable () -> Unit,
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
