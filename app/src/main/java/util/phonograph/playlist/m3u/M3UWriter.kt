/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.m3u

import player.phonograph.model.Song
import kotlin.text.Charsets.UTF_8
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStream

object M3UWriter {

    @Throws(IOException::class)
    fun write(targetDir: File, songs: List<Song>, filename: String, addHeader: Boolean = true): File {
        if (!targetDir.exists()) targetDir.mkdirs()
        val file = File(targetDir, "$filename.$FILE_EXTENSION")
        if (!file.exists()) {
            file.createNewFile()
        } else {
            throw IOException("File(${file.path}) already existed!")
        }
        file.bufferedWriter(UTF_8).use { writer ->
            writeImpl(writer, songs, addHeader)
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
        "\n$ENTRY_HEADER${song.duration}$SEPARATOR_COMMA${song.artistName}$SEPARATOR_HYPHEN${song.title}\n${song.data}"

    private fun writeImpl(writer: BufferedWriter, songs: List<Song>, addHeader: Boolean) {
        if (addHeader) writer.write(FILE_HEADER)
        for (song in songs) {
            writer.write(songLine(song))
        }
    }


    private const val FILE_EXTENSION = "m3u"
    private const val FILE_HEADER = "#EXTM3U"
    private const val ENTRY_HEADER = "#EXTINF:"
    private const val SEPARATOR_COMMA = ","
    private const val SEPARATOR_HYPHEN = " - "
}
