/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.ui.theme.secondaryTextColorOn
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.theme.textColorPrimary
import player.phonograph.ui.theme.textColorSecondary
import player.phonograph.ui.theme.themeCardBackgroundColor
import player.phonograph.ui.theme.themeDividerColor
import player.phonograph.ui.theme.themeFooterColor
import player.phonograph.ui.theme.themeIconColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

// fixme: replace view system functions from `.ui.theme.*`. It is better to create a new one for Compose system

object ItemDefaults {

    //region Styles
    fun listLayoutStyle(
        height: Dp,
        contentStartPadding: Dp = 16.dp,
        contentEndPadding: Dp = 16.dp,
        menuWidth: Dp = 48.dp,
        titleLines: Int = 1,
        secondaryLines: Int = 1,
    ) = ListLayoutStyle(
        height = height,
        contentStartPadding = contentStartPadding,
        contentEndPadding = contentEndPadding,
        menuWidth = menuWidth,
        titleLines = titleLines,
        secondaryLines = secondaryLines,
    )
    //endregion

    //region Text
    val titleStyle = textStyle(16)
    val secondaryStyle = textStyle(14)
    val gridTitleStyle = textStyle(16, FontWeight.Medium)
    val cardTitleStyle = textStyle(14)
    val captionStyle = textStyle(12)

    private fun textStyle(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = 0.sp,
    )
    //endregion

    //region Colors
    @Composable
    fun listColors(): ItemColors = colors(LocalContext.current, Color.Transparent, strong = false)

    @Composable
    fun gridColors(paletteColor: Color? = null): ItemColors {
        val context = LocalContext.current
        val background = paletteColor ?: Color(themeFooterColor(context))
        return colors(context, background, strong = true).copy(
            title = Color(textColorOn(context, background.toArgb())),
            secondaryText = Color(secondaryTextColorOn(context, background.toArgb())),
        )
    }

    @Composable
    fun cardColors(paletteColor: Color? = null): ItemColors {
        val context = LocalContext.current
        val background = paletteColor ?: Color(themeCardBackgroundColor(context))
        val colors = colors(context, background, strong = true)
        return if (paletteColor == null) colors else colors.copy(
            title = Color(textColorOn(context, background.toArgb())),
            secondaryText = Color(secondaryTextColorOn(context, background.toArgb())),
        )
    }

    private fun colors(context: Context, background: Color, strong: Boolean): ItemColors = ItemColors(
        title = Color(textColorPrimary(context)),
        secondaryText = Color(textColorSecondary(context)),
        icon = Color(themeIconColor(context)),
        divider = Color(themeDividerColor(context)),
        selection = Color(themeIconColor(context)).copy(alpha = if (strong) 0.2f else 0.12f),
        background = background,
    )
    //endregion
}

