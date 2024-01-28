/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun CascadeVerticalItem(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerColumnModifier: Modifier = Modifier,
    collapsible: Boolean = true,
    collapsed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    CascadeItem(modifier.padding(vertical = 8.dp), title, textStyle, Modifier, collapsible, collapsed) {
        Column(innerColumnModifier.padding(start = 8.dp)) {
            content()
        }
    }
}
@Composable
fun CascadeHorizontalItem(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerRowModifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CascadeItem(modifier.padding(vertical = 4.dp), title, textStyle, Modifier, collapsible = false, collapsed = false) {
        Row(
            innerRowModifier
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CascadeFlowRow(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerRowModifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CascadeItem(
        modifier = modifier,
        title = title,
        textStyle = textStyle,
        textModifier = innerRowModifier,
        collapsible = true,
        collapsed = false
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}
@Composable
fun CascadeItem(
    modifier: Modifier,
    title: String,
    textStyle: TextStyle,
    textModifier: Modifier,
    collapsible: Boolean,
    collapsed: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier, verticalArrangement = Arrangement.SpaceEvenly
    ) {
        var collapseState by remember { mutableStateOf(collapsed) }
        Row(
            Modifier
                .clickable {
                    collapseState = !collapseState
                }
        ) {
            if (collapsible) {
                Icon(
                    if (collapseState) Icons.AutoMirrored.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                    "Collapse",
                    Modifier.align(Alignment.CenterVertically)
                )
            }
            Text(
                title,
                modifier = textModifier
                    .padding(vertical = 2.dp)
                    .align(Alignment.Top)
                    .fillMaxWidth(),
                style = textStyle,
            )
        }
        AnimatedVisibility(!collapsible || !collapseState) {
            content()
        }
        Spacer(Modifier.height(4.dp))
    }
}