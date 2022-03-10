/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import player.phonograph.ui.compose.theme.Phonograph_PlusTheme

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Phonograph_PlusTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    PhonographAppBar()
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Phonograph_PlusTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun PhonographAppBar() {
    Phonograph_PlusTheme {
        TopAppBar {}
    }
}
