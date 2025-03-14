/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTBLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_BLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_DARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_LIGHT
import player.phonograph.util.theme.ThemeSettingsDelegate
import player.phonograph.util.theme.setupSystemBars
import player.phonograph.util.theme.updateSystemBarsColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity

@Composable
fun PhonographTheme(content: @Composable () -> Unit) {
    val colors = phonographColors()
    PhonographTheme(colors = colors) {
        content()
    }
}

@Composable
fun PhonographTheme(primary: Color?, content: @Composable () -> Unit) {
    val colors = tweakColors(phonographColors(), primary)
    MaterialTheme(colors = colors) {
        content()
    }
}

@Composable
private fun PhonographTheme(colors: Colors, content: @Composable () -> Unit) {
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
    AwareSystemUIColor(colors.primaryVariant)
}

@Composable
fun ExperimentalContentThemeOverride(content: @Composable (() -> Unit)) {
    MaterialTheme(
        colors = experimentalContentColors(),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/////////////

@Composable
private fun phonographColors(): Colors {
    val resources = LocalResources.current
    val theme by ThemeSettingsDelegate.underlyingTheme(resources).collectAsState(THEME_AUTO_LIGHTBLACK)
    val colorPalette: ColorPalette = defaultColorPalette()
    return when (theme) {
        THEME_DARK  -> colorSchemaDark(colorPalette)
        THEME_BLACK -> colorSchemaBlack(colorPalette)
        THEME_LIGHT -> colorSchemaLight(colorPalette)
        else        -> colorSchemaDark(colorPalette)
    }
}

@Composable
private fun experimentalContentColors(): Colors {
    val resources = LocalResources.current
    val theme by ThemeSettingsDelegate.underlyingTheme(resources).collectAsState(THEME_AUTO_LIGHTBLACK)
    val colorPalette: ColorPalette = defaultColorPalette()
    return when (theme) {
        THEME_DARK  -> colorSchemaDark(colorPalette).copy(
            surface = Color(0xFF5C1F0C),
            onSurface = Color(0xFFFFA0A0),
        )

        THEME_BLACK -> colorSchemaBlack(colorPalette).copy(
            surface = Color(0xFF4D0C00),
            onSurface = Color(0xFFEE5555),
        )

        THEME_LIGHT -> colorSchemaLight(colorPalette).copy(
            surface = Color(0xFFF6DCC8),
            onSurface = Color(0xFF4B0000),
        )

        else        -> colorSchemaDark(colorPalette).copy(
            surface = Color(0xFF5C1F0C),
            onSurface = Color(0xFFFFA0A0),
        )
    }
}

@Composable
private fun tweakColors(
    colors: Colors,
    primary: Color?,
): Colors = if (primary != null) {
    colors.copy(
        primary = primary,
        primaryVariant = primary.darker().darker(),
        onPrimary = textColorOn(LocalContext.current, primary),
    )
} else {
    colors
}


@Composable
private fun AwareSystemUIColor(color: Color) {
    val context = LocalContext.current
    LaunchedEffect(color) {
        if (context is Activity) {
            context.setupSystemBars()
            context.updateSystemBarsColor(color.toArgb(), 64 shl 24)
        }
    }
}

///////////////////////////////

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
