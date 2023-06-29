/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

object SongCollectionLoader {


    fun all(context: Context): List<SongCollection> = runBlocking {
        val allSongs = querySongs(context).intoSongs()
        convertSongsToSongCollections(context, allSongs)
    }

    private suspend fun convertSongsToSongCollections(
        context: Context,
        allSongs: List<Song>
    ): List<SongCollection> {

        val sorted = mutableMapOf<String, MutableList<Song>>()

        allSongs.asFlow().onEach { song: Song ->
            val folder =
                song.data.substringBeforeLast('/', "(root-directory)")
            sorted.getOrPut(folder) { mutableListOf() }.add(song)
        }.collect()


        //
        // collect short names
        //
        val shortNames = mutableMapOf<String, String>()
        sorted.forEach { (folder, _) ->
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
            conflictNames.forEach { name ->
                val items = shortNames.filter { (_, value) -> value == name }
                items.forEach { item ->
                    shortNames[item.key] = item.key //todo: make different
                }
            }
        }

        // convert
        return sorted.map { (folder, songs) ->
            SongCollection(
                shortNames.getOrDefault(folder, "Unknown"),
                songs,
                folder
            )
        }
    }
}