/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.util.Log

object MediaStoreSongCollections {

    suspend fun all(context: Context): List<SongCollection> {
        val allSongs = querySongs(context).intoSongs()
        return convertSongsToSongCollections(context, allSongs)
    }

    private suspend fun convertSongsToSongCollections(
        context: Context,
        allSongs: List<Song>,
    ): List<SongCollection> {

        //
        // Group songs by their folder
        //
        val grouped: MutableMap<String, MutableList<Song>> = mutableMapOf() // folder names & songs
        for (song in allSongs) {
            val path = song.data.stripPrefix()
            val folder = path.substringBeforeLast('/').ifEmpty { "/" }
            grouped.getOrPut(folder) { mutableListOf() }.add(song)
        }


        //
        // Generate short names
        //
        val shortNames: MutableMap<String, String> = generateUniqueShortNames(grouped.keys)

        // convert
        return grouped.map { (folder, songs) ->
            SongCollection(
                name = shortNames.getOrDefault(folder, "Unknown"),
                songs = songs,
                detail = folder
            )
        }
    }

    /**
     * remove path prefix for Android (likes `/storage/emulated/0`)
     */
    private fun String.stripPrefix(): String =
        substringAfter("/storage").substringAfter("/emulated/0")// remove prefix for Android

    /***
     * Generate unique short names for folder paths (base on their parent folder names)
     * @param paths folder names
     * @return map of short names, key is full folder name, value is the unique short name
     */
    private fun generateUniqueShortNames(folders: Set<String>): MutableMap<String, String> {

        val shortNames = mutableMapOf<String, String>() // folder names & last folder name
        for (folder in folders) {
            val short = folder.substringAfterLast('/')
            shortNames[folder] = short
        }


        // todo: improve `solve name conflicts`

        //
        // solve name conflicts
        //
        val uniques = shortNames.values.toSet()
        if (uniques.size != shortNames.size) {
            // name conflict!
            val conflictNames = mutableListOf<String>().let { list ->
                shortNames.values.toCollection(list).also {
                    val result = it.removeAll(uniques)
                    if (!result) {
                        Log.w("SongCollection", "can not solve conflicts")
                        it.clear()
                    }
                }
            }
            for (name in conflictNames) {
                val items = shortNames.filter { (_, value) -> value == name }
                for (item in items) {
                    shortNames[item.key] = item.key //todo: make different
                }
            }
        }
        return shortNames
    }
}