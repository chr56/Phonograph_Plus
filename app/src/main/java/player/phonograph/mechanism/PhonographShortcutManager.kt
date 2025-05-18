/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism

import player.phonograph.R
import player.phonograph.model.ui.AppShortcutType
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.ui.modules.auxiliary.StarterActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import util.theme.color.isColorLight
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.IconCompat
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle

/**
 * @author Adrian Campos
 */
@RequiresApi(VERSION_CODES.N_MR1)
object PhonographShortcutManager {

    private fun shortcutManager(context: Context): ShortcutManager =
        context.getSystemService(ShortcutManager::class.java)

    fun initDynamicShortcuts(context: Context) {
        val shortcutManager = shortcutManager(context)
        if (shortcutManager.dynamicShortcuts.isEmpty()) {
            shortcutManager.dynamicShortcuts = defaultShortcuts(context)
        }
    }

    fun updateDynamicShortcuts(context: Context) {
        val shortcutManager = shortcutManager(context)
        shortcutManager.updateShortcuts(defaultShortcuts(context))
    }

    fun reportShortcutUsed(context: Context, shortcutId: String) {
        val shortcutManager = shortcutManager(context)
        shortcutManager.reportShortcutUsed(shortcutId)
    }


    fun defaultShortcutTypes(): List<AppShortcutType> = listOf(
        AppShortcutType.LastAddedShortcut,
        AppShortcutType.ShuffleAllShortcut,
        AppShortcutType.TopTracksShortcut,
    )

    fun defaultShortcuts(context: Context): List<ShortcutInfo> {
        val colored = Setting(context)[Keys.coloredAppShortcuts].data
        return defaultShortcutTypes().map { type -> shortcutInfo(context, type, colored) }
    }

    fun shortcutInfo(
        context: Context, type: AppShortcutType, colored: Boolean,
    ): ShortcutInfo = when (type) {
        AppShortcutType.LastAddedShortcut -> shortcutInfo(
            context,
            type.id,
            context.getString(R.string.app_shortcut_last_added_short),
            context.getString(R.string.last_added),
            themedIcon(context, R.drawable.ic_app_shortcut_last_added, colored),
            playIntent(context, type.id)
        )

        AppShortcutType.ShuffleAllShortcut -> shortcutInfo(
            context,
            type.id,
            context.getString(R.string.app_shortcut_shuffle_all_short),
            context.getString(R.string.action_shuffle_all),
            themedIcon(context, R.drawable.ic_app_shortcut_shuffle_all, colored),
            playIntent(context, type.id),
        )

        AppShortcutType.TopTracksShortcut -> shortcutInfo(
            context,
            type.id,
            context.getString(R.string.app_shortcut_top_tracks_short),
            context.getString(R.string.my_top_tracks),
            themedIcon(context, R.drawable.ic_app_shortcut_top_tracks, colored),
            playIntent(context, type.id),
        )
    }

    private fun shortcutInfo(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        icon: Icon,
        intent: Intent,
    ): ShortcutInfo {
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(icon)
            .setIntent(intent)
            .build()
    }

    private fun playIntent(context: Context, shortcutType: String): Intent =
        Intent(context, StarterActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtras(
                Bundle().apply {
                    putString(StarterActivity.SHORTCUT_TYPE, shortcutType)
                    putBoolean(StarterActivity.EXTRA_SHORTCUT_MODE, true)
                }
            )
        }

    private fun themedIcon(context: Context, iconId: Int, colored: Boolean): Icon =
        if (colored) {
            val primaryColor = ThemeSetting.primaryColor(context)
            val background = if (isColorLight(primaryColor)) {
                context.getColor(R.color.divider_lightdark)
            } else {
                context.getColor(R.color.background_medium_lightdark)
            }
            icon(
                context, iconId,
                primaryColor,
                background
            ).toIcon(context)
        } else {
            icon(
                context, iconId,
                context.getColor(R.color.app_shortcut_default_foreground),
                context.getColor(R.color.app_shortcut_default_background)
            ).toIcon(context)
        }

    private fun icon(
        context: Context,
        iconId: Int,
        foregroundColor: Int,
        backgroundColor: Int,
    ): IconCompat {
        // Get and tint foreground and background drawables
        val foregroundDrawable = context.getTintedDrawable(iconId, foregroundColor)
        val backgroundDrawable = context.getTintedDrawable(R.drawable.ic_app_shortcut_background, backgroundColor)
        return if (SDK_INT >= VERSION_CODES.O) {
            val adaptiveIconDrawable = AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable)
            IconCompat.createWithAdaptiveBitmap(BitmapUtil.createBitmap(adaptiveIconDrawable))
        } else {
            // Squash the two drawables together
            val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, foregroundDrawable))
            // Return as an Icon
            IconCompat.createWithBitmap(BitmapUtil.createBitmap(layerDrawable))
        }
    }

}
