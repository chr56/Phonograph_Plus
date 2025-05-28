/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.debug
import player.phonograph.util.isEmbeddingOverflow
import player.phonograph.util.mediastoreUriSongs
import player.phonograph.util.produceSafeId
import android.content.Context
import android.provider.MediaStore.Audio
import android.util.Log

object MediaStoreSongsActions {

    /**
     * delete songs via MediaStore
     * @return list of songs that failed to delete
     */
    fun delete(context: Context, songs: Collection<Song>): List<Song> =
        songs.filter { song -> !deleteViaMediaStoreImpl(context, song) }

    /**
     * delete song via MediaStore
     * @return success or not
     */
    fun delete(context: Context, song: Song): Boolean = deleteViaMediaStoreImpl(context, song)


    /**
     * @return success or not
     */
    private fun deleteViaMediaStoreImpl(context: Context, song: Song): Boolean {
        val output = context.contentResolver.delete(
            mediastoreUriSongs(MEDIASTORE_VOLUME_EXTERNAL), "${Audio.Media.DATA} = ?", arrayOf(song.data)
        )
        // if it failed
        return if (output <= 0) {
            debug { Log.w(TAG, "fail to delete ${song.title}(${song.data})") }
            false
        } else {
            true
        }
    }

    suspend fun dumpAllSongIds(context: Context): Collection<Long> = Songs.all(context).map { it.id }

    suspend fun checkEmbeddedIdOverflow(context: Context): Collection<Song> {

        val ids = dumpAllSongIds(context)
        val overflowed = ids.filter { isEmbeddingOverflow(it) }

        return if (overflowed.isNotEmpty()) {
            overflowed.mapNotNull { Songs.id(context, it) }
        } else {
            emptyList()
        }
    }

    suspend fun checkIdConflict(context: Context): Collection<Song> {
        val ids = dumpAllSongIds(context)
        val safeIds = ids.mapIndexed { pos, id -> produceSafeId(id, pos) }

        val uniques = safeIds.toSet()
        if (uniques.size != ids.size) {
            val conflicted = ids - uniques
            return conflicted.mapNotNull { Songs.id(context, it) }
        } else {
            return emptyList()
        }
    }

    private const val TAG = "DeleteSongs"
}