/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import legacy.phonograph.MediaStoreCompat
import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.foundation.error.record
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriPlaylists
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

object MediaStorePlaylistsActions {

    fun playlistId(uri: Uri?): Long = uri?.lastPathSegment?.toLong() ?: -1

    fun create(
        context: Context,
        name: String,
        songs: Collection<Song>,
    ): Uri? {
        val uri = insert(context, name) ?: return null
        val editResult = amendSongs(context, uri, songs)
        return if (editResult) uri else null
    }

    fun create(
        context: Context,
        name: String,
    ): Uri? {
        val uri = insert(context, name)
        return uri
    }

    private fun insert(
        context: Context,
        name: String,
        volume: String = MEDIASTORE_VOLUME_EXTERNAL,
    ): Uri? {
        val values = ContentValues(1).apply {
            put(MediaStoreCompat.Audio.PlaylistsColumns.NAME, name)
        }
        val playlistsUri = mediastoreUriPlaylists(volume)
        return try {
            val uri = context.contentResolver.insert(playlistsUri, values)
            if (uri != null) {
                // Necessary because somehow the MediaStoreObserver doesn't work for playlists
                context.contentResolver.notifyChange(uri, null)
                EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            }
            uri
        } catch (e: Exception) {
            record(context, e, TAG)
            null
        }
    }


    fun rename(
        context: Context,
        playlistUri: Uri,
        newName: String,
    ): Boolean = try {
        val result = context.contentResolver.update(
            playlistUri,
            ContentValues().apply { put(MediaStoreCompat.Audio.PlaylistsColumns.NAME, newName) },
            null, null
        )
        if (result > 0) {
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(playlistUri, null)
            EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        record(context, e, TAG)
        false
    }


    fun amendSongs(
        context: Context,
        playlistUri: Uri,
        songs: Collection<Song>,
    ): Boolean {
        var cursor: Cursor? = null
        return try {
            val base = try {
                cursor = context.contentResolver.query(
                    playlistUri,
                    arrayOf(Playlists.Members.PLAY_ORDER),
                    null,
                    null,
                    "${Playlists.Members.PLAY_ORDER} DESC"
                )
                if (cursor != null && cursor.moveToFirst()) {
                    cursor.getInt(0) + 1
                } else {
                    0
                }
            } catch (e: Exception) {
                record(context, e, TAG)
                0
            } finally {
                cursor?.close()
            }

            @Suppress("SameParameterValue")
            fun items(
                songs: Collection<Song>,
                offset: Int,
                length: Int,
                base: Int,
            ): Array<ContentValues?> {
                var len = length
                if (offset + len > songs.size) {
                    len = songs.size - offset
                }
                val songs = songs.toList()
                val contentValues = arrayOfNulls<ContentValues>(len)
                for (i in 0 until len) {
                    contentValues[i] = ContentValues().apply {
                        put(Playlists.Members.PLAY_ORDER, base + offset + i)
                        put(Playlists.Members.AUDIO_ID, songs[offset + i].id)
                    }
                }
                return contentValues
            }

            var numInserted = 0
            var offSet = 0
            while (offSet < songs.size) {
                numInserted += context.contentResolver.bulkInsert(playlistUri, items(songs, offSet, 1000, base))
                offSet += 1000
            }

            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(playlistUri, null)
            EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            true
        } catch (e: Exception) {
            record(context, e, TAG)
            false
        }
    }

    fun removeSong(
        context: Context,
        playlistMembersUri: Uri,
        songId: Long,
        position: Long,
    ): Boolean {
        return try {
            val deleted = context.contentResolver.delete(
                playlistMembersUri,
                "${Playlists.Members.PLAY_ORDER} = ? AND ${Playlists.Members.AUDIO_ID} = ?",
                arrayOf((position + 1).toString(), songId.toString()) // start with 1
            )
            if (deleted > 0) {
                // Necessary because somehow the MediaStoreObserver doesn't work for playlists
                context.contentResolver.notifyChange(playlistMembersUri, null)
                EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            }
            deleted > 0
        } catch (e: Exception) {
            record(context, e, TAG)
            false
        }
    }

    // fun swapSong(
    //     context: Context,
    //     playlistUri: Uri,
    //     positionA: Int,
    //     positionB: Int,
    // ): Boolean {
    //     return false
    // }

    fun moveSong(
        context: Context,
        playlistUri: Uri,
        from: Int,
        to: Int,
    ): Boolean {
        val playlistId = playlistId(playlistUri)
        return try {
            val result =
                Playlists.Members.moveItem(context.contentResolver, playlistId, from, to)
            if (result) {
                // Necessary because somehow the MediaStoreObserver doesn't work for playlists
                context.contentResolver.notifyChange(playlistUri, null)
                EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            }
            result
        } catch (e: Exception) {
            record(context, e, TAG)
            false
        }
    }

    fun delete(
        context: Context,
        playlistUri: Uri,
    ): Boolean {
        val playlistId = playlistId(playlistUri)
        return try {
            val result = context.contentResolver.delete(
                mediastoreUriPlaylists(MEDIASTORE_VOLUME_EXTERNAL),
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(playlistId.toString())
            )
            if (result > 0) {
                // Necessary because somehow the MediaStoreObserver doesn't work for playlists
                context.contentResolver.notifyChange(playlistUri, null)
                EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            }
            result > 0
        } catch (e: Exception) {
            record(context, e, TAG)
            false
        }
    }


    private const val TAG = "MediaStorePlaylistsActions"
}