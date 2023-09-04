/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.components.HorizontalTextItem
import player.phonograph.ui.compose.components.VerticalTextItem
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtistCredit
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import util.phonograph.tagsources.musicbrainz.MusicBrainzTag
import util.phonograph.tagsources.musicbrainz.MusicBrainzTrack
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun BoxScope.DetailMusicBrainz(viewModel: WebSearchViewModel, query: MusicBrainzQuery) {
    val item by query.detail.collectAsState()
    when (val i = item) {
        is MusicBrainzReleaseGroup -> MusicBrainzReleaseGroup(i)
        is MusicBrainzRelease      -> MusicBrainzRelease(i)
        is MusicBrainzArtist       -> MusicBrainzArtist(i)
        is MusicBrainzRecording    -> MusicBrainzRecording(i)
        is MusicBrainzTrack        -> MusicBrainzTrack(i)
        null                       -> Text(
            stringResource(R.string.empty), modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}


@Composable
fun MusicBrainzReleaseGroup(release: MusicBrainzReleaseGroup) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Item(stringResource(R.string.title), release.title)
        MusicBrainzArtistCredits(release.artistCredit)
        Item(stringResource(R.string.year), release.firstReleaseDate)
        Item("Type", release.primaryType)
        Item("Type", release.secondaryTypes)
        Item(stringResource(R.string.comment), release.disambiguation)
    }
}
@Composable
fun MusicBrainzRelease(release: MusicBrainzRelease) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Item(stringResource(R.string.title), release.title)
        MusicBrainzArtistCredits(release.artistCredit)
        Item("Release Group", release.title)
        Item(stringResource(R.string.year), release.date)
        Item("Status", release.status)
        Item("Country", release.country)
        MusicBrainzMedias(release.media)
        Item("Media", release.media?.map { "${it.format}(${it.discCount})" })
        Item("Barcode", release.barcode)
        Item("MarketLabel", release.labelInfo?.map { it.label.name })
        MusicBrainzTags(release.tags)
    }
}

@Composable
fun MusicBrainzArtist(artist: MusicBrainzArtist) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Item(stringResource(R.string.title), artist.name)
        Item("Type", artist.type)
        Item("Gender", artist.gender)
        Item("Country", artist.country)
        MusicBrainzLifeSpan(artist.lifeSpan)
        Item("Area", artist.area?.name)
        Item("Alias", artist.aliases?.map { it.name })
        MusicBrainzTags(artist.tags)
    }
}

@Composable
fun MusicBrainzRecording(recording: MusicBrainzRecording, embed: Boolean = false) {
    val content = @Composable {
        Item(stringResource(R.string.title), recording.title)
        MusicBrainzArtistCredits(recording.artistCredit)
        Item(stringResource(R.string.year), recording.firstReleaseDate)
        Item(stringResource(R.string.comment), recording.disambiguation)
        Item("Releases", recording.releases?.map { "${it.title} (${it.date}/${it.barcode})" })
    }
    if (embed) {
        content()
    } else
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
}

@Composable
fun MusicBrainzTrack(track: MusicBrainzTrack) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Item(stringResource(R.string.title), track.title)
        MusicBrainzArtistCredits(track.artistCredit)
        Item(stringResource(R.string.label_track_length), track.length.toString())
        Item(stringResource(R.string.track), track.number)
        Item("recording", track.recording.title)
    }
}

@Composable
fun MusicBrainzArtistCredits(artistCredits: List<MusicBrainzArtistCredit>?) {
    if (!artistCredits.isNullOrEmpty()) {
        Column(
            Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                stringResource(R.string.artists),
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Start),
            )
            SelectionContainer {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    val stylePrimary = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                    val styleSecondary = TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start
                    )
                    for (artistCredit in artistCredits) {
                        Text(
                            "${artistCredit.joinphrase} ${artistCredit.name}",
                            style = stylePrimary,
                        )
                        Text(
                            "* ${artistCredit.artist.name} (${artistCredit.artist.type})",
                            style = styleSecondary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicBrainzMedias(medias: List<MusicBrainzRelease.Media>?) {
    if (!medias.isNullOrEmpty()) {
        for (media in medias) {
            MusicBrainzMedia(media)
        }
    }
}

@Composable
private fun MusicBrainzMedia(media: MusicBrainzRelease.Media) {
    SelectionContainer {
        Column(
            Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "Media",
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Start),
            )
            Column(Modifier.padding(horizontal = 4.dp)) {
                Item("format", media.format)
                Item("count", "${media.discCount} * ${media.trackCount}")
                if (!media.tracks.isNullOrEmpty()) {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            "Tracks",
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.Start),
                        )
                        Column(Modifier.padding(horizontal = 4.dp)) {
                            for (track in media.tracks) {
                                Item("Track", value = track.title)
                                Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                                    MusicBrainzRecording(track.recording, embed = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MusicBrainzLifeSpan(lifeSpan: MusicBrainzArtist.LifeSpan?) {
    if (lifeSpan != null) Item("LifeSpan", lifeSpan.run { "$begin~$end ${if (ended) "ENDED" else ""}" })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicBrainzTags(tags: List<MusicBrainzTag>?) {
    if (!tags.isNullOrEmpty()) {
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
                for (tag in tags) {
                    item{ MusicBrainzTag(tag) }
                }
            }
        }
    }
}

@Composable
private fun MusicBrainzTag(tag: MusicBrainzTag) {
    Surface(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = Color.LightGray
    ) {
        Row(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
            SelectionContainer {
                Text(
                    text = tag.name,
                    modifier = Modifier,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun Item(label: String, value: String?) {
    if (value != null) HorizontalTextItem(label, value)
}

@Composable
private fun Item(label: String, values: Collection<String>?) {
    if (!values.isNullOrEmpty()) {
        val value = values.joinToString(separator = "\n") { it }
        VerticalTextItem(label, value)
    }
}
