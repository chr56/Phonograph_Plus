/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
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
import player.phonograph.util.SongDetailUtil.SongInfo
import player.phonograph.util.SongDetailUtil.getFileSizeString
import player.phonograph.util.SongDetailUtil.loadArtwork
import player.phonograph.util.SongDetailUtil.loadSong

import player.phonograph.model.getReadableDurationString
import player.phonograph.util.SongDetailUtil

class DetailActivity : ToolbarActivity() {

    lateinit var model: DetailModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val song = intent.extras?.getParcelable<Song>("song")

        model = ViewModelProvider(this).get(DetailModel::class.java)

        song?.let {
            model.info = loadSong(song)
            model.artwork = loadArtwork(this, song = song)
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
    var artwork: MutableState<SongDetailUtil.BitmapPaletteWrapper?> = mutableStateOf(null)
}

@Composable
private fun DetailActivityContent(viewModel: DetailModel) {
    val info by remember { mutableStateOf(viewModel.info) }
    val wrapper by remember { viewModel.artwork }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState)
            .fillMaxSize()
    ) {
        // Cover Artwork
        val (painter, color) = if (wrapper != null) {
            Pair(
                BitmapPainter(wrapper!!.bitmap.asImageBitmap()),
                Color(wrapper!!.paletteColor)
            )
        } else {
            Pair(
                painterResource(R.drawable.default_album_art),
                MaterialTheme.colors.surface
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.CenterHorizontally)
                .background(color)
        ) {

            Image(
                painter = painter,
                contentDescription = "Cover",
                modifier = Modifier.align(Alignment.Center)
                    .sizeIn(
                        maxWidth = maxWidth,
                        maxHeight = maxWidth,
                        minHeight = maxWidth.div(3)
                    )
            )
        }
        // Text Information
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            // File info
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.file), color = MaterialTheme.colors.primaryVariant)
            Item(stringResource(id = R.string.label_file_name), info.fileName ?: "-")
            Item(stringResource(id = R.string.label_file_path), info.filePath ?: "-")
            Item(stringResource(id = R.string.label_track_length), getReadableDurationString(info.trackLength ?: -1))
            Item(stringResource(id = R.string.label_file_format), info.fileFormat ?: "-")
            Item(stringResource(id = R.string.label_file_size), getFileSizeString(info.fileSize ?: -1))
            Item(stringResource(id = R.string.label_bit_rate), info.bitRate ?: "-")
            Item(stringResource(id = R.string.label_sampling_rate), info.samplingRate ?: "-")
            // Common Tag
            Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.lyrics), color = MaterialTheme.colors.primaryVariant)
            Spacer(modifier = Modifier.height(16.dp))
        }
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
