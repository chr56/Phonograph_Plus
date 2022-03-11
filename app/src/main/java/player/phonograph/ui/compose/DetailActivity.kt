/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import org.jaudiotagger.tag.id3.AbstractTagFrame
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
        val song = intent.extras?.getParcelable<Song>("song")

        song?.let {
            info = loadSong(song)
        }
    }

    @Composable
    override fun Content() {
        PhonographTheme {
            DetailActivityContent(info)
        }
    }

    override val title: String
        get() = getString(R.string.label_details)
    override val backClick: () -> Unit
        get() = { this.onBackPressed() }

    private var info: SongInfo = SongInfo()
    private val coroutines: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    private fun load(song: Song) {
        coroutines.launch {
            info = loadSong(song)
        }
    }
}

@Composable
internal fun DetailActivityContent(info: SongInfo) {

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .padding(4.dp)
    ) {
        Item(stringResource(id = R.string.label_file_name), info.fileName ?: "-")
        Item(stringResource(id = R.string.label_file_path), info.filePath ?: "-")
        Item(stringResource(id = R.string.label_track_length), getReadableDurationString(((info.trackLength ?: -1) * 1000)))
        Item(stringResource(id = R.string.label_file_format), info.fileFormat ?: "-")
        Item(stringResource(id = R.string.label_file_size), getFileSizeString(info.fileSize ?: -1))
        Item(stringResource(id = R.string.label_bit_rate), info.bitRate ?: "-")
        Item(stringResource(id = R.string.label_sampling_rate), info.samplingRate ?: "-")
        // Common Tag
        Item(stringResource(id = R.string.title), info.title ?: "-")
        Item(stringResource(id = R.string.artist), info.artist ?: "-")
        Item(stringResource(id = R.string.album), info.album ?: "-")
        Item(stringResource(id = R.string.album_artist), info.albumArtist ?: "-")
        Item(stringResource(id = R.string.composer), info.composer ?: "-")
        Item(stringResource(id = R.string.lyricist), info.lyricist ?: "-")
        Item(stringResource(id = R.string.year), info.year ?: "-")
        Item(stringResource(id = R.string.genre), info.genre ?: "-")
        Item(stringResource(id = R.string.track), info.track ?: "-")
        // Other Tag
        Item(stringResource(id = R.string.comment), info.comment ?: "-")
        info.otherTags?.let { tags ->
            for (tag in tags) {
                Item(tag.key, tag.value)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Item(tag: String = "TagName", value: String = "TagValue") {
    Box {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text(
                text = tag,
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.Top)
                    .defaultMinSize(minWidth = 64.dp),
            )
            SelectionContainer(modifier = Modifier.align(Alignment.Top)) {
                Text(text = value, modifier = Modifier.align(Alignment.Top))
            }
        }
    }
}

// @Preview(showBackground = true)
@Composable
internal fun PreviewContent() {
    PhonographTheme(previewMode = true) {
        DetailActivityContent(SongInfo("name"))
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
    var composer: String? = "",
    var lyricist: String? = "",
    var year: String? = "",
    var genre: String? = "",
    var track: String? = "",
    var comment: String? = "",
    var otherTags: MutableMap<String, String>? = null,
)

fun loadSong(song: Song): SongInfo {
    val file = File(song.data)
    return if (file.exists())
        loadSong(file)
    else
        SongInfo("FILE NOT FOUND")
}

fun loadSong(songFile: File): SongInfo {
    val songInfo = SongInfo()

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
            songInfo.title = audioFile.tag.getFirst(FieldKey.TITLE)
            songInfo.artist = audioFile.tag.getFirst(FieldKey.ARTIST)
            songInfo.album = audioFile.tag.getFirst(FieldKey.ALBUM)
            songInfo.albumArtist = audioFile.tag.getFirst(FieldKey.ALBUM_ARTIST)
            songInfo.composer = audioFile.tag.getFirst(FieldKey.COMPOSER)
            songInfo.lyricist = audioFile.tag.getFirst(FieldKey.LYRICIST)
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
                for (index in 0 until limit) {
                    val field = customInfoField[index] as AbstractTagFrame
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
                        return songInfo.apply { title = "error while reading the song file" }
                    }
                else -> throw e
            }
        }
    }
    return songInfo
}

internal fun getFileSizeString(sizeInBytes: Long): String {
    val fileSizeInKB = sizeInBytes / 1024
    val fileSizeInMB = fileSizeInKB / 1024
    return "$fileSizeInMB MB"
}
