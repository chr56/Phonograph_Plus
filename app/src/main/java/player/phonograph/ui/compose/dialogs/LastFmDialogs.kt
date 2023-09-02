/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.dialogs

import coil.Coil
import coil.request.ImageRequest
import player.phonograph.R
import player.phonograph.ui.compose.components.HorizontalTextItem
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
import util.phonograph.tagsources.lastfm.LastFMUtil
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmWikiData
import util.phonograph.tagsources.lastfm.Tags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import kotlinx.coroutines.launch

@Composable
fun LastFmArtist(artist: LastFmArtist) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Image(artist)
        HorizontalTextItem(stringResource(R.string.artist), artist.name)
        Wiki(artist.bio, isBio = true)
        MusicBrainzIdentifier(artist.mbid)
        Tags(artist.tags)
        Links(artist.url, artist.mbid, "artist")
    }
}

@Composable
fun LastFmAlbum(album: LastFmAlbum) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Image(album)
        HorizontalTextItem(stringResource(R.string.artist), album.name)
        HorizontalTextItem(stringResource(R.string.album), album.artist.orEmpty())
        Wiki(album.wiki, isBio = false)
        MusicBrainzIdentifier(album.mbid)
        Tags(album.tags)
        Links(album.url, album.mbid, "release")
        Tracks(album.tracks)
    }
}


@Composable
private fun ColumnScope.Links(lastFmUri: String, mbid: String?, type: String) {
    val context = LocalContext.current
    Row(Modifier.align(Alignment.End)) {
        if (!mbid.isNullOrEmpty())
            TextButton(
                onClick = {
                    clickLink(context, "https://musicbrainz.org/$type/$mbid")
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("MusicBrainz")
            }
        TextButton(
            onClick = { clickLink(context, lastFmUri) },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("Last.FM")
        }
    }
}

@Composable
private fun Wiki(wikiData: LastFmWikiData?, isBio: Boolean) {
    Box(modifier = Modifier.padding(24.dp, 12.dp)) {
        if (wikiData != null
            && (wikiData.content != null && wikiData.summary != null)
            && !wikiData.summary.startsWith(" <a href=\"https://")
        ) {
            var clicked by remember(wikiData) { mutableStateOf(false) }
            val text = Html.fromHtml(if (clicked) wikiData.content else wikiData.summary, Html.FROM_HTML_MODE_COMPACT)
            Column(Modifier.padding(8.dp, 8.dp)) {
                SelectionContainer {
                    Text(
                        text.toString(),
                        Modifier.clickable { clicked = !clicked }
                    )
                }
                Text(wikiData.published.orEmpty(), Modifier.padding(4.dp), fontSize = 10.sp)
            }
        } else {
            Text(
                stringResource(id = if (isBio) R.string.biography_unavailable else R.string.wiki_unavailable),
                Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MusicBrainzIdentifier(string: String?) {
    if (!string.isNullOrEmpty()) VerticalTextItem("MusicBrainz Identifier", string)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tags(tags: Tags?) {
    if (tags != null && tags.tag.isNotEmpty())
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 96.dp),
        ) {
            LazyHorizontalStaggeredGrid(
                StaggeredGridCells.Adaptive(32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalItemSpacing = 2.dp
            ) {
                for (tag in tags.tag) {
                    item { Tag(tag) }
                }
            }
        }
}


@Composable
private fun Tag(tag: Tags.Tag) {
    Surface(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = Color.LightGray
    ) {
        SelectionContainer {
            Text(
                text = tag.name,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
    }
}


@Composable
private fun ColumnScope.Tracks(tracks: LastFmAlbum.Tracks?) {
    if (tracks != null && !tracks.track.isNullOrEmpty() && tracks.track.isNotEmpty()) {
        Title(title = stringResource(id = R.string.songs))
        Spacer(modifier = Modifier.height(6.dp))
        for (track in tracks.track) {
            Track(track)
        }
    }
}

@Composable
private fun ColumnScope.Track(track: LastFmAlbum.Tracks.Track) {
    val context = LocalContext.current
    SelectionContainer(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = track.name,
            color = MaterialTheme.colors.primaryVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    clickLink(context, track.url)
                }
                .align(Alignment.Start)
        )
    }
}

@Composable
private fun Image(artist: LastFmArtist) {
    Image(LastFMUtil.getLargestArtistImageUrl(artist.image))
}

@Composable
private fun Image(album: LastFmAlbum) {
    Image(LastFMUtil.getLargestAlbumImageUrl(album.image))
}

@Composable
private fun Image(artistImageUrl: String?) {
    val context = LocalContext.current

    var imageBitmap: ImageBitmap? by remember(artistImageUrl) { mutableStateOf(null) }
    LaunchedEffect(artistImageUrl) {
        launch {
            if (artistImageUrl != null) {
                Coil.imageLoader(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(artistImageUrl)
                        .target {
                            imageBitmap = it.toBitmapOrNull()?.asImageBitmap()
                        }
                        .build()
                )
            }
        }
    }


    Image(imageBitmap)
}


@Composable
private fun Image(bitmap: ImageBitmap?) {

    if (bitmap != null)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = stringResource(id = R.string.pref_header_images),
                modifier = Modifier
                    .align(Alignment.Center)
                    .sizeIn(
                        maxWidth = maxWidth / 2,
                        minHeight = maxWidth.div(3)
                    )
            )
        }
}


private fun clickLink(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}