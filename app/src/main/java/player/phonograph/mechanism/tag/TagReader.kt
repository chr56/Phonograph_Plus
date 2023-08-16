/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagField
import player.phonograph.util.reportError
import java.io.File

fun loadSongInfo(song: Song): SongInfoModel = loadSongInfo(File(song.data))

fun loadSongInfo(songFile: File): SongInfoModel {
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
        val keys =
            arrayOf(
                FieldKey.TITLE,
                FieldKey.ARTIST,
                FieldKey.ALBUM,
                FieldKey.ALBUM_ARTIST,
                FieldKey.COMPOSER,
                FieldKey.LYRICIST,
                FieldKey.YEAR,
                FieldKey.GENRE,
                FieldKey.DISC_NO,
                FieldKey.TRACK,
                FieldKey.COMMENT,
            )
        val tagFields = readTagFields(audioFile, keys)
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
            tagFields = tagFields,
            tagFormat = tagFormat,
            allTags,
        )
    } catch (e: Exception) {
        reportError(e, "TagRead", "error while reading the song file")
        return SongInfoModel.EMPTY().also {
            it.fileName = StringFilePropertyField(fileName)
            it.filePath = StringFilePropertyField(filePath)
            it.fileSize = LongFilePropertyField(fileSize)
            it.tagFields = mapOf(FieldKey.TITLE to TagField(FieldKey.TITLE, "error while reading the song file"))
        }
    }
}

private fun readTagField(audioFile: AudioFile, id: FieldKey): TagField =
    TagField(id, audioFile.tag.getFirst(id))


private fun readTagFields(audioFile: AudioFile, keys: Array<FieldKey>): Map<FieldKey, TagField> =
    keys.associateWith { key ->
        TagField(key, audioFile.tag.getFirst(key))
    }
