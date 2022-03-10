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
fun PhonographTheme(darkTheme: Boolean = isSystemInDarkTheme(), previewMode: Boolean = false, content: @Composable () -> Unit) {
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

fun colorsLight(previewMode: Boolean): Colors {
    val cfg = getColorConfig(previewMode)
    return lightColors(
        primary = cfg[0],
        primaryVariant = cfg[1],
        secondary = cfg[2]
    )
}

fun colorsNight(previewMode: Boolean): Colors {
    val cfg = getColorConfig(previewMode)
    return darkColors(
        primary = cfg[0],
        primaryVariant = cfg[1],
        secondary = cfg[2]
    )
}

// todo
fun getColorConfig(previewMode: Boolean = false): Array<Color> {
    return if (previewMode) {
        arrayOf(
            Color(util.mdcolor.R.color.md_blue_A400),
            Color(util.mdcolor.R.color.md_blue_900),
            Color(util.mdcolor.R.color.md_yellow_900),
        )
    } else {
        arrayOf(
            Color(ThemeColor.primaryColor(App.instance)),
            Color(ThemeColor.statusBarColor(App.instance)),
            Color(ThemeColor.accentColor(App.instance)),
        )
    }
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
