/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class TagEditorActivity : ComposeToolbarActivity() {
    @Composable
    override fun SetUpContent() {
        TagEditorActivityContent(Color(primaryColor))
    }

    override val title: String get() = getString(R.string.action_tag_editor)
}

@Composable
fun TagEditorActivityContent(titleColor: Color) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
    }
}