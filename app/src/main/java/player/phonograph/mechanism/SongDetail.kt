/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package player.phonograph.mechanism

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import player.phonograph.mechanism.tageditor.TagFormat
import player.phonograph.mechanism.tageditor.readAllTags
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagField
import player.phonograph.util.reportError
import java.io.File

object SongDetail {

    fun readSong(song: Song): SongInfoModel = readSong(File(song.data))

    fun readSong(songFile: File): SongInfoModel {
        require(songFile.exists()) { "${songFile.path} doesn't exist! please check file or permission of device storage." }
        val fileName = songFile.name
        val filePath = songFile.absolutePath
        val fileSize = songFile.length()
        try {
            val audioFile: AudioFile = AudioFileIO.read(songFile)
            // files of the song
            val audioHeader: AudioHeader = audioFile.audioHeader
            val fileFormat = audioHeader.format
            val trackLength = (audioHeader.trackLength * 1000).toLong()
            val bitRate = "${audioHeader.bitRate} kb/s"
            val samplingRate = "${audioHeader.sampleRate} Hz"
            // tags of the song
            val title = readTagField(audioFile, FieldKey.TITLE)
            val artist = readTagField(audioFile, FieldKey.ARTIST)
            val album = readTagField(audioFile, FieldKey.ALBUM)
            val albumArtist = readTagField(audioFile, FieldKey.ALBUM_ARTIST)
            val composer = readTagField(audioFile, FieldKey.COMPOSER)
            val lyricist = readTagField(audioFile, FieldKey.LYRICIST)
            val year = readTagField(audioFile, FieldKey.YEAR)
            val genre = readTagField(audioFile, FieldKey.GENRE)
            val track = readTagField(audioFile, FieldKey.TRACK)
            val comment = readTagField(audioFile, FieldKey.COMMENT)
            // tags of custom field
            val tagFormat = TagFormat.of(audioFile.tag)
            val allTags = readAllTags(audioFile)
            return SongInfoModel(
                fileName = StringFilePropertyField(fileName),
                filePath = StringFilePropertyField(filePath),
                fileFormat = StringFilePropertyField(fileFormat),
                bitRate = StringFilePropertyField(bitRate),
                samplingRate = StringFilePropertyField(samplingRate),
                fileSize = LongFilePropertyField(fileSize),
                trackLength = LongFilePropertyField(trackLength),
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                composer = composer,
                lyricist = lyricist,
                year = year,
                genre = genre,
                track = track,
                comment = comment,
                tagFormat = tagFormat,
                allTags,
            )
        } catch (e: Exception) {
            reportError(e, "TagRead", "error while reading the song file")
            return SongInfoModel.EMPTY().also {
                it.fileName = StringFilePropertyField(fileName)
                it.filePath = StringFilePropertyField(filePath)
                it.fileSize = LongFilePropertyField(fileSize)
                it.title = TagField(FieldKey.KEY, "error while reading the song file")
            }
        }
    }

    private fun readTagField(audioFile: AudioFile, id: FieldKey): TagField =
        TagField(id, audioFile.tag.getFirst(id))

}
