package player.phonograph.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import player.phonograph.appshortcuts.shortcuttype.LastAddedShortcutType
import player.phonograph.appshortcuts.shortcuttype.ShuffleAllShortcutType
import player.phonograph.appshortcuts.shortcuttype.TopTracksShortcutType
import java.util.*

/**
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class DynamicShortcutManager(private val context: Context) {

    private val shortcutManager: ShortcutManager = context.getSystemService(ShortcutManager::class.java)

    fun initDynamicShortcuts() {
        if (shortcutManager.dynamicShortcuts.size == 0) {
            shortcutManager.dynamicShortcuts = defaultShortcuts
        }
    }

    fun updateDynamicShortcuts() {
        shortcutManager.updateShortcuts(defaultShortcuts)
    }

    val defaultShortcuts: List<ShortcutInfo>
        get() = listOf(
            ShuffleAllShortcutType(context).shortcutInfo,
            TopTracksShortcutType(context).shortcutInfo,
            LastAddedShortcutType(context).shortcutInfo
        )

    companion object {
        fun createShortcut(
            context: Context?,
            id: String?,
            shortLabel: String?,
            longLabel: String?,
            icon: Icon?,
            intent: Intent?
        ): ShortcutInfo {
            return ShortcutInfo.Builder(context, id)
                .setShortLabel(shortLabel!!)
                .setLongLabel(longLabel!!)
                .setIcon(icon)
                .setIntent(intent!!)
                .build()
        }

        fun reportShortcutUsed(context: Context, shortcutId: String?) {
            context.getSystemService(ShortcutManager::class.java).reportShortcutUsed(shortcutId)
        }
    }
}
