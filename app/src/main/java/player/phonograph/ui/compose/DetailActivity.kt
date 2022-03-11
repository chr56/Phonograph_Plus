/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.datatype.DataTypes
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.MusicUtil
import java.io.File
import java.io.IOException
import player.phonograph.model.getReadableDurationString

class DetailActivity : ToolbarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun Content() {
        PhonographTheme {
            DetailActivityContent(songTest)
        }
    }

    override val title: String
        get() = getString(R.string.label_details)
    override val backClick: () -> Unit
        get() = { this.onBackPressed() }

    private var info: SongInfo? = null
    private fun load(song: Song, songInfo: SongInfo) {
        val coroutines = CoroutineScope(Dispatchers.IO)
        coroutines.launch {
            loadSong(song, songInfo)
        }
    }
}

@Composable
internal fun DetailActivityContent(song: Song) {
    val info = SongInfo()
    loadSong(song, info)

    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(state = scrollState).padding(4.dp)) {
        Item(stringResource(id = R.string.label_file_name), info.fileName ?: "-")
        Item(stringResource(id = R.string.label_file_path), info.filePath ?: "-")
        Item(stringResource(id = R.string.label_track_length), getReadableDurationString(((info.trackLength ?: -1) * 1000)))
        Item(stringResource(id = R.string.label_file_format), info.fileFormat ?: "-")
        Item(stringResource(id = R.string.label_file_size), getFileSizeString(info.fileSize ?: -1))
        Item(stringResource(id = R.string.label_bit_rate), info.bitRate ?: "-")
        Item(stringResource(id = R.string.label_sampling_rate), info.samplingRate ?: "-")
    }
}

internal val songTest = Song.EMPTY_SONG

@Preview(showBackground = true)
@Composable
fun Item(tag: String = "TagName", value: String = "TagValue") {
    Box {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text(text = tag, modifier = Modifier.padding(end = 8.dp).align(Alignment.Bottom).defaultMinSize(minWidth = 64.dp), style = TextStyle(fontWeight = FontWeight.Bold))
            Text(text = value, modifier = Modifier.align(Alignment.Bottom))
        }
    }
}

// @Preview(showBackground = true)
@Composable
internal fun PreviewContent() {
    PhonographTheme(previewMode = true) {
        DetailActivityContent(songTest)
    }
}

data class SongInfo(
    var fileName: String? = "",
    var filePath: String? = "",
    var fileFormat: String? = "",
    var bitRate: String? = "",
    var samplingRate: String? = "",
    var fileSize: Long? = 0,
    var trackLength: Long? = 0,
    var title: String? = "",
    var artist: String? = "",
    var album: String? = "",
    var albumArtist: String? = "",
    var year: String? = "",
    var genre: String? = "",
    var track: String? = "",
    var comment: String? = "",
    var otherTags: MutableMap<String, String>? = null,
)
fun loadSong(song: Song, songInfo: SongInfo) {
    val songFile = File(song.data)
    if (songFile.exists()) {
        songInfo.fileName = songFile.name
        songInfo.filePath = songFile.absolutePath
        songInfo.fileSize = songFile.length()
        try {
            val audioFile: AudioFile = AudioFileIO.read(songFile)
            // files of the song
            val audioHeader: AudioHeader = audioFile.audioHeader
            songInfo.fileFormat = audioHeader.format
            songInfo.trackLength = (audioHeader.trackLength * 1000).toLong()
            songInfo.bitRate = audioHeader.bitRate // + " kb/s"
            songInfo.samplingRate = audioHeader.sampleRate // + " Hz"
            // tags of the song
            songInfo.title = song.title
            songInfo.artist = song.artistName
            songInfo.album = song.albumName
            songInfo.albumArtist = audioFile.tag.getFirst(FieldKey.ALBUM_ARTIST)
            songInfo.year = audioFile.tag.getFirst(FieldKey.YEAR)
            songInfo.genre = audioFile.tag.getFirst(FieldKey.GENRE)
            songInfo.track = audioFile.tag.getFirst(FieldKey.TRACK)
            songInfo.comment = audioFile.tag.getFirst(FieldKey.COMMENT)
            // tags of custom field
            val customTags: MutableMap<String, String> = HashMap()
            val customInfoField = audioFile.tag.getFields("TXXX")
            if (customInfoField != null && customInfoField.size > 0) {
                if (customInfoField.size >= 32) {
                    Toast.makeText(App.instance, "Other tags in this song is too many, only show the first 32 entries", Toast.LENGTH_LONG).show()
                }

                val limit = if (customInfoField.size <= 32) customInfoField.size else 31
                for (index in 0..limit) {
                    val field = customInfoField[index] as AbstractID3v2Frame
                    customTags.put(
                        field.body.getObjectValue(DataTypes.OBJ_DESCRIPTION) as String,
                        field.body.getObjectValue(DataTypes.OBJ_TEXT) as String
                    )
                }
                songInfo.otherTags = customTags
            }
        } catch (e: Exception) {
            when (e) {
                is CannotReadException, is TagException, is ReadOnlyFileException, is InvalidAudioFrameException, is IOException ->
                    {
                        Log.e("TagRead", "error while reading the song file", e)
                        songInfo.trackLength = song.duration // fallback
                    }
                else -> throw e
            }
        }
    }
}

internal fun getFileSizeString(sizeInBytes: Long): String {
    val fileSizeInKB = sizeInBytes / 1024
    val fileSizeInMB = fileSizeInKB / 1024
    return "$fileSizeInMB MB"
}
