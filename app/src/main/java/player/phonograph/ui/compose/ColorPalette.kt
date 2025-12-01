/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.util.theme.ThemeSettingsDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun defaultColorPalette(): ColorPalette =
    if (LocalInspectionMode.current) PreviewColorPalette else ThemeColorPalette

sealed interface ColorPalette {
    @Composable
    fun primaryColor(context: Context): State<Color>

    @Composable
    fun accentColor(context: Context): State<Color>
}

object ThemeColorPalette : ColorPalette {

    private val primaryColorFlow: Flow<Color> = ThemeSettingsDelegate.primaryColor.map { Color(it) }
    private val accentColorFlow: Flow<Color> = ThemeSettingsDelegate.accentColor.map { Color(it) }

    @Composable
    override fun primaryColor(context: Context): State<Color> =
        primaryColorFlow.collectAsState(initial = Color(ThemeSettingsDelegate.primaryColor()))

    @Composable
    override fun accentColor(context: Context): State<Color> =
        accentColorFlow.collectAsState(initial = Color(ThemeSettingsDelegate.accentColor()))
}

object PreviewColorPalette : ColorPalette {

    @Composable
    override fun primaryColor(context: Context): State<Color> =
        remember { mutableStateOf(Color(0xFF2962FF)) }


    @Composable
    override fun accentColor(context: Context): State<Color> =
        remember { mutableStateOf(Color(0xFFFF7043)) }
}