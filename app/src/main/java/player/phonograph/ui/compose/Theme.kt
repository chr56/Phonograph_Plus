/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.App
import player.phonograph.mechanism.setting.StyleConfig
import player.phonograph.mechanism.setting.StyleConfig.THEME_AUTO
import player.phonograph.mechanism.setting.StyleConfig.THEME_BLACK
import player.phonograph.mechanism.setting.StyleConfig.THEME_DARK
import player.phonograph.mechanism.setting.StyleConfig.THEME_LIGHT
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.systemDarkmode
import util.theme.color.shiftColor
import util.theme.materials.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

@Composable
fun PhonographTheme(content: @Composable () -> Unit) {

    val previewMode = LocalInspectionMode.current
    val colors = when (StyleConfig.generalTheme(LocalContext.current)) {
        THEME_AUTO  -> colorAuto(previewMode, LocalContext.current)
        THEME_DARK  -> colorsDark(previewMode)
        THEME_BLACK -> colorsBlack(previewMode)
        THEME_LIGHT -> colorsLight(previewMode)
        else        -> colorAuto(previewMode, LocalContext.current)
    }
    PhonographTheme(colors) {
        content()
    }
}


@Composable
fun PhonographTheme(primary: Color?, content: @Composable () -> Unit) {
    val previewMode = LocalInspectionMode.current
    val colors = when (StyleConfig.generalTheme(LocalContext.current)) {
        THEME_AUTO  -> colorAuto(previewMode, LocalContext.current)
        THEME_DARK  -> colorsDark(previewMode)
        THEME_BLACK -> colorsBlack(previewMode)
        THEME_LIGHT -> colorsLight(previewMode)
        else        -> colorAuto(previewMode, LocalContext.current)
    }.let { colors ->
        if (primary != null) {
            colors.copy(
                primary = primary,
                primaryVariant = primary.darker().darker(),
                onPrimary = textColorOn(LocalContext.current, primary),
            )
        } else {
            colors
        }
    }

    MaterialTheme(colors = colors) {
        content()
    }
}

@Composable
private fun PhonographTheme(colors: Colors, content: @Composable () -> Unit) {
    // val previewMode = LocalInspectionMode.current
    // val view = LocalView.current
    // if (!previewMode) {
    //     SideEffect {
    //         val window = (view.context as Activity).window
    //         window.statusBarColor = colors.primary.toArgb()
    //         window.navigationBarColor = colors.primary.toArgb()
    //         WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    //         WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    //     }
    // }
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private fun colorAuto(previewMode: Boolean, context: Context) =
    if (systemDarkmode(context.resources)) {
        colorsDark(previewMode)
    } else {
        colorsLight(previewMode)
    }

fun colorsLight(previewMode: Boolean): Colors = with(colorConfig(previewMode)) {
    Colors(
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

fun colorsDark(previewMode: Boolean): Colors = with(colorConfig(previewMode)) {
    Colors(
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

fun colorsBlack(previewMode: Boolean): Colors = with(colorConfig(previewMode)) {
    Colors(
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

class ColorConfig(
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val accentDark: Color = accent,
    val onPrimary: Color = textColorOn(App.instance, primary),
    val onAccent: Color = textColorOn(App.instance, accent),
)

fun colorConfig(previewMode: Boolean, context: Context = App.instance): ColorConfig =
    if (previewMode) {
        ColorConfig(
            Color(R.color.md_blue_A400),
            Color(R.color.md_blue_900),
            Color(R.color.md_yellow_900),
            Color(R.color.md_orange_900),
        )
    } else {
        val primary = ThemeSetting.primaryColor(context)
        val accent = ThemeSetting.accentColor(context)
        ColorConfig(
            Color(primary),
            Color(shiftColor(primary, 0.8f)),
            Color(accent),
            Color(shiftColor(accent, 0.9f)),
        )
    }

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )

)

val Shapes = Shapes(
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp)
)
