/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun LabeledItemLayout(
    modifier: Modifier = Modifier,
    label: String,
    labelStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    labelModifier: Modifier = Modifier.padding(8.dp),
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Text(text = label, style = labelStyle, modifier = labelModifier)
            content()
        }
    ) { measurables, constraints ->

        val labelText = measurables[0]
        val mainContent = measurables[1]

        val labelWidth = labelText.maxIntrinsicWidth(constraints.maxHeight)

        val contentWidth = mainContent.maxIntrinsicWidth(constraints.maxHeight)
        val contentHeight = mainContent.maxIntrinsicHeight(contentWidth)

        val shouldUseVerticalLayout =
            when {
                contentHeight > (contentWidth * 2) -> true // content too long (height)
                labelWidth > (constraints.maxWidth / 2) -> true //label too long
                labelWidth + contentWidth > constraints.maxWidth -> true
                else -> false
            }

        if (shouldUseVerticalLayout) {
            // vertical
            val labelTextPlaceable =
                labelText.measure(constraints)
            val leftHeight =
                if (constraints.maxHeight != Constraints.Infinity) constraints.maxHeight - labelTextPlaceable.height else constraints.maxHeight
            val mainContentPlaceable =
                mainContent.measure(constraints.copy(maxHeight = leftHeight))

            val height = labelTextPlaceable.height + mainContentPlaceable.height
            val width = max(labelTextPlaceable.width, mainContentPlaceable.width)
            layout(width, height) {
                labelTextPlaceable.place(0, 0)
                mainContentPlaceable.place(0, labelTextPlaceable.height)
            }
        } else {
            // horizontal
            val labelTextPlaceable =
                labelText.measure(constraints)
            val leftWidth =
                if (constraints.maxWidth != Constraints.Infinity) constraints.maxWidth - labelTextPlaceable.width else constraints.maxWidth
            val mainContentPlaceable =
                mainContent.measure(constraints.copy(maxWidth = leftWidth))

            val height = max(labelTextPlaceable.height, mainContentPlaceable.height)
            val width = labelTextPlaceable.width + mainContentPlaceable.width
            layout(width, height) {
                labelTextPlaceable.place(0, 0)
                mainContentPlaceable.place(labelTextPlaceable.width, 0)
            }
        }
    }
}


object LabeledItemLayoutDefault {
    val titleStyle: TextStyle get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp)
}