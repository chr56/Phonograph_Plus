/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.activity.ThemeActivity
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import android.os.Bundle

class WebSearchActivity : ThemeActivity() {

    private val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.useCustomStatusBar = false
        super.onCreate(savedInstanceState)
    }
}

class WebSearchViewModel : ViewModel() {


}