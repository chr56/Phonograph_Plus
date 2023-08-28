/*
 *  Copyright (c) 2022~2023 chr_56 & Karim Abou Zeid (kabouzeid)
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.intoFirstSong
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore

object SongLoader : Loader<Song> {

    override fun all(context: Context): List<Song> =
        querySongs(context).intoSongs()

    override fun id(context: Context, id: Long): Song =
        querySongs(context, "${MediaStore.Audio.AudioColumns._ID} =? ", arrayOf(id.toString())).intoFirstSong()

    fun path(context: Context, path: String): Song =
        querySongs(context, "${MediaStore.Audio.AudioColumns.DATA} =? ", arrayOf(path)).intoFirstSong()

    /**
     * @param withoutPathFilter true if disable path filter
     */
    fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        querySongs(
            context,
            "${MediaStore.Audio.AudioColumns.DATA} LIKE ? ",
            arrayOf(path),
            withoutPathFilter = withoutPathFilter
        ).intoSongs()

    fun searchByTitle(context: Context, title: String): List<Song> {
        val cursor =
            querySongs(context, "${MediaStore.Audio.AudioColumns.TITLE} LIKE ?", arrayOf("%$title%"))
        return cursor.intoSongs()
    }

    fun since(context: Context, timestamp: Long): List<Song> {
        val cursor =
            querySongs(context, "${MediaStore.MediaColumns.DATE_MODIFIED}  > ?", arrayOf(timestamp.toString()))
        return cursor.intoSongs()
    }

    fun latest(context: Context): Song? {
        return all(context).maxByOrNull { it.dateModified }
    }
}
