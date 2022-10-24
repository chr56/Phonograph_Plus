/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.ui.compose.ColorTools.makeSureContrastWith
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.SongDetailUtil.SongInfo
import player.phonograph.util.SongDetailUtil.getFileSizeString
import player.phonograph.util.SongDetailUtil.loadArtwork
import player.phonograph.util.SongDetailUtil.loadSong
import android.content.Context
import android.content.Intent

class DetailActivity : ComposeToolbarActivity() {
    val model: DetailModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.song = parseIntent(this, intent)
        with(model) {
            info = loadSong(song)
            artwork = loadArtwork(this@DetailActivity, song = song) {
                updateBarsColor()
                model.isDefaultArtwork.value = false
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
        model.artwork.value?.paletteColor?.let { color ->
            if (color != 0) {
                val colorInt = darkenColor(color)
                appbarColor.value = Color(colorInt)
                window.statusBarColor = darkenColor(colorInt)
                if (ThemeColor.coloredNavigationBar(this)) {
                    window.navigationBarColor = darkenColor(colorInt)
                }
            }
        }
    }


    companion object {
        private fun parseIntent(context: Context, intent: Intent): Song =
            SongLoader.getSong(context, intent.extras?.getLong(SONG_ID) ?: -1)

        const val SONG_ID = "SONG_ID"

        fun launch(context: Context, songId: Long) {
            context.startActivity(
                Intent(context.applicationContext, DetailActivity::class.java).apply {
                    putExtra(SONG_ID, songId)
                }
            )
        }
    }
}

class DetailModel : ViewModel() {
    lateinit var song: Song
    lateinit var info: SongInfo
    var artwork: MutableState<SongDetailUtil.BitmapPaletteWrapper?> = mutableStateOf(null)
    var isDefaultArtwork = mutableStateOf(true)
}

@Composable
internal fun DetailActivityContent(viewModel: DetailModel) {
    val info by remember { mutableStateOf(viewModel.info) }
    val wrapper by remember { viewModel.artwork }
    val isDefaultArtwork by remember { viewModel.isDefaultArtwork }
    val paletteColor =
        makeSureContrastWith(MaterialTheme.colors.surface) {
            if (wrapper != null) {
                Color(wrapper!!.paletteColor)
            } else {
                MaterialTheme.colors.primaryVariant
            }
        }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        CoverImage(bitmap = wrapper!!.bitmap,
                   backgroundColor = paletteColor,
                   showCover = !isDefaultArtwork)
        InfoTable(info, paletteColor)
    }
}


@Composable
internal fun CoverImage(bitmap: Bitmap, backgroundColor: Color, showCover: Boolean) {
    if (showCover) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(backgroundColor)
        ) {
            // Cover Artwork
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = "Cover",
                modifier = Modifier
                    .align(Alignment.Center)
                    .sizeIn(
                        maxWidth = maxWidth,
                        maxHeight = maxWidth,
                        minHeight = maxWidth.div(3)
                    )
            )
        }
    }
}


/**
 * Text infomation
 */
@Composable
internal fun InfoTable(info: SongInfo, titleColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // File info
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.file), color = titleColor)
        TagItem(stringResource(id = R.string.label_file_name), info.fileName)
        TagItem(stringResource(id = R.string.label_file_path), info.filePath)
        TagItem(stringResource(id = R.string.label_track_length),
                getReadableDurationString(info.trackLength ?: -1))
        TagItem(stringResource(id = R.string.label_file_format), info.fileFormat)
        TagItem(stringResource(id = R.string.label_file_size),
                getFileSizeString(info.fileSize ?: -1))
        TagItem(stringResource(id = R.string.label_bit_rate), info.bitRate)
        TagItem(stringResource(id = R.string.label_sampling_rate), info.samplingRate)
        // Common Tag
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)
        TagItem(stringResource(id = R.string.title), info.title)
        TagItem(stringResource(id = R.string.artist), info.artist)
        TagItem(stringResource(id = R.string.album), info.album)
        TagItem(stringResource(id = R.string.album_artist), info.albumArtist, true)
        TagItem(stringResource(id = R.string.composer), info.composer, true)
        TagItem(stringResource(id = R.string.lyricist), info.lyricist, true)
        TagItem(stringResource(id = R.string.year), info.year)
        TagItem(stringResource(id = R.string.genre), info.genre)
        TagItem(stringResource(id = R.string.track), info.track, true)
        // Other Tag (if available)
        if (info.otherTags != null && info.comment != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Title(stringResource(R.string.other_information))
            TagItem(stringResource(id = R.string.comment), info.comment, true)
            info.otherTags?.let { tags ->
                for (tag in tags) {
                    Item(tag.key, tag.value)
                }
            }
        }
        // Lyrics
        // Spacer(modifier = Modifier.height(16.dp))
        // Title(stringResource(R.string.lyrics), color = color)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun TagItem(tag: String, value: String?, hideIfEmpty: Boolean = false) {
    if (hideIfEmpty) {
        if (!value.isNullOrEmpty()) {
            Item(tag, value)
        }
    } else {
        Item(tag, value ?: "-")
    }
}

@Composable
internal fun Item(tag: String = "KeyName", value: String = "KeyValue") {
    VerticalTextItem(tag, value)
}