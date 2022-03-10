/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import player.phonograph.R
import player.phonograph.ui.compose.theme.PhonographTheme

class DetailActivity : ToolbarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun Content() {
        PhonographTheme {
            DetailActivityContent(title = getString(R.string.label_details))
        }
    }

    override val title: String
        get() = getString(R.string.label_details)
    override val backClick: () -> Unit
        get() = { this.onBackPressed() }
}

@Composable
internal fun DetailActivityContent(title: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Hello world!")
    }
}

@Preview(showBackground = true)
@Composable
internal fun PreviewContent() {
    PhonographTheme(previewMode = true) {
        DetailActivityContent(title = "Detail")
    }
}
