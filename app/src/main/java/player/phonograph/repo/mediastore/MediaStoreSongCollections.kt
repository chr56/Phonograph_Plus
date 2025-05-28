/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context

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
    private fun generateUniqueShortNames(paths: Set<String>): MutableMap<String, String> {
        val shortNames: MutableMap<String, String> = mutableMapOf()

        // First attempt: use the last folder name as the short name
        val initialShortNames: MutableMap<String, String> = mutableMapOf()
        for (folder in paths) {
            val shortName = folder.substringAfterLast('/')
            initialShortNames[folder] = shortName.ifEmpty { "/" }
        }


        // Group folders by their short names to identify conflicts, key is short name, value is folders with same short names
        val shortNameGroups: Map<String, List<String>> = groupByShortName(initialShortNames)
        for ((shortName, folders) in shortNameGroups) {
            if (folders.size == 1) {
                // No conflict, use the original short name
                shortNames[folders.first()] = shortName
            } else {
                // Conflict, update short names
                val resolvedNames = resolveConflicts(folders)
                for ((folder, resolvedName) in resolvedNames) {
                    shortNames[folder] = resolvedName
                }
            }
        }

        return shortNames
    }

    /**
     * Group by short names
     * @param folders short name map, key is full path, value is its short name
     * @return  key is short name, value is folders with same short names
     */
    private fun groupByShortName(folders: MutableMap<String, String>): Map<String, List<String>> =
        folders.entries.groupBy { it.value }
            .mapValues { it.value.map { it.key } }

    /**
     * Handle conflicts by progressively adding parent directory components
     * @return key is full folder name, value is the unique short name
     */
    private fun resolveConflicts(conflictedFolders: List<String>): Map<String, String> {

        val result: MutableMap<String, String> = mutableMapOf()
        val remainingConflicts: MutableList<String> = conflictedFolders.toMutableList()

        // Extend with parent folders
        for (depth in 2..5) {

            val candidates: MutableMap<String, String> = mutableMapOf()

            // Subsequence attempt: generate candidate names for remaining folders
            for (folder in remainingConflicts) {
                val stem = folder.stripPrefix().removePrefix("/")
                val pathSegments = stem.split('/').filter { it.isNotEmpty() }

                val candidate = if (depth <= pathSegments.size) {
                    pathSegments.takeLast(depth).joinToString("/") // take backward
                } else {
                    stem // exhausted or root
                }

                candidates[folder] = candidate
            }

            // Check conflicts and update
            val shortNameGroups = groupByShortName(candidates)
            for ((candidateShortName, foldersWithSameShortName) in shortNameGroups) {
                if (foldersWithSameShortName.size == 1) {
                    // No conflict
                    val folder = foldersWithSameShortName.first()
                    result[folder] = candidateShortName
                    remainingConflicts.remove(folder)
                }
            }

            // done if no more conflicts
            if (remainingConflicts.isEmpty()) break
        }

        // Remaining conflicts: use full path
        if (remainingConflicts.isNotEmpty()) {
            for (folder in remainingConflicts) {
                result[folder] = folder.stripPrefix()
            }
        }

        return result
    }
}