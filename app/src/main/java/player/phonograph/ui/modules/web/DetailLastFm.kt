/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import coil.Coil
import coil.compose.rememberAsyncImagePainter
import player.phonograph.R
import player.phonograph.coil.lastfm.LastFmImageBundle
import player.phonograph.ui.compose.components.Chip
import player.phonograph.ui.compose.components.HorizontalTextItem
import player.phonograph.ui.compose.components.VerticalTextItem
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmTrack
import util.phonograph.tagsources.lastfm.LastFmWikiData
import util.phonograph.tagsources.lastfm.Tags
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.text.Html



@Composable
fun DetailLastFm(viewModel: WebSearchViewModel, lastFmDetail: PageDetail.LastFmDetail) {
    val detail by lastFmDetail.detail.collectAsState()
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        when (val item = detail) {
            is LastFmAlbum -> LastFmAlbum(item)
            is LastFmArtist -> LastFmArtist(item)
            is LastFmTrack -> LastFmTrack(item)
            else -> Text(
                stringResource(R.string.empty), modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun LastFmArtist(artist: LastFmArtist) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Image(LastFmImageBundle.from(artist))
        HorizontalTextItem(stringResource(R.string.artist), artist.name)
        Wiki(artist.bio, isBio = true)
        MusicBrainzIdentifier(artist.mbid)
        Tags(artist.tags)
        Links(artist.url, artist.mbid, MusicBrainzAction.Target.Artist)
    }
}

@Composable
fun LastFmAlbum(album: LastFmAlbum) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Image(LastFmImageBundle.from(album))
        HorizontalTextItem(stringResource(R.string.artist), album.name)
        HorizontalTextItem(stringResource(R.string.album), album.artist.orEmpty())
        Wiki(album.wiki, isBio = false)
        MusicBrainzIdentifier(album.mbid)
        Tags(album.tags)
        Links(album.url, album.mbid, MusicBrainzAction.Target.Release)
        Tracks(album.tracks)
    }
}

@Composable
fun LastFmTrack(track: LastFmTrack) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        HorizontalTextItem(stringResource(R.string.title), track.name)
        HorizontalTextItem(stringResource(R.string.artist), track.artist?.name.orEmpty())
        HorizontalTextItem(stringResource(R.string.album), track.album?.name.orEmpty())
        Wiki(track.wiki, isBio = false)
        MusicBrainzIdentifier(track.mbid)
        Tags(track.toptags)
        Links(track.url, track.mbid, MusicBrainzAction.Target.Recording)
    }
}


@Composable
private fun ColumnScope.Links(lastFmUri: String, mbid: String?, type: MusicBrainzAction.Target) {
    Row(Modifier.align(Alignment.End)) {
        JumpMusicBrainz(Modifier.align(Alignment.CenterVertically), type, mbid)
        LinkMusicBrainz(Modifier.align(Alignment.CenterVertically), type, mbid)
        LinkLastFm(Modifier.align(Alignment.CenterVertically), lastFmUri)
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
    if (!string.isNullOrEmpty()) VerticalTextItem(stringResource(R.string.key_mbid), string)
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
            val context = LocalContext.current
            LazyHorizontalStaggeredGrid(
                StaggeredGridCells.Adaptive(32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalItemSpacing = 2.dp
            ) {
                for (tag in tags.tag) {
                    item { Tag(tag, context) }
                }
            }
        }
}


@Composable
private fun Tag(tag: Tags.Tag, context: Context) {
    Chip {
        Row(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
            SelectionContainer {
                Text(
                    text = tag.name,
                    modifier = Modifier,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(id = R.string.website),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable {
                        if (tag.url != null) clickLink(context, tag.url)
                    }
            )
        }
    }
}


@Composable
private fun ColumnScope.Tracks(tracks: LastFmAlbum.Tracks?) {
    if (tracks != null && !tracks.track.isNullOrEmpty() && tracks.track.isNotEmpty()) {
        Text(stringResource(id = R.string.songs), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        for (track in tracks.track) {
            Track(track)
        }
    }
}

@Composable
private fun ColumnScope.Track(track: LastFmAlbum.Tracks.Track) {
    val context = LocalContext.current
    Row(
        Modifier
            .align(Alignment.Start)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {

        Box(Modifier.weight(8f)) {
            SelectionContainer {
                Text(
                    text = track.name,
                    modifier = Modifier.wrapContentWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(id = R.string.website),
            modifier = Modifier
                .weight(2f)
                .clickable {
                    clickLink(context, track.url)
                }
        )
    }
}

@Composable
private fun Image(lastFmImageBundle: LastFmImageBundle) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(lastFmImageBundle, Coil.imageLoader(context))
    Image(painter)
}


@Composable
private fun Image(painter: Painter?) {

    if (painter != null)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(128.dp)
        ) {
            Image(
                painter, stringResource(id = R.string.pref_header_images), Modifier
                    .align(Alignment.Center)
                    .size(
                        width = maxWidth / 3 * 2,
                        height = maxWidth / 3 * 2
                    )
            )
        }
}
