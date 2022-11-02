package player.phonograph.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build
import player.phonograph.ui.activities.StarterActivity
import android.os.Bundle

/**
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
abstract class BaseShortcutType(var context: Context) {

    abstract val shortcutInfo: ShortcutInfo

    /**
     * Creates an Intent that will launch Main Activtiy and immediately play songs in either shuffle or normal mode
     *
     * @param shortcutType Describes the type of shortcut to create (ShuffleAll, TopTracks, custom playlist, etc.)
     * @return
     */
    fun getPlaySongsIntent(shortcutType: Int): Intent =
        Intent(context, StarterActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtras(
                Bundle().apply {
                    putInt(StarterActivity.SHORTCUT_TYPE, shortcutType)
                    putBoolean(StarterActivity.EXTRA_SHORTCUT_MODE, true)
                }
            )
        }

    companion object {
        const val ID_PREFIX = "player.phonograph.appshortcuts.id."
        const val id: String = "${ID_PREFIX}invalid"
    }
}
