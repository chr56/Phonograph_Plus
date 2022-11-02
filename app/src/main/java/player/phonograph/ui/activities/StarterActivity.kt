/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.appshortcuts.DynamicShortcutManager.Companion.reportShortcutUsed
import player.phonograph.appshortcuts.shortcuttype.LastAddedShortcutType
import player.phonograph.appshortcuts.shortcuttype.ShuffleAllShortcutType
import player.phonograph.appshortcuts.shortcuttype.TopTracksShortcutType
import player.phonograph.helper.SearchQueryHelper
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.ShuffleAllPlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicService
import player.phonograph.service.queue.ShuffleMode
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.DocumentsContractCompat.getDocumentId
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlin.random.Random
import java.io.File

class StarterActivity : AppCompatActivity() {

    private fun debugLog(msg: String) {
        if (BuildConfig.DEBUG) Log.d("Starter", msg)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherIntent = intent
        val extras = launcherIntent.extras

        if (extras != null && extras.getBoolean(EXTRA_SHORTCUT_MODE, false)) {
            debugLog("ShortCut Mode")
            processShortCut(launcherIntent.extras?.getInt(SHORTCUT_TYPE) ?: SHORTCUT_TYPE_NONE)
            finish()
        } else {
            debugLog("Normal Mode")
            DynamicShortcutManager(this).updateDynamicShortcuts()
            processFrontGroundMode(launcherIntent)
            finish()
        }
    }

    private fun processFrontGroundMode(intent: Intent) {
        val playRequest = lookupSongsFromIntent(intent)
        if (playRequest != null)
            MusicPlayerRemote.playQueue(playRequest.songs, playRequest.position, true, null)
        else
            Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show()
        startActivity(
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }


    private fun lookupSongsFromIntent(intent: Intent): PlayRequest? {
        var playRequest: PlayRequest? = null
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

        return playRequest
    }

    private fun handleUriPlayRequest(intent: Intent): PlayRequest? {
        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            val songs = parseUri(uri)
            if (songs != null) return PlayRequest(songs, 0)
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun parseUri(uri: Uri): List<Song>? {
        var songs: List<Song>? = null

        if (uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority != null) {
            val songId =
                when (uri.authority) {
                    AUTHORITY_MEDIA_PROVIDER -> getDocumentId(uri)!!.split(":")[1]
                    AUTHORITY_MEDIA          -> uri.lastPathSegment
                    else                     -> null
                }
            if (songId != null) {
                songs = listOf(SongLoader.getSong(this, songId.toLong()))
            }
        }

        if (songs == null) {
            val file: File? =
                if (uri.authority != null && uri.authority == AUTHORITY_DOCUMENTS_PROVIDER) {
                    File(
                        Environment.getExternalStorageDirectory(),
                        uri.path!!.split(Regex("^.*:.*$"), 2)[1]
                    )
                } else {
                    val path = getFilePathFromUri(this, uri)
                    when {
                        path != null     -> File(path)
                        uri.path != null -> File(uri.path!!)
                        else             -> null
                    }
                }

            if (file != null) {
                songs = SongLoader.getSongs(
                    SongLoader.makeSongCursor(this,
                                              "${MediaStore.Audio.AudioColumns.DATA}=?",
                                              arrayOf(file.absolutePath)
                    )
                )
            }
        }

        return songs
    }

    private fun handleSearchRequest(intent: Intent): PlayRequest? {
        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = SearchQueryHelper.getSongs(this, intent.extras!!)
                if (songs.isNotEmpty()) return PlayRequest(songs, 0)
            }
        }
        return null
    }

    private fun handleExtra(intent: Intent): PlayRequest? {
        when (intent.type) {
            MediaStore.Audio.Playlists.CONTENT_TYPE -> {
                val id = parseIdFromIntent(intent, "playlistId", "playlist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = PlaylistSongLoader.getPlaylistSongList(this, id)
                    if (songs.isNotEmpty()) return PlayRequest(songs, position)
                }
            }
            MediaStore.Audio.Albums.CONTENT_TYPE    -> {
                val id = parseIdFromIntent(intent, "albumId", "album")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = AlbumLoader.getAlbum(this, id).songs
                    if (songs.isNotEmpty()) return PlayRequest(songs, position)
                }
            }
            MediaStore.Audio.Artists.CONTENT_TYPE   -> {
                val id = parseIdFromIntent(intent, "artistId", "artist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = ArtistLoader.getArtist(this, id).songs
                    if (songs.isNotEmpty()) return PlayRequest(songs, position)
                }
            }
        }
        return null
    }


    private fun processShortCut(shortcutType: Int) {
        var shuffleMode = ShuffleMode.NONE
        val playlist: SmartPlaylist? = when (shortcutType) {
            SHORTCUT_TYPE_SHUFFLE_ALL -> {
                reportShortcutUsed(this, ShuffleAllShortcutType.id)
                shuffleMode = ShuffleMode.SHUFFLE
                ShuffleAllPlaylist(applicationContext)

            }
            SHORTCUT_TYPE_TOP_TRACKS  -> {
                reportShortcutUsed(this, TopTracksShortcutType.id)
                MyTopTracksPlaylist(applicationContext)

            }
            SHORTCUT_TYPE_LAST_ADDED  -> {
                reportShortcutUsed(this, LastAddedShortcutType.id)
                LastAddedPlaylist(applicationContext)
            }
            else                      -> null
        }
        val songs = playlist?.getSongs(applicationContext)

        if (songs != null) {
            MusicPlayerRemote.playQueue(
                songs,
                if (shuffleMode == ShuffleMode.SHUFFLE) Random.nextInt(songs.size) else 0,
                true,
                shuffleMode,
            )
            startService(
                Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY
                }
            )
        } else {
            Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val AUTHORITY_MEDIA_PROVIDER = "com.android.providers.media.documents"
        const val AUTHORITY_DOCUMENTS_PROVIDER = "com.android.externalstorage.documents"
        const val AUTHORITY_MEDIA = "media"

        data class PlayRequest(val songs: List<Song>, val position: Int)

        fun getFilePathFromUri(context: Context, uri: Uri): String? {
            val column = "_data"
            val projection = arrayOf(column)

            val cursor: Cursor? =
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    null
                )

            runCatching {
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndexOrThrow(column)
                        return it.getString(columnIndex)
                    }
                }
            }.also {
                if (it.isFailure && it.exceptionOrNull() != null) {
                    val errMsg = it.exceptionOrNull()?.stackTraceToString().orEmpty()
                    ErrorNotification.postErrorNotification(it.exceptionOrNull()!!, errMsg)
                    Log.e(MusicPlayerRemote.TAG, errMsg)
                }
            }

            return null
        }

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
        const val SHORTCUT_TYPE = "player.phonograph.appshortcuts.ShortcutType"
        const val SHORTCUT_TYPE_SHUFFLE_ALL = 0
        const val SHORTCUT_TYPE_TOP_TRACKS = 1
        const val SHORTCUT_TYPE_LAST_ADDED = 2
        const val SHORTCUT_TYPE_NONE = 3
    }

}