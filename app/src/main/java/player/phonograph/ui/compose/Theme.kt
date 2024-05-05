/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.THEME_AUTO
import player.phonograph.settings.THEME_BLACK
import player.phonograph.settings.THEME_DARK
import player.phonograph.settings.THEME_LIGHT
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.accentColorFlow
import player.phonograph.util.theme.primaryColorFlow
import player.phonograph.util.theme.systemDarkmode
import util.theme.materials.MaterialColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun PhonographTheme(content: @Composable () -> Unit) {
    val colors = phonographColors()
    PhonographTheme(colors) {
        content()
    }
}


@Composable
fun PhonographTheme(primary: Color?, content: @Composable () -> Unit) {
    val colors = phonographColors().let { colors ->
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
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
private fun phonographColors(): Colors {
    val theme by Setting(LocalContext.current)[Keys.theme].flow.collectAsState(THEME_AUTO)
    val previewMode = LocalInspectionMode.current
    val colorPalette: ColorPalette = if (previewMode) {
        PreviewColorPalette
    } else {
        ThemeColorPalette(LocalContext.current)
    }
    return when (theme) {
        THEME_AUTO  -> colorAuto(colorPalette, LocalContext.current)
        THEME_DARK  -> colorsDark(colorPalette)
        THEME_BLACK -> colorsBlack(colorPalette)
        THEME_LIGHT -> colorsLight(colorPalette)
        else        -> colorAuto(colorPalette, LocalContext.current)
    }
}


@Composable
private fun colorAuto(palette: ColorPalette, context: Context) =
    if (systemDarkmode(context.resources)) {
        colorsDark(palette)
    } else {
        colorsLight(palette)
    }


@Composable
fun colorsLight(palette: ColorPalette): Colors {
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
fun colorsDark(palette: ColorPalette): Colors {
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
fun colorsBlack(palette: ColorPalette): Colors {
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
        remember { mutableStateOf(Color(MaterialColor.Blue._A400.asColor)) }


    @Composable
    override fun accentColor(context: Context): State<Color> =
        remember { mutableStateOf(Color(MaterialColor.Yellow._900.asColor)) }
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
