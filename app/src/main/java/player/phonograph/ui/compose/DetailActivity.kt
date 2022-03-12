/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.MusicUtil
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.SongDetailUtil.SongInfo
import player.phonograph.util.SongDetailUtil.getFileSizeString
import player.phonograph.util.SongDetailUtil.loadArtwork
import player.phonograph.util.SongDetailUtil.loadSong
import java.io.File
import java.io.IOException
import player.phonograph.model.getReadableDurationString

class DetailActivity : ToolbarActivity() {

    lateinit var model: DetailModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val song = intent.extras?.getParcelable<Song>("song")

        model = ViewModelProvider(this).get(DetailModel::class.java)

        song?.let {
            model.info = loadSong(song)
            loadArtwork(this, song = song, bitmapHolder = model.bitmapHolder)
        }
    }

    @Composable
    override fun Content() {
        PhonographTheme {
            DetailActivityContent(model)
        }
    }

    override val title: String
        get() = getString(R.string.label_details)

    private val coroutines: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }
    private fun load(song: Song) {
        coroutines.launch {
            model.info = loadSong(song)
        }
    }
}

class DetailModel : ViewModel() {
    var info: SongInfo = SongInfo()
    var bitmapHolder: SongDetailUtil.BitmapHolder = SongDetailUtil.BitmapHolder()
}

@Composable
private fun DetailActivityContent(viewModel: DetailModel) {
    val info = viewModel.info
    var bitmap by remember { mutableStateOf(viewModel.bitmapHolder.bitmap) }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .padding(4.dp)
    ) {
        // Cover
        bitmap?.let {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Cover",
            )
        }
        // File info
        Title(stringResource(R.string.file), color = MaterialTheme.colors.primaryVariant)
        Item(stringResource(id = R.string.label_file_name), info.fileName ?: "-")
        Item(stringResource(id = R.string.label_file_path), info.filePath ?: "-")
        Item(stringResource(id = R.string.label_track_length), getReadableDurationString(((info.trackLength ?: -1) * 1000)))
        Item(stringResource(id = R.string.label_file_format), info.fileFormat ?: "-")
        Item(stringResource(id = R.string.label_file_size), getFileSizeString(info.fileSize ?: -1))
        Item(stringResource(id = R.string.label_bit_rate), info.bitRate ?: "-")
        Item(stringResource(id = R.string.label_sampling_rate), info.samplingRate ?: "-")
        // Common Tag
        Spacer(modifier = Modifier.height(12.dp))
        Title(stringResource(R.string.music_tags), color = MaterialTheme.colors.primaryVariant)
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
        Spacer(modifier = Modifier.height(8.dp))
        Title(stringResource(R.string.other_information))
        Item(stringResource(id = R.string.comment), info.comment ?: "-")
        info.otherTags?.let { tags ->
            for (tag in tags) {
                Item(tag.key, tag.value)
            }
        }
        // Lyrics
        Spacer(modifier = Modifier.height(12.dp))
        Title(stringResource(R.string.lyrics), color = MaterialTheme.colors.primaryVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun Item(tag: String = "KeyName", value: String = "KeyValue") {
    VerticalTextItem(tag, value)
}

@Preview(showBackground = true)
@Composable
internal fun PreviewContent() {
    PhonographTheme(previewMode = true) {
        DetailActivityContent(DetailModel().apply { info = SongInfo("name") })
    }
}
