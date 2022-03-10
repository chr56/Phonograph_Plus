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
import player.phonograph.App
import util.mdcolor.pref.ThemeColor

@Composable
fun Phonograph_PlusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors =
        if (darkTheme) {
            colorsNight()
        } else {
            colorsLight()
        }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

fun colorsLight(): Colors {
    val accentColor = Color(ThemeColor.accentColor(App.instance))
    val primaryColor = Color(ThemeColor.primaryColor(App.instance))
    val primaryDarkenColor = Color(ThemeColor.statusBarColor(App.instance))

    return lightColors(
        primary = primaryColor,
        primaryVariant = primaryDarkenColor,
        secondary = accentColor
    )
}

fun colorsNight(): Colors {
    val accentColor = Color(ThemeColor.accentColor(App.instance))
    val primaryColor = Color(ThemeColor.primaryColor(App.instance))
    val primaryDarkenColor = Color(ThemeColor.statusBarColor(App.instance))

    return darkColors(
        primary = primaryColor,
        primaryVariant = primaryDarkenColor,
        secondary = accentColor
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
