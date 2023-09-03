/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Density
import android.util.Log
import kotlin.math.roundToInt



const val MAIN_CONTENT = "MAIN_CONTENT"

interface ItemRowLayoutScope {
    fun Modifier.maxPercentage(percentage: Float): Modifier
}


/**
 * A Row-like layout, which all children's height is identical to the max height child and width is limited by percentage.
 */
@Composable
fun ItemRowLayout(modifier: Modifier = Modifier, content: @Composable ItemRowLayoutScope.() -> Unit) {
    val scopeImpl = ItemRowLayoutScopeInstance()
    Layout(
        content = { scopeImpl.content() },
        modifier = modifier
    ) { measurables, constraints ->

        val mainContent = measurables.firstOrNull { it.layoutId == MAIN_CONTENT }

        val maxHeight =
            mainContent?.minIntrinsicHeight(constraints.maxWidth)
                ?: measurables.maxOfOrNull { it.minIntrinsicHeight(constraints.maxWidth) } ?: 0

        var totalPercentage = 0f
        val percentageData = measurables.map {
            val percentage = (it.parentData as? LayoutData)?.percentage ?: 1f
            totalPercentage += percentage
            percentage
        }
        if (totalPercentage > 1f) {
            Log.w("ItemLayout", "Total Percentage $totalPercentage is over 1 !")
        }

        val sizeLimitedConstraints = percentageData.map {
            constraints.copy(maxHeight = maxHeight, maxWidth = (constraints.maxWidth * it).roundToInt())
        }

        val placeables = measurables.mapIndexed { i: Int, measurable: Measurable ->
            measurable.measure(sizeLimitedConstraints[i])
        }

        layout(constraints.maxWidth, maxHeight) {
            var x = 0
            for (placeable in placeables) {
                placeable.place(x, 0)
                x += placeable.width
            }
        }
    }
}

data class LayoutData(var percentage: Float = 1f)

private class ItemRowLayoutScopeInstance : ItemRowLayoutScope {
    override fun Modifier.maxPercentage(percentage: Float): Modifier = this.then(LayoutDataModifier(percentage))

    private data class LayoutDataModifier(private val percentage: Float) : ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?): Any =
            ((parentData as? LayoutData) ?: LayoutData()).also { it.percentage = percentage }
    }

}
