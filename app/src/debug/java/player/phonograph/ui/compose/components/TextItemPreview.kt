/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun TextItemTest() {
    Column(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {

        Row {
            var i = 0
            val colors = listOf(Color.White, Color.Blue, Color.Green, Color.Yellow, Color.Red)
            repeat(32) {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors[i.mod(colors.size)])
                )
                i++
            }
        }
        for (length in 8..28 step 4) {
            val info = "maxLength = $length"
            val strings = listOf("NoSpace", "Text ")
            Text("Test for length $length", style = MaterialTheme.typography.h4)
            for (s in strings) {
                for (i in 1..7 step 2) {
                    val title = s.repeat(i)
                    TextItem(maxLength = length, value = info, title = title)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(128.dp))
        }

    }
}