/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import coil.Coil
import coil.compose.rememberAsyncImagePainter
import player.phonograph.R
import player.phonograph.ui.compose.components.Item
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.TrackResult
import util.phonograph.tagsources.lastfm.largestUrl
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun LastFmSearchResult(result: LastFmSearchResults?, onSelectItem: (Any) -> Unit = {}, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        if (result != null) {
            val total = result.totalResults.toLongOrNull()
            if (total != null && total > 0) {
                AlbumResult(result.albums) { onSelectItem(it) }
                ArtistResult(result.artists) { onSelectItem(it) }
                TrackResult(result.tracks) { onSelectItem(it) }
            }
        } else {
            Text(
                stringResource(R.string.no_results),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun AlbumResult(albumResult: AlbumResult?, getDetail: (AlbumResult.Album) -> Unit) {
    if (albumResult != null && !albumResult.album.isNullOrEmpty()) {
        val context = LocalContext.current
        LazyColumn {
            items(albumResult.album) { album ->
                val url = album.image.largestUrl()
                val painter = rememberAsyncImagePainter(url, Coil.imageLoader(context))
                Item(Modifier, album.name, album.artist, { getDetail(album) }, {}, painter)
            }
        }
    }
}

@Composable
private fun ArtistResult(artistResult: ArtistResult?, getDetail: (ArtistResult.Artist) -> Unit) {
    if (artistResult != null && !artistResult.artist.isNullOrEmpty()) {
        val context = LocalContext.current
        LazyColumn {
            items(artistResult.artist) { artist ->
                val url = artist.image.largestUrl()
                val painter = rememberAsyncImagePainter(url, Coil.imageLoader(context))
                Item(Modifier, artist.name, artist.mbid.orEmpty(), { getDetail(artist) }, {}, painter)
            }
        }
    }
}
@Composable
private fun TrackResult(trackResult: TrackResult?, getDetail: (TrackResult.Track) -> Unit) {
    if (trackResult != null && !trackResult.track.isNullOrEmpty()) {
        LazyColumn {
            items(trackResult.track) { track ->
                Item(Modifier, track.name, track.artist, { getDetail(track) }, {})
            }
        }
    }
}
