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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun DetailMusicBrainz(
    viewModel: WebSearchViewModel,
    musicBrainzDetail: Page.Detail.MusicBrainzDetail,
) {
    val detail by musicBrainzDetail.detail.collectAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        when (val item = detail) {
            is MusicBrainzReleaseGroup -> MusicBrainzReleaseGroup(item)
            is MusicBrainzRelease      -> MusicBrainzRelease(item)
            is MusicBrainzArtist       -> MusicBrainzArtist(item)
            is MusicBrainzRecording    -> MusicBrainzRecording(item)
            is MusicBrainzTrack        -> MusicBrainzTrack(item)
            else                       -> Text(
                stringResource(R.string.empty),
                Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}


@Composable
fun ColumnScope.MusicBrainzReleaseGroup(release: MusicBrainzReleaseGroup) {
    Item(stringResource(R.string.title), release.title)
    MusicBrainzArtistCredits(release.artistCredit)
    Item(stringResource(R.string.year), release.firstReleaseDate)
    Item("Type", release.primaryType)
    CascadeItem("Type") {
        if (!release.secondaryTypes.isNullOrEmpty()) {
            for (secondaryType in release.secondaryTypes) {
                Text(secondaryType)
            }
        }
    }
    MusicBrainzDisambiguation(release.disambiguation)
}

@Composable
fun ColumnScope.MusicBrainzRelease(release: MusicBrainzRelease) {
    Item(stringResource(R.string.title), release.title)
    MusicBrainzArtistCredits(release.artistCredit)
    Item("Release Group", release.title)
    Item(stringResource(R.string.year), release.date)
    Item("Status", release.status)
    Item("Country", release.country)
    MusicBrainzMedias(release.media)
    Item("Barcode", release.barcode)
    CascadeItem("Type") {
        if (!release.labelInfo.isNullOrEmpty()) {
            for (labelInfo in release.labelInfo) {
                Text(labelInfo.label.name)
            }
        }
    }
    MusicBrainzTags(release.tags)
}

@Composable
fun ColumnScope.MusicBrainzArtist(artist: MusicBrainzArtist) {
    Item(stringResource(R.string.title), artist.name)
    Item("Type", artist.type)
    Item("Gender", artist.gender)
    Item("Country", artist.country)
    MusicBrainzLifeSpan(artist.lifeSpan)
    Item("Area", artist.area?.name)
    CascadeItem("Alias") {
        if (!artist.aliases.isNullOrEmpty()) {
            for (alias in artist.aliases) {
                Text("${alias.name} (${alias.locale})")
            }
        }
    }
    MusicBrainzTags(artist.tags)
}

@Composable
fun ColumnScope.MusicBrainzRecording(recording: MusicBrainzRecording) {
    Item(stringResource(R.string.title), recording.title)
    MusicBrainzArtistCredits(recording.artistCredit)
    Item("Date", recording.firstReleaseDate)
    MusicBrainzDisambiguation(recording.disambiguation)
    if (!recording.releases.isNullOrEmpty()) {
        CascadeItem("Related Releases") {
            for (release in recording.releases) {
                Text("${release.title} (${release.date}/${release.barcode})")
            }
        }
    }
}

@Composable
fun ColumnScope.MusicBrainzTrack(track: MusicBrainzTrack) {
    Item(stringResource(R.string.title), track.title)
    MusicBrainzArtistCredits(track.artistCredit)
    Item(stringResource(R.string.label_track_length), track.length.toString())
    Item(stringResource(R.string.track), track.number)
    Item("recording", track.recording.title)
}

@Composable
fun MusicBrainzArtistCredits(artistCredits: List<MusicBrainzArtistCredit>?) {
    if (!artistCredits.isNullOrEmpty()) {
        SelectionContainer {
            CascadeItem(stringResource(R.string.artists)) {
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
        CascadeItem("Media") {
            Item("Format", media.format)
            Item("Count", "${media.discCount} * ${media.trackCount}")
            if (!media.tracks.isNullOrEmpty()) {
                CascadeItem("Tracks", innerPadding = 24.dp) {
                    for (track in media.tracks) {
                        Item("Track ${track.number}", value = track.title)
                        Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                            MusicBrainzRecording(track.recording)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MusicBrainzLifeSpan(lifeSpan: MusicBrainzArtist.LifeSpan?) {
    if (lifeSpan != null) Item("LifeSpan", lifeSpan.run { "$begin~$end ${if (ended == true) "ENDED" else ""}" })
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
                    item { MusicBrainzTag(tag) }
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
private fun MusicBrainzDisambiguation(string: String?) {
    if (!string.isNullOrEmpty()) {
        Item(stringResource(R.string.comment), string)
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


@Composable
private fun CascadeItem(
    title: String, outerPadding: Dp = 8.dp, innerPadding: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.padding(outerPadding),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            title,
            style = TextStyle(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start),
        )
        Column(Modifier.padding(innerPadding)) {
            content()
        }
    }
}