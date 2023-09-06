/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.components.LabeledItemLayout
import player.phonograph.ui.compose.components.LabeledItemLayoutDefault
import player.phonograph.ui.compose.web.MusicBrainzQuery.Target
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtistCredit
import util.phonograph.tagsources.musicbrainz.MusicBrainzGenre
import util.phonograph.tagsources.musicbrainz.MusicBrainzMedia
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import util.phonograph.tagsources.musicbrainz.MusicBrainzTag
import util.phonograph.tagsources.musicbrainz.MusicBrainzTrack
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun ColumnScope.MusicBrainzReleaseGroup(releaseGroup: MusicBrainzReleaseGroup) {
    Item("Release Group", releaseGroup.title)
    MusicBrainzArtistCredits(releaseGroup.artistCredit)
    Item(stringResource(R.string.year), releaseGroup.firstReleaseDate)
    Item("Type", releaseGroup.primaryType)
    if (releaseGroup.secondaryTypes.isNotEmpty()) {
        CascadeItem("Type") {
            for (secondaryType in releaseGroup.secondaryTypes) {
                Text(secondaryType)
            }
        }
    }
    MusicBrainzDisambiguation(releaseGroup.disambiguation)
    MusicBrainzTags(releaseGroup.tags)
    if (!releaseGroup.releases.isNullOrEmpty()) {
        CascadeItem("Release", innerModifier = Modifier.padding(24.dp)) {
            for ((index, release) in releaseGroup.releases.withIndex()) {
                Item("Release ${index + 1}", value = release.title)
                JumpAndLinkMusicBrainz(Modifier.align(Alignment.End), Target.Release, release.id)
            }
        }
    }
    JumpAndLinkMusicBrainz(Modifier.align(Alignment.End), Target.ReleaseGroup, releaseGroup.id)
}

@Composable
fun ColumnScope.MusicBrainzRelease(release: MusicBrainzRelease) {
    Item("Release", release.title)
    MusicBrainzArtistCredits(release.artistCredit)
    if (release.releaseGroup != null) {
        CascadeItem("Release Group", innerModifier = Modifier.padding(8.dp)) {
            MusicBrainzReleaseGroup(release.releaseGroup)
        }
    }
    Item(stringResource(R.string.year), release.date)
    Item("Status", release.status)
    Item("Country", release.country)
    MusicBrainzMedias(release.media)
    Item("Barcode", release.barcode)
    if (release.labelInfo.isNotEmpty()) {
        CascadeItem("Label") {
            for (labelInfo in release.labelInfo) {
                if (labelInfo.label != null) {
                    Text(labelInfo.label.name)
                }
            }
        }
    }
    MusicBrainzTags(release.tags)
    JumpAndLinkMusicBrainz(Modifier.align(Alignment.End), Target.Release, release.id)
}

@Composable
fun ColumnScope.MusicBrainzArtist(artist: MusicBrainzArtist) {
    Item(stringResource(R.string.artist), artist.name)
    Item("Type", artist.type)
    Item("Gender", artist.gender)
    MusicBrainzLifeSpan(artist.lifeSpan)
    Item("Country", artist.country)
    Item("Area", artist.area?.name)
    MusicBrainzDisambiguation(artist.disambiguation)
    MusicBrainzTags(artist.tags)
    if (artist.aliases.isNotEmpty()) {
        CascadeItem("Alias") {
            for (alias in artist.aliases) {
                Text("${alias.name} (${alias.locale})")
            }
        }
    }
    if (artist.releaseGroups.isNotEmpty()) {
        CascadeItem("Release Group", innerModifier = Modifier.padding(8.dp)) {
            for (releaseGroup in artist.releaseGroups) {
                MusicBrainzReleaseGroup(releaseGroup)
            }
        }
    }
    if (artist.releases.isNotEmpty()) {
        CascadeItem("Release", innerModifier = Modifier.padding(8.dp)) {
            for (release in artist.releases) {
                MusicBrainzRelease(release)
            }
        }
    }
    Item("IPI Code", artist.ipis)
    Item("ISNI Code", artist.isnis)
    JumpAndLinkMusicBrainz(Modifier.align(Alignment.End), Target.Artist, artist.id)
}

@Composable
fun ColumnScope.MusicBrainzRecording(recording: MusicBrainzRecording?) {
    if (recording != null) {
        Item("Recording", recording.title)
        MusicBrainzArtistCredits(recording.artistCredit)
        Item("Date", recording.firstReleaseDate)
        MusicBrainzDisambiguation(recording.disambiguation)
        if (!recording.releases.isNullOrEmpty()) {
            CascadeItem("Related Releases") {
                for ((index, release) in recording.releases.withIndex()) {
                    CascadeItem(
                        "Related Release ${index + 1}",
                        innerModifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .fillMaxWidth()
                    ) {
                        MusicBrainzRelease(release)
                    }
                }
            }
        }
        JumpAndLinkMusicBrainz(Modifier.align(Alignment.End), Target.Recording, recording.id)
    }
}

