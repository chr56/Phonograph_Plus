/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.util.Log

@Preview(showBackground = true)
@Composable
fun PickerTest() {
    Box(Modifier.fillMaxSize()) {
        val allItems =
            listOf(
                "AAAAAAAAAAAAAAAAAAAA",
                "BBBBBBBBBBBBBBBBBBBB",
                "CCCCCCCCCCCCCCCCCCCC",
                "DDDDDDDDDDDDDDDDDDDD",
                "EEEEEEEEEEEEEEEEEEEE",
                "FFFFFFFFFFFFFFFFFFFF",
                "GGGGGGGGGGGGGGGGGGGG",
                "HHHHHHHHHHHHHHHHHHHH"
            )
        WheelPicker(
            items = allItems,
            initialIndex = 2,
            modifier = Modifier
                .fillMaxSize(0.5f)
                .align(Alignment.Center),
            visibleItemCount = 4.5f
        ) {
            Log.v("PickerTest", it.toString())
        }
    }
}