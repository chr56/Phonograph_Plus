/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.m3u

import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.datetimeSufix
import android.content.Context
import kotlin.text.Charsets.UTF_8
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStream

object M3UWriter {

    @Throws(IOException::class)
    fun write(context: Context, targetDir: File, playlist: Playlist): File {
        val songs = playlist.getSongs(context)
        val filename: String =
            if (playlist is SmartPlaylist) {
                // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
                playlist.name + datetimeSufix(currentDate())
            } else {
                playlist.name
            }
        return write(targetDir, songs, filename)
    }

    @Throws(IOException::class)
    fun write(targetDir: File, songs: List<Song>, filename: String): File {
        if (!targetDir.exists()) targetDir.mkdirs()
        val file = File(targetDir, "$filename.$EXTENSION")
        file.bufferedWriter(UTF_8).use { writer ->
            writeImpl(writer, songs, addHeader = true)
        }
        return file
    }


    @Throws(IOException::class)
    fun write(outputStream: OutputStream, songs: List<Song>, addHeader: Boolean) {
        outputStream.bufferedWriter(UTF_8).use { writer ->
            writeImpl(writer, songs, addHeader)
        }
    }

    private fun songLine(song: Song): String =
        "\n$ENTRY${song.duration}$DURATION_SEPARATOR${song.artistName} - ${song.title}\n${song.data}"

    private fun writeImpl(writer: BufferedWriter, songs: List<Song>, addHeader: Boolean) {
        if (addHeader) writer.write(HEADER)
        for (song in songs) {
            writer.write(songLine(song))
        }
    }


    private const val EXTENSION = "m3u"
    private const val HEADER = "#EXTM3U"
    private const val ENTRY = "#EXTINF:"
    private const val DURATION_SEPARATOR = ","
}
