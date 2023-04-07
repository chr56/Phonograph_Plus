package player.phonograph.appshortcuts

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.TypedValue
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.IconCompat
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.settings.Setting
import player.phonograph.util.ImageUtil
import player.phonograph.util.theme.createTintedDrawable

/**
 * @author Adrian Campos
 */
@RequiresApi(Build.VERSION_CODES.N_MR1)
object AppShortcutIconGenerator {

    fun generateThemedIcon(context: Context, iconId: Int): Icon =
        if (Setting.instance(context).coloredAppShortcuts) {
            generateUserThemedIcon(context, iconId).toIcon(context)
        } else {
            generateDefaultThemedIcon(context, iconId).toIcon(context)
        }

    private fun generateDefaultThemedIcon(context: Context, iconId: Int): IconCompat {
        // Return an Icon of iconId with default colors
        return generateThemedIcon(
            context, iconId,
            context.getColor(R.color.app_shortcut_default_foreground),
            context.getColor(R.color.app_shortcut_default_background)
        )
    }

    private fun generateUserThemedIcon(context: Context, iconId: Int): IconCompat {
        // Get background color from context's theme
        val typedColorBackground = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, typedColorBackground, true)

        // Return an Icon of iconId with those colors
        return generateThemedIcon(
            context, iconId,
            ThemeColor.primaryColor(context),
            typedColorBackground.data
        )
    }

    private fun generateThemedIcon(context: Context, iconId: Int, foregroundColor: Int, backgroundColor: Int): IconCompat {
        // Get and tint foreground and background drawables
        val vectorDrawable = context.createTintedDrawable(iconId, foregroundColor)
        val backgroundDrawable = context.createTintedDrawable(R.drawable.ic_app_shortcut_background, backgroundColor)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val adaptiveIconDrawable = AdaptiveIconDrawable(backgroundDrawable, vectorDrawable)
            IconCompat.createWithAdaptiveBitmap(ImageUtil.createBitmap(adaptiveIconDrawable))
        } else {
            // Squash the two drawables together
            val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, vectorDrawable))

            // Return as an Icon
            IconCompat.createWithBitmap(ImageUtil.createBitmap(layerDrawable))
        }
    }
}
