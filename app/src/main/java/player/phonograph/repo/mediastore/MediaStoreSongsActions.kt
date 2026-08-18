/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.debug
import player.phonograph.foundation.SafeIdentifierGenerator
import player.phonograph.foundation.mediastore.mediastoreUriSongsExternal
import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
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
            mediastoreUriSongsExternal(), "${Audio.Media.DATA} = ?", arrayOf(song.data)
        )
        // if it failed
        return if (output <= 0) {
            debug { Log.w(TAG, "fail to delete ${song.title}(${song.data})") }
            false
        } else {
            true
        }
    }

    class ValidationResult(val invalid: List<Song>, val overflowed: List<Song>)

    /**
     * Debug function, check invalid and embedding-overflowed ids
     */
    suspend fun validateSongsIds(context: Context): ValidationResult {
        val invalid = mutableListOf<Song>()
        val overflowed = mutableListOf<Song>()
        for (song in Songs.all(context)) {
            val id = song.id
            if (SafeIdentifierGenerator.checkInvalidation(id)) invalid.add(song)
            if (SafeIdentifierGenerator.checkEmbeddingOverflowed(id)) overflowed.add(song)
        }
        return ValidationResult(invalid = invalid, overflowed = overflowed)
    }


    private const val TAG = "DeleteSongs"
}