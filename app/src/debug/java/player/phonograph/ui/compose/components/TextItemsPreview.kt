/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun VerticalTextItemPreview1() {
    VerticalTextItem(label = "VerticalTextItem-Label", value = "VerticalTextItem-Value")
}
@Preview(showBackground = true)
@Composable
fun VerticalTextItemPreview2() {
    VerticalTextItem(label = "VerticalTextItem-Label", Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Content("VerticalTextItem-Content")
    }
}

@Preview(showBackground = true)
@Composable
fun HorizontalTextItemPreview1() {
    HorizontalTextItem(label = "HorizontalTextItem-Label", value = "HorizontalTextItem-Value")
}

@Preview(showBackground = true)
@Composable
fun HorizontalTextItemPreview2() {
    HorizontalTextItem(label = "HorizontalTextItem-Label") {
        Content("HorizontalTextItem-Content")
    }
}

@Composable
private fun Content(text: String) {
    Box(
        Modifier
            .background(Color.Gray)
            .padding(12.dp)
    ) {
        Text(text)
    }
}