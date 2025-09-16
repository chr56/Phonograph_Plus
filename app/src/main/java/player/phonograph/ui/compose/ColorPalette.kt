/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.accentColorFlow
import player.phonograph.util.theme.primaryColorFlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun defaultColorPalette(): ColorPalette {
    val previewMode = LocalInspectionMode.current
    val colorPalette: ColorPalette = if (previewMode) {
        PreviewColorPalette
    } else {
        ThemeColorPalette(LocalContext.current)
    }
    return colorPalette
}

sealed interface ColorPalette {
    @Composable
    fun primaryColor(context: Context): State<Color>

    @Composable
    fun accentColor(context: Context): State<Color>
}

class ThemeColorPalette(context: Context) : ColorPalette {

    private val primaryColorFlow: Flow<Color> = primaryColorFlow(context).map { Color(it) }
    private val accentColorFlow: Flow<Color> = accentColorFlow(context).map { Color(it) }

    @Composable
    override fun primaryColor(context: Context): State<Color> =
        primaryColorFlow.collectAsState(initial = Color(ThemeSetting.primaryColor(context)))

    @Composable
    override fun accentColor(context: Context): State<Color> =
        accentColorFlow.collectAsState(initial = Color(ThemeSetting.accentColor(context)))
}

@Suppress("ConvertObjectToDataObject")
object PreviewColorPalette : ColorPalette {

    @Composable
    override fun primaryColor(context: Context): State<Color> =
        remember { mutableStateOf(Color(0xFF2962FF)) }


    @Composable
    override fun accentColor(context: Context): State<Color> =
        remember { mutableStateOf(Color(0xFFFF7043)) }
}