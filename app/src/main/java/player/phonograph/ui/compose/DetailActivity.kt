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
import androidx.compose.ui.tooling.preview.Preview
import player.phonograph.R
import player.phonograph.ui.compose.theme.Phonograph_PlusTheme

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Phonograph_PlusTheme {
                DetailActivityUI(title = getString(R.string.label_details))
            }
        }
    }
}

@Composable
fun DetailActivityUI(title: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        PhonographAppBar(title)
        Text(text = "Hello world!")
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewUI() {
    Phonograph_PlusTheme(previewMode = true) {
        DetailActivityUI(title = "Detail")
    }
}

@Composable
fun PhonographAppBar(title: String) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}
