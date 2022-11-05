package player.phonograph.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.os.Build
import player.phonograph.R
import player.phonograph.appshortcuts.AppShortcutIconGenerator
import player.phonograph.ui.activities.StarterActivity

/**
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class LastAddedShortcutType(context: Context) : BaseShortcutType(context) {

    override val shortcutInfo: ShortcutInfo
        get() = ShortcutInfo.Builder(context, id)
            .setShortLabel(context.getString(R.string.app_shortcut_last_added_short))
            .setLongLabel(context.getString(R.string.last_added))
            .setIcon(AppShortcutIconGenerator.generateThemedIcon(context, R.drawable.ic_app_shortcut_last_added))
            .setIntent(getPlaySongsIntent(StarterActivity.SHORTCUT_TYPE_LAST_ADDED))
            .build()

    companion object {
        const val id: String = "${ID_PREFIX}last_added"
    }
}
