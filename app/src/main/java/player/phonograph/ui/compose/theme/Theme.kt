/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mt.pref.ThemeColor
import mt.pref.accentColor
import mt.util.color.primaryTextColor
import mt.util.color.shiftColor
import player.phonograph.App
import player.phonograph.ui.compose.textColorOn
import androidx.compose.ui.platform.LocalInspectionMode
import android.content.Context

@Composable
fun PhonographTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val previewMode = LocalInspectionMode.current
    val colors =
        if (darkTheme) {
            colorsNight(previewMode)
        } else {
            colorsLight(previewMode)
        }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
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

fun colorsNight(previewMode: Boolean): Colors = with(colorConfig(previewMode)) {
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
            Color(mt.color.R.color.md_blue_A400),
            Color(mt.color.R.color.md_blue_900),
            Color(mt.color.R.color.md_yellow_900),
            Color(mt.color.R.color.md_orange_900),
        )
    } else {
        val primary = ThemeColor.primaryColor(context)
        val accent = ThemeColor.accentColor(context)
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
