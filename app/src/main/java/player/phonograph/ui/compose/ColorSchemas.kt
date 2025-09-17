/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

//region Default

@Composable
fun colorSchemaLight(palette: ColorPalette): Colors {
    val context = LocalContext.current
    val accent by palette.accentColor(context)
    val primary by palette.primaryColor(context)
    val primaryDark = remember(primary) { primary.darker() }
    val accentDark = remember(accent) { accent.darker() }
    val onPrimary = remember(primary) { textColorOn(context, primary) }
    val onAccent = remember(accent) { textColorOn(context, accent) }
    return Colors(
        primary = primary,
        primaryVariant = primaryDark,
        secondary = accent,
        secondaryVariant = accentDark,
        background = Color.White,
        surface = Color(0xFFF5F5F5),
        error = Color(0xFFB00020),
        onPrimary = onPrimary,
        onSecondary = onAccent,
        onBackground = Color.Black,
        onSurface = Color.Black,
        onError = Color.White,
        isLight = true
    )
}

@Composable
fun colorSchemaDark(palette: ColorPalette): Colors {
    val context = LocalContext.current
    val accent by palette.accentColor(context)
    val primary by palette.primaryColor(context)
    val primaryDark = remember(primary) { primary.darker() }
    val accentDark = remember(accent) { accent.darker() }
    val onPrimary = remember(primary) { textColorOn(context, primary) }
    val onAccent = remember(accent) { textColorOn(context, accent) }
    return Colors(
        primary = primary,
        primaryVariant = primaryDark,
        secondary = accent,
        secondaryVariant = accentDark,
        background = Color(0xFF1B1B1B),
        surface = Color(0xFF1B1B1B),
        error = Color(0xFF85002D),
        onPrimary = onPrimary,
        onSecondary = onAccent,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Black,
        isLight = true
    )
}

@Composable
fun colorSchemaBlack(palette: ColorPalette): Colors {
    val context = LocalContext.current
    val accent by palette.accentColor(context)
    val primary by palette.primaryColor(context)
    val primaryDark = remember(primary) { primary.darker() }
    val accentDark = remember(accent) { accent.darker() }
    val onPrimary = remember(primary) { textColorOn(context, primary) }
    val onAccent = remember(accent) { textColorOn(context, accent) }
    return Colors(
        primary = primary,
        primaryVariant = primaryDark,
        secondary = accent,
        secondaryVariant = accentDark,
        background = Color(0xFF000000),
        surface = Color(0xFF0C0C0C),
        error = Color(0xFF85002D),
        onPrimary = onPrimary,
        onSecondary = onAccent,
        onBackground = Color(0xFFC4C4C4),
        onSurface = Color(0xFFD3D3D3),
        onError = Color.Black,
        isLight = true
    )
}

//endregion