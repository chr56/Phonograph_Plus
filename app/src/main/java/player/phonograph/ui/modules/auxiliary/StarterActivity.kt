/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.auxiliary

import legacy.phonograph.MediaStoreCompat
import org.koin.android.ext.android.get
import player.phonograph.R
import player.phonograph.mechanism.PhonographShortcutManager
import player.phonograph.mechanism.SongUriParsers
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.PlayRequest
import player.phonograph.model.PlayRequest.SongRequest
import player.phonograph.model.PlayRequest.SongsRequest
import player.phonograph.model.Song
import player.phonograph.model.playlist.DynamicPlaylists
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ACTION_PLAY
import player.phonograph.model.service.ShuffleMode
import player.phonograph.model.ui.AppShortcutType
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.service.MusicService
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.executePlayRequest
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.dialogs.OpenWithDialog
import player.phonograph.ui.modules.main.MainActivity
import player.phonograph.util.debug
import androidx.appcompat.app.AppCompatActivity
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class StarterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherIntent = intent
        val extras = launcherIntent.extras

        if (extras != null && extras.getBoolean(EXTRA_SHORTCUT_MODE, false)) {
            debug {
                Log.d("Starter", "ShortCut Mode")
            }
            val shortcutType = launcherIntent.extras?.getString(SHORTCUT_TYPE, null)
            if (shortcutType != null) {
                processShortcut(shortcutType)
            }
            finish()
        } else {
            debug {
                Log.d("Starter", "Normal Mode")
            }
            if (SDK_INT >= N_MR1) {
                PhonographShortcutManager.updateDynamicShortcuts(this)
            }
            processFrontGroundMode(launcherIntent)
        }
    }

    private fun processFrontGroundMode(intent: Intent) {
        val playRequest = runBlocking { lookupSongsFromIntent(intent) }
        if (playRequest is PlayRequest.EmptyRequest) {
            Toast.makeText(this, R.string.msg_empty, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            val showPrompt: Boolean = Setting(this.applicationContext)[Keys.externalPlayRequestShowPrompt].data
            val silence: Boolean = Setting(this.applicationContext)[Keys.externalPlayRequestSilence].data
            if (showPrompt) {
                val openWithDialog = OpenWithDialog.create(playRequest, gotoMainActivity = !silence)
                openWithDialog?.show(supportFragmentManager, null)
            } else {
                executePlayRequestByDefault(playRequest, silence)
            }
        }
    }

    private fun executePlayRequestByDefault(playRequest: PlayRequest, silence: Boolean) {
        val queueManager: QueueManager = get()
        val key = when (playRequest) {
            is SongRequest -> Keys.externalPlayRequestSingleMode
            is SongsRequest -> Keys.externalPlayRequestMultipleMode
            else -> return
        }
        val mode = Setting(this.applicationContext)[key].data
        executePlayRequest(queueManager, playRequest, mode)
        if (!silence) gotoMainActivity() else finish()
    }


    private suspend fun lookupSongsFromIntent(intent: Intent): PlayRequest {
        var playRequest: PlayRequest?
        // uri first
        playRequest = handleUriPlayRequest(intent)

        // then search
        if (playRequest == null) {
            playRequest = handleSearchRequest(intent)
        }
        // then
        if (playRequest == null) {
            playRequest = handleExtra(intent)
        }

        return playRequest ?: PlayRequest.EmptyRequest
    }

    private suspend fun handleUriPlayRequest(intent: Intent): PlayRequest? {
        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            val songs = parseUri(uri)
            return PlayRequest.from(songs)
        }
        return null
    }

    private suspend fun parseUri(uri: Uri): List<Song> {
        for (parser in SongUriParsers) {
            val taken = parser.check(uri.scheme, uri.authority)
            if (taken) {
                val result = parser.parse(this, uri)
                if (result.isNotEmpty()) return result.toList()
            }
        }
        return emptyList()
    }

    private suspend fun handleSearchRequest(intent: Intent): PlayRequest? {
        intent.action?.let { action ->
            val extras = intent.extras
            if (action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH && extras != null) {
                val query = extras.getString(SearchManager.QUERY)
                val title = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)
                val album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                val artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                val songs = MediaStoreSongs.search(this, query, title, album, artist)
                return PlayRequest.from(songs)
            }
        }
        return null
    }

    private suspend fun handleExtra(intent: Intent): PlayRequest? {
        when (intent.type) {
            MediaStoreCompat.Audio.Playlists.CONTENT_TYPE -> {
                val id = parseIdFromIntent(intent, "playlistId", "playlist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = MediaStorePlaylists.songs(this, id).map { it.song }
                    if (songs.isNotEmpty()) return SongsRequest(songs, position)
                }
            }

            MediaStore.Audio.Albums.CONTENT_TYPE          -> {
                val id = parseIdFromIntent(intent, "albumId", "album")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = Songs.album(this, id)
                    if (songs.isNotEmpty()) return SongsRequest(songs, position)
                }
            }

            MediaStore.Audio.Artists.CONTENT_TYPE         -> {
                val id = parseIdFromIntent(intent, "artistId", "artist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = Songs.artist(this, id)
                    if (songs.isNotEmpty()) return SongsRequest(songs, position)
                }
            }
        }
        return null
    }

    private fun processShortcut(shortcutType: String) {
        val type = AppShortcutType.from(shortcutType)
        var shuffleMode = ShuffleMode.NONE
        val playlist: Playlist = when (type) {
            AppShortcutType.ShuffleAllShortcut ->
                DynamicPlaylists.random(resources).also { shuffleMode = ShuffleMode.SHUFFLE }

            AppShortcutType.TopTracksShortcut  ->
                DynamicPlaylists.myTopTrack(resources)

            AppShortcutType.LastAddedShortcut  ->
                DynamicPlaylists.lastAdded(resources)

            else                               -> return
        }

        val songs =
            runBlocking(Dispatchers.IO) { PlaylistSongsActions.reader(playlist).allSongs(this@StarterActivity) }
        play(songs, shuffleMode)

        if (SDK_INT >= N_MR1) PhonographShortcutManager.reportShortcutUsed(this, type.id)
    }

    private fun play(songs: List<Song>, shuffleMode: ShuffleMode) {
        if (songs.isNotEmpty()) {
            val queueManager: QueueManager = get()
            queueManager.swapQueue(
                songs,
                if (shuffleMode == ShuffleMode.SHUFFLE) Random.nextInt(songs.size) else 0,
                false
            )
            queueManager.modifyShuffleMode(shuffleMode, false)
            queueManager.modifyPosition(0, false)
            startService(
                Intent(this, MusicService::class.java).apply { action = ACTION_PLAY }
            )
        } else {
            Toast.makeText(this, R.string.msg_empty_playlist, Toast.LENGTH_SHORT).show()
        }
    }

    private fun gotoMainActivity() {
        startActivity(
            MainActivity.launchingIntent(this, Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val TAG = "Starter"

        fun parseIdFromIntent(intent: Intent, longKey: String, stringKey: String): Long {
            var id = intent.getLongExtra(longKey, -1)
            if (id < 0) {
                val idString = intent.getStringExtra(stringKey)
                if (idString != null) {
                    try {
                        id = idString.toLong()
                    } catch (e: NumberFormatException) {
                        Log.w("Starter", e)
                    }
                }
            }
            return id
        }

        const val EXTRA_SHORTCUT_MODE = "player.phonograph.SHORTCUT_MODE"
        const val SHORTCUT_TYPE = "player.phonograph.appshortcuts.SHORTCUT_TYPE"
    }

}