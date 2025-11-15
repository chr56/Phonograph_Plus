/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import dev.zt64.compose.pipette.ColorPickerDefaults
import dev.zt64.compose.pipette.HsvColor
import dev.zt64.compose.pipette.SquareColorPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


@Composable
fun ColorPicker(
    selected: Color,
    modifier: Modifier = Modifier,
    onColorChanged: (Color) -> Unit ={},
) {
    Column(modifier.fillMaxWidth()) {
        var color by remember { mutableStateOf(HsvColor(selected)) }
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            SquareColorPicker(
                color = color,
                onColorChange = { color = it },
                onColorChangeFinished = {
                    onColorChanged(color.toColor())
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            )
            Spacer(Modifier.width(4.dp))
            ColorHueVerticalSliderBar(
                color = color,
                onColorChange = { color = it },
                onColorChangeFinished = {
                    onColorChanged(color.toColor())
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            )
        }

        ColorRGBHexEditor(
            color = color,
            onColorChange = { color = it },
            onColorChangeFinished = {
                onColorChanged(color.toColor())
            },
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ColorHueVerticalSliderBar(
    color: HsvColor,
    onColorChange: (color: HsvColor) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable () -> Unit = {
        ColorPickerDefaults.Thumb(color.toColor(), interactionSource)
    },
    onColorChangeFinished: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val updatedColor by rememberUpdatedState(color)
    val colors = remember(updatedColor.saturation, updatedColor.value) {
        val saturation = updatedColor.saturation
        val value = updatedColor.value
        (0..360 step 15).map { h ->
            Color.hsv(h.toFloat(), saturation, value)
        }
    }
    val brush = Brush.verticalGradient(colors)

    fun lookupPosition(offset: Offset, size: IntSize): Float {
        val clampedY = offset.y.coerceIn(0f, size.height.toFloat())
        return 360f * (clampedY / size.height)
    }

    Box(
        modifier = modifier
            .height(128.dp)
            .width(24.dp)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val hue = lookupPosition(it, size)
                        onColorChange(updatedColor.copy(hue = hue))
                        onColorChangeFinished()
                    }
                )
            }
            .pointerInput(Unit) {
                var interaction: DragInteraction.Start? = null
                detectDragGestures(
                    onDragStart = {
                        scope.launch {
                            interaction = DragInteraction.Start()
                            interactionSource.emit(interaction)
                        }
                    },
                    onDrag = { change, _ ->
                        val hue = lookupPosition(change.position, size)
                        onColorChange(updatedColor.copy(hue = hue))
                    },
                    onDragEnd = {
                        scope.launch {
                            interaction?.let {
                                interactionSource.emit(DragInteraction.Stop(it))
                            }
                        }
                        onColorChangeFinished()
                    },
                    onDragCancel = {
                        scope.launch {
                            interaction?.let {
                                interactionSource.emit(DragInteraction.Cancel(it))
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .background(brush)
        )

        Box(
            modifier = Modifier.offset {
                IntOffset(
                    x = size.width / 2,
                    y = ((updatedColor.hue / 360) * size.height).roundToInt()
                )
            }
        ) {
            thumb()
        }
    }
}


@Composable
private fun ColorRGBHexEditor(
    color: HsvColor,
    onColorChange: (HsvColor) -> Unit,
    onColorChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    var hex by remember { mutableStateOf(colorToHex(color.toColor())) }
    var isEditing by remember { mutableStateOf(false) }
    LaunchedEffect(color) {
        if (!isEditing) {
            hex = colorToHex(color.toColor())
        }
    }

    val backgroundColor = remember(color) { color.toColor() }
    val foregroundColor = remember(color) {
        if (color.value > 0.5 && color.saturation < 0.6) Color.Black else Color.White
    }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 96.dp)
            .background(backgroundColor)
    ) {
        BasicTextField(
            value = hex,
            onValueChange = { updated ->
                hex = updated
                val updatedColor = hexToColor(updated)
                if (updatedColor != null) {
                    onColorChange(HsvColor(updatedColor))
                }
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .heightIn(min = 24.dp)
                .padding(vertical = 8.dp)
                .onFocusChanged {
                    isEditing = it.isFocused
                    if (!it.isFocused) {
                        onColorChangeFinished()
                    }
                },
            textStyle = MaterialTheme.typography.h6.copy(
                textAlign = TextAlign.Center, color = foregroundColor
            ),
            singleLine = true,
        )
    }
}

private fun colorToHex(color: Color): String {
    return sequenceOf(color.red, color.green, color.blue).map {
        (it * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
    }.joinToString(separator = "", prefix = "#")
}

private fun hexToColor(hex: String): Color? {
    val value = hex.removePrefix("#").toLongOrNull(16) ?: -1
    return if (value > 0) {
        Color(value)
    } else {
        null
    }
}