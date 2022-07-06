package player.phonograph.appshortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import player.phonograph.appshortcuts.DynamicShortcutManager.Companion.reportShortcutUsed
import player.phonograph.appshortcuts.shortcuttype.LastAddedShortcutType
import player.phonograph.appshortcuts.shortcuttype.ShuffleAllShortcutType
import player.phonograph.appshortcuts.shortcuttype.TopTracksShortcutType
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.ShuffleAllPlaylist
import player.phonograph.service.MusicService
import player.phonograph.service.queue.SHUFFLE_MODE_NONE
import player.phonograph.service.queue.SHUFFLE_MODE_SHUFFLE

/**
 * @author Adrian Campos
 */
class AppShortcutLauncherActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var shortcutType = SHORTCUT_TYPE_NONE

        // Set shortcutType from the intent extras
        shortcutType = intent.extras?.getInt(KEY_SHORTCUT_TYPE) ?: SHORTCUT_TYPE_NONE

        when (shortcutType) {
            SHORTCUT_TYPE_SHUFFLE_ALL -> {
                startServiceWithPlaylist(
                    SHUFFLE_MODE_SHUFFLE,
                    ShuffleAllPlaylist(applicationContext)
                )
                reportShortcutUsed(this, ShuffleAllShortcutType.id)
            }
            SHORTCUT_TYPE_TOP_TRACKS -> {
                startServiceWithPlaylist(
                    SHUFFLE_MODE_NONE,
                    MyTopTracksPlaylist(applicationContext)
                )
                reportShortcutUsed(this, TopTracksShortcutType.id)
            }
            SHORTCUT_TYPE_LAST_ADDED -> {
                startServiceWithPlaylist(
                    SHUFFLE_MODE_NONE,
                    LastAddedPlaylist(applicationContext)
                )
                reportShortcutUsed(this, LastAddedShortcutType.id)
            }
        }
        finish()
    }

    private fun startServiceWithPlaylist(shuffleMode: Int, playlist: Playlist) {
        startService(
            Intent(this, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_PLAYLIST
                putExtras(
                    Bundle().apply {
                        putParcelable(MusicService.INTENT_EXTRA_PLAYLIST, playlist)
                        putInt(MusicService.INTENT_EXTRA_SHUFFLE_MODE, shuffleMode)
                    }
                )
            }
        )
    }

    companion object {
        const val KEY_SHORTCUT_TYPE = "player.phonograph.appshortcuts.ShortcutType"

        const val SHORTCUT_TYPE_SHUFFLE_ALL = 0
        const val SHORTCUT_TYPE_TOP_TRACKS = 1
        const val SHORTCUT_TYPE_LAST_ADDED = 2
        const val SHORTCUT_TYPE_NONE = 3
    }
}
