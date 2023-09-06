/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.BreakIterator
import java.text.StringCharacterIterator



@Composable
fun TextItem(modifier: Modifier = Modifier, title: String, maxLength: Int, value: String,) {
    TextItem(modifier, title, maxLength) {
        SelectionContainer {
            Text(
                text = value,
                style = TextStyle(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// todo use layout
@Composable
fun TextItem(modifier: Modifier, title: String, maxLength: Int, content: @Composable (Modifier) -> Unit) {

    val length = TextItemDefaults.calculateLength(title)

    val titleStyle = TextItemDefaults.titleStyle

    val density = LocalDensity.current
    val titleWidth = with(density) {
        (titleStyle.fontSize.toDp()) * maxLength //todo
    }

    if (length < maxLength) {
        // horizontal
        Row(
            modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextItemTitle(modifier = Modifier.align(Alignment.Top), title, titleWidth, titleStyle)
            content(Modifier.align(Alignment.Top))
        }
    } else {
        // vertical
        Column(
            modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // title
            TextItemTitle(modifier = Modifier.align(Alignment.Start), title, Dp.Unspecified, titleStyle)
            // content
            content(Modifier.align(Alignment.Start))
        }
    }
}

@Composable
private fun TextItemTitle(
    modifier: Modifier,
    title: String,
    titleWidth: Dp,
    style: TextStyle,
) {
    Text(
        text = title,
        style = style,
        modifier = modifier.width(titleWidth),
    )
}

object TextItemDefaults {

    fun title(modifier: Modifier, title: String, titleWidth: Dp): @Composable () -> Unit = {
        TextItemTitle(
            modifier, title, titleWidth, titleStyle
        )
    }

    val titleStyle: TextStyle get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp)


    /** @see [androidx.compose.ui.text.android.minIntrinsicWidth] **/
    fun calculateLength(string: String): Int {
        val breakIterator = BreakIterator.getWordInstance()
        breakIterator.text = StringCharacterIterator(string)

        var maxLength = 0

        var start = 0
        var end = breakIterator.next()
        while (end != BreakIterator.DONE) {
            val current = end - start
            if (current > 0 && current >= maxLength) {
                maxLength = current
            }
            start = end
            end = breakIterator.next()
        }

        return maxLength
    }

}
