/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.m3u

import player.phonograph.mediastore.loaders.SongLoader
import player.phonograph.model.Song
import android.content.Context
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object M3UParser {

    fun readForSongs(inputStream: InputStream, context: Context): List<Song> {
        val songFiles = readForPaths(inputStream)
        val songs: MutableList<Song> = ArrayList(15)
        for (file in songFiles) {
            songs.add(
                SongLoader.searchByPath(context, file).firstOrNull() ?: Song.EMPTY_SONG
            )
        }
        return if (songs.isNotEmpty()) songs else ArrayList(1)
    }

    fun readForPaths(inputStream: InputStream): List<String> {
        val inputStreamReader = InputStreamReader(inputStream, StandardCharsets.UTF_8)

        val songFiles: MutableList<String> = ArrayList(15)
        val pathRegex = Regex("(/.)*/?.*\\..*")
        inputStreamReader.use { reader ->
            val lines = reader.readLines()
            for (line in lines) {
                if (line[0] == '#') continue // ignore comment
                if (line.startsWith("http")) continue // ignore stream
                if (pathRegex.matches(line)) songFiles.add(line)
            }
        }
        return songFiles
    }

}