@Composable
fun ColumnScope.MusicBrainzTrack(track: MusicBrainzTrack) {
    Item("Track", track.title)
    MusicBrainzArtistCredits(track.artistCredit)
    Item(stringResource(R.string.label_track_length), track.length.toString())
    Item(stringResource(R.string.track), track.number)
    if (track.recording != null) {
        CascadeItem("Recording", innerModifier = Modifier.padding(8.dp)) {
            MusicBrainzRecording(track.recording)
        }
    }
    if (track.media != null) {
        CascadeItem("Media", innerModifier = Modifier.padding(8.dp)) {
            MusicBrainzMedia(track.media)
        }
    }
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
                val context = LocalContext.current
                val navigator = LocalPageNavigator.current
                for (artistCredit in artistCredits.asReversed()) {
                    Text(
                        "${artistCredit.joinphrase} ${artistCredit.name}",
                        style = stylePrimary,
                    )
                    with(artistCredit.artist) {
                        Text(
                            "* $name ${type?.let { "($it)" }.orEmpty()}",
                            style = styleSecondary,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clickable {
                                    jumpMusicbrainz(context, navigator, Target.Artist, id)
                                }
                        )
                        if (area != null) {
                            Text(
                                "* ${area.name} ${country.orEmpty()}",
                                style = styleSecondary,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicBrainzMedias(medias: List<MusicBrainzMedia>?) {
    if (!medias.isNullOrEmpty()) {
        for (media in medias) {
            MusicBrainzMedia(media)
        }
    }
}

@Composable
private fun MusicBrainzMedia(media: MusicBrainzMedia) {
    SelectionContainer {
        CascadeItem("Media") {
            Item(stringResource(R.string.title), media.title)
            Item("Format", media.format)
            Item("Count", "${media.discCount} * ${media.trackCount}")
            if (!media.tracks.isNullOrEmpty()) {
                CascadeItem("Tracks", innerModifier = Modifier.padding(24.dp)) {
                    for (track in media.tracks) {
                        Item("Track ${track.number}", value = track.title)
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .fillMaxWidth()
                        ) {
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
    if (lifeSpan != null) {
        val begin = lifeSpan.begin ?: "?"
        val end = lifeSpan.end
        val text =
            if (!end.isNullOrEmpty()) {
                if (lifeSpan.ended == true) {
                    "$begin ~ $end (ended)"
                } else {
                    "$begin ~ $end"
                }
            } else {
                "$begin ~ "
            }

        Item("LifeSpan", text)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicBrainzTags(tags: List<MusicBrainzTag>?) {
    if (!tags.isNullOrEmpty()) {
        Box(
            Modifier
                .padding(16.dp)
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
private fun MusicBrainzGenres(genres: List<MusicBrainzGenre>?) {
    if (!genres.isNullOrEmpty()) {
        CascadeItem("Genres") {
            Column {
                for (genre in genres) {
                    val text = genre.name + if (!genre.disambiguation.isNullOrEmpty()) "($genre.disambiguation)" else ""
                    Text(text)
                }
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
    if (!value.isNullOrEmpty()) {
        LabeledItemLayout(Modifier.padding(horizontal = 8.dp), label) {
            SelectionContainer {
                ValueText(value)
            }
        }
    }
}

@Composable
private fun Item(label: String, values: Collection<String>?) {
    if (!values.isNullOrEmpty()) {
        LabeledItemLayout(Modifier.padding(horizontal = 8.dp), label) {
            SelectionContainer {
                Column {
                    for (value in values) {
                        ValueText(value)
                    }
                }
            }
        }
    }
}

@Composable
private fun ValueText(value: String) {
    Text(
        text = value,
        style = TextStyle(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
            fontSize = 14.sp,
        ),
        modifier = Modifier
            .wrapContentSize()
            .padding(10.dp)
    )
}


@Composable
private fun CascadeItem(
    title: String,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier.padding(horizontal = 8.dp),
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerModifier: Modifier = Modifier.padding(8.dp),
    content: @Composable () -> Unit,
) {
    Column(
        modifier, verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            title,
            style = textStyle,
            modifier = textModifier
                .align(Alignment.Start)
                .padding(8.dp),
        )
        Column(innerModifier.padding(horizontal = 8.dp)) {
            content()
        }
    }
}