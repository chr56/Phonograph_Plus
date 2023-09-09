/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.os.Bundle

class WebSearchActivity : ComposeThemeActivity() {

    val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executeCommand(this, intent)

        setContent {

            val highlightColor by primaryColor.collectAsState()
            PhonographTheme(highlightColor) {
                val scaffoldState = rememberScaffoldState()
                val page by viewModel.navigator.currentPage.collectAsState()
                WebSearch(viewModel, scaffoldState, page)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            val result = viewModel.navigator.navigateUp()
            isEnabled = result
        }
    }

}

