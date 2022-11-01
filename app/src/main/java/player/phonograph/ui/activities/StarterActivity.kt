/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.helper.SearchQueryHelper
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
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
import java.io.File

class StarterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        startActivity(
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }


    private fun handleIntent(intent: Intent): Boolean {
        var songs: List<Song>? = null
        // uri first
        songs = handleUriPlayRequest(intent)

        // then search
        if (songs == null) {
            songs = handleSearchRequest(intent)
        }
        // then
        if (songs == null) {
            songs = handleExtra(intent)
        }

        return if (songs == null) {
            false
        } else {
            startMusicService(songs)
            true
        }
    }

    private fun startMusicService(queue: List<Song>) {
        MusicPlayerRemote.playQueue(queue, 0, true, null)
    }

    private fun handleUriPlayRequest(intent: Intent): List<Song>? {
        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            val songs = parseUri(uri)
            if (songs != null) return songs
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

    private fun handleSearchRequest(intent: Intent): List<Song>? {
        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = SearchQueryHelper.getSongs(this, intent.extras!!)
                if (songs.isNotEmpty()) return songs
            }
        }
        return null
    }

    private fun handleExtra(intent: Intent): List<Song>? {
        when (intent.type) {
            MediaStore.Audio.Playlists.CONTENT_TYPE -> {
                val id = parseIdFromIntent(intent, "playlistId", "playlist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = PlaylistSongLoader.getPlaylistSongList(this, id)
                    if (songs.isNotEmpty()) return songs
                }
            }
            MediaStore.Audio.Albums.CONTENT_TYPE    -> {
                val id = parseIdFromIntent(intent, "albumId", "album")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = AlbumLoader.getAlbum(this, id).songs
                    if (songs.isNotEmpty()) return songs
                }
            }
            MediaStore.Audio.Artists.CONTENT_TYPE   -> {
                val id = parseIdFromIntent(intent, "artistId", "artist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = ArtistLoader.getArtist(this, id).songs
                    if (songs.isNotEmpty()) return songs
                }
            }
        }
        return null
    }

    companion object {
        const val AUTHORITY_MEDIA_PROVIDER = "com.android.providers.media.documents"
        const val AUTHORITY_DOCUMENTS_PROVIDER = "com.android.externalstorage.documents"
        const val AUTHORITY_MEDIA = "media"

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
    }

}