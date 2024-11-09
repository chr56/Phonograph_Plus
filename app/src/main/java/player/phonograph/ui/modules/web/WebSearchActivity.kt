/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import player.phonograph.ui.basis.MultiLanguageActivity
import player.phonograph.ui.compose.PhonographTheme
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.os.Bundle

class WebSearchActivity : MultiLanguageActivity() {

    val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executeCommand(this, intent)

        viewModel.selectorMode = intent.getBooleanExtra(EXTRA_SELECTOR_MODE, false)

        setContent {
            PhonographTheme {
                val scaffoldState = rememberScaffoldState()
                val page by viewModel.navigator.currentPage.collectAsState()
                WebSearch(viewModel, scaffoldState, page)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            val result = viewModel.navigator.navigateUp()
            isEnabled = result
        }
        setResult(RESULT_CANCELED)
    }

}

