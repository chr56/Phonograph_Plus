/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import player.phonograph.R
import player.phonograph.glide.palette.BitmapPaletteWrapper
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.base.TailTextField
import player.phonograph.ui.compose.base.Title
import player.phonograph.ui.compose.base.VerticalTextItem
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.MusicUtil
import player.phonograph.util.SongDetailUtil.SongInfo
import player.phonograph.util.SongDetailUtil.getFileSizeString
import player.phonograph.util.SongDetailUtil.loadArtwork
import player.phonograph.util.SongDetailUtil.loadSong
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor

class DetailActivity : ComposeToolbarActivity() {
    val model: DetailModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.song = intent.extras?.getParcelable("song")!!
        model.paletteColor = mutableStateOf(Color(primaryColor))
        with(model) {
            info = loadSong(song)
            artwork = loadArtwork(this@DetailActivity, song = song) {
                updateBarsColor()
                isDefaultArtwork.value = false
                paletteColor.value = Color(it.palette.getVibrantColor(primaryColor))
            }
        }
    }

    @Composable
    override fun SetUpContent() {
        PhonographTheme {
            DetailActivityContent(model)
        }
    }

    override val title: String get() = getString(R.string.label_details)

    private fun updateBarsColor() {
        val model: DetailModel by viewModels()
        model.artwork.value.palette.let { palette ->
            val colorInt = palette
                .getVibrantColor(ThemeColor.primaryColor(this)).let { ColorUtil.darkenColor(it) }
            val darkColorInt = ColorUtil.darkenColor(colorInt)
            setNavigationbarColor(darkColorInt)
            setStatusbarColor(darkColorInt)
            appBarColor = Color(colorInt)
        }
    }

    @Composable
    override fun ToolbarActions(rowScope: RowScope) {
        Button(
            onClick = {
                model.editMode.value = !model.editMode.value
            },
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
        ) {
            Image(
                imageVector = Icons.Filled.Edit, contentDescription = "Edit",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.surface)
            )
        }
    }
}

class DetailModel : ViewModel() {
    lateinit var song: Song
    lateinit var info: SongInfo
    lateinit var artwork: MutableState<BitmapPaletteWrapper>
    lateinit var paletteColor: MutableState<Color>
    var isDefaultArtwork = mutableStateOf(true)

    var editMode = mutableStateOf(false)
}

@Composable
private fun DetailActivityContent(viewModel: DetailModel) {
    val info by remember { mutableStateOf(viewModel.info) }
    val wrapper by remember { viewModel.artwork }
    val isDefaultArtwork by remember { viewModel.isDefaultArtwork }
    val paletteColor by remember { viewModel.paletteColor }

    val editMode by remember { viewModel.editMode }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        // Cover Artwork
        if (!isDefaultArtwork) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
                    .background(paletteColor)
            ) {
                Image(
                    painter = BitmapPainter(wrapper.bitmap.asImageBitmap()),
                    contentDescription = "Cover",
                    modifier = Modifier.align(Alignment.Center)
                        .sizeIn(
                            maxWidth = maxWidth,
                            maxHeight = maxWidth,
                            minHeight = maxWidth.div(3)
                        )
                )
            }
        }
        // Text Information
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            // File info
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.file), color = paletteColor)
            Item(stringResource(id = R.string.label_file_name), info.fileName ?: "-")
            Item(stringResource(id = R.string.label_file_path), info.filePath ?: "-")
            Item(stringResource(id = R.string.label_track_length), MusicUtil.getReadableDurationString(info.trackLength ?: -1))
            Item(stringResource(id = R.string.label_file_format), info.fileFormat ?: "-")
            Item(stringResource(id = R.string.label_file_size), getFileSizeString(info.fileSize ?: -1))
            Item(stringResource(id = R.string.label_bit_rate), "${info.bitRate ?: "-"} kb/s")
            Item(stringResource(id = R.string.label_sampling_rate), "${info.samplingRate ?: "-"} Hz")
            // Common Tag
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.music_tags), color = paletteColor)
            Item(stringResource(id = R.string.title), info.title ?: "-", editMode)
            Item(stringResource(id = R.string.artist), info.artist ?: "-", editMode)
            Item(stringResource(id = R.string.album), info.album ?: "-", editMode)
            Item(stringResource(id = R.string.album_artist), info.albumArtist ?: "-", editMode)
            Item(stringResource(id = R.string.composer), info.composer ?: "-", editMode)
            Item(stringResource(id = R.string.lyricist), info.lyricist ?: "-", editMode)
            Item(stringResource(id = R.string.year), info.year ?: "-", editMode)
            Item(stringResource(id = R.string.genre), info.genre ?: "-", editMode)
            Item(stringResource(id = R.string.track), info.track ?: "-", editMode)
            // Other Tag
            Spacer(modifier = Modifier.height(8.dp))
            Title(stringResource(R.string.other_information))
            Item(stringResource(id = R.string.comment), info.comment ?: "-")
            info.otherTags?.let { tags ->
                for (tag in tags) {
                    Item(tag.key, tag.value, editMode)
                }
            }
            // Lyrics
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.lyrics), color = paletteColor)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Item(tag: String = "KeyName", value: String = "KeyValue", withEditor: Boolean = false) {
    VerticalTextItem(tag, value)
    if (withEditor)
        TailTextField(hint = value) { }
}

@Preview(showBackground = true)
@Composable
internal fun PreviewContent() {
    PhonographTheme(previewMode = true) {
        DetailActivityContent(DetailModel().apply { info = SongInfo("name") })
    }
}
