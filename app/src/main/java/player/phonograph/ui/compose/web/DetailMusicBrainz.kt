/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.components.Chip
import player.phonograph.ui.compose.components.LabeledItemLayout
import player.phonograph.ui.compose.components.LabeledItemLayoutDefault
import player.phonograph.ui.compose.web.MusicBrainzQuery.Target
import player.phonograph.util.text.bracketedIfAny
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtistCredit
import util.phonograph.tagsources.musicbrainz.MusicBrainzGenre
import util.phonograph.tagsources.musicbrainz.MusicBrainzMedia
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import util.phonograph.tagsources.musicbrainz.MusicBrainzTag
import util.phonograph.tagsources.musicbrainz.MusicBrainzTrack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowForward
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        when (val item = detail) {
            is MusicBrainzReleaseGroup -> MusicBrainzReleaseGroup(item, false)
            is MusicBrainzRelease      -> MusicBrainzRelease(item, false)
            is MusicBrainzArtist       -> MusicBrainzArtist(item)
            is MusicBrainzRecording    -> MusicBrainzRecording(item)
            is MusicBrainzTrack        -> MusicBrainzTrack(item)
            else                       -> Text(
                stringResource(R.string.empty),
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            )
        }
    }
}


@Composable
fun ColumnScope.MusicBrainzReleaseGroup(releaseGroup: MusicBrainzReleaseGroup, embed: Boolean) {
    EntityTitle(Target.ReleaseGroup, releaseGroup.id, releaseGroup.title)
    MusicBrainzArtistCredits(releaseGroup.artistCredit)
    Item("Date", releaseGroup.firstReleaseDate)

    if (!embed) {
        MusicBrainzMultipleTypes(releaseGroup.primaryType, releaseGroup.secondaryTypes)
        MusicBrainzDisambiguation(releaseGroup.disambiguation)
        MusicBrainzGenres(releaseGroup.genres)
        MusicBrainzTags(releaseGroup.tags)
        if (!releaseGroup.releases.isNullOrEmpty()) {
            CascadeVerticalItem("Releases") {
                for (release in releaseGroup.releases) {
                    EntityTitle(Target.Release, release.id, release.title)
                }
            }
        }
    } else {
        MusicBrainzDisambiguation(releaseGroup.disambiguation)
        MusicBrainzMultipleTypes(releaseGroup.primaryType, releaseGroup.secondaryTypes)
    }
}

@Composable
fun ColumnScope.MusicBrainzRelease(release: MusicBrainzRelease, embed: Boolean) {
    EntityTitle(Target.Release, release.id, release.title)
    MusicBrainzArtistCredits(release.artistCredit)
    if (release.releaseGroup != null) {
        CascadeVerticalItem("Release Group") {
            LinkableItem(Target.ReleaseGroup, release.releaseGroup.id, Modifier.padding(vertical = 4.dp)) {
                Text(
                    release.releaseGroup.title,
                    Modifier,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start
                )
                Text(release.releaseGroup.id, fontSize = 8.sp, fontWeight = FontWeight.Thin)
            }
        }
    }
    Item(stringResource(R.string.year), release.date)
    if (!embed) {
        Item("Status", release.status)
        Item("Country", release.country)
        MusicBrainzMedias(release.media)
        Item("Barcode", release.barcode)
        if (release.labelInfo.isNotEmpty()) {
            CascadeVerticalItem("Label") {
                for (labelInfo in release.labelInfo) {
                    if (labelInfo.label != null) {
                        Text(labelInfo.label.name)
                    }
                }
            }
        }
        MusicBrainzGenres(release.genres)
        MusicBrainzTags(release.tags)
    } else {
        Item("Barcode", release.barcode)
        Item("Country", release.country)
    }
}

@Composable
fun ColumnScope.MusicBrainzArtist(artist: MusicBrainzArtist) {
    EntityTitle(Target.Artist, artist.id, artist.name)
    Item("Type", artist.type)
    Item("Gender", artist.gender)
    MusicBrainzLifeSpan(artist.lifeSpan)
    Item("Country", artist.country)
    Item("Area", artist.area?.name)
    MusicBrainzDisambiguation(artist.disambiguation)
    MusicBrainzTags(artist.tags)
    if (artist.aliases.isNotEmpty()) {
        CascadeVerticalItem("Alias", collapsible = true, collapsed = true) {
            for (alias in artist.aliases) {
                Text("${alias.name} (${alias.locale})")
            }
        }
    }
    if (artist.releaseGroups.isNotEmpty()) {
        CascadeVerticalItem("Release Groups", collapsible = true, collapsed = true) {
            for (releaseGroup in artist.releaseGroups) {
                MusicBrainzReleaseGroup(releaseGroup, embed = true)
            }
        }
    }
    if (artist.releases.isNotEmpty()) {
        CascadeVerticalItem("Releases", collapsible = true, collapsed = true) {
            for (release in artist.releases) {
                MusicBrainzRelease(release, embed = true)
            }
        }
    }
    Item("IPI Code", artist.ipis)
    Item("ISNI Code", artist.isnis)
}

@Composable
fun ColumnScope.MusicBrainzRecording(recording: MusicBrainzRecording?) {
    if (recording != null) {
        EntityTitle(Target.Recording, recording.id, recording.title)
        MusicBrainzArtistCredits(recording.artistCredit)
        Item("Date", recording.firstReleaseDate)
        MusicBrainzDisambiguation(recording.disambiguation)
        MusicBrainzGenres(recording.genres)
        MusicBrainzTags(recording.tags)
        if (!recording.releases.isNullOrEmpty()) {
            CascadeVerticalItem("Related Releases") {
                for ((index, release) in recording.releases.withIndex()) {
                    CascadeVerticalItem("Related Release ${index + 1}") {
                        MusicBrainzRelease(release, embed = true)
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.MusicBrainzTrack(track: MusicBrainzTrack) {
    Item("Track", track.title)
    MusicBrainzArtistCredits(track.artistCredit)
    Item(stringResource(R.string.label_track_length), track.length.toString())
    Item(stringResource(R.string.track), track.number)
    if (track.recording != null) {
        CascadeVerticalItem("Recording") {
            MusicBrainzRecording(track.recording)
        }
    }
    if (track.media != null) {
        CascadeVerticalItem("Media") {
            MusicBrainzMedia(track.media)
        }
    }
}

@Composable
fun MusicBrainzArtistCredits(artistCredits: List<MusicBrainzArtistCredit>?) {
    if (!artistCredits.isNullOrEmpty()) {
        SelectionContainer {
            CascadeVerticalItem(stringResource(R.string.artists)) {
                for (artistCredit in artistCredits.asReversed()) {
                    MusicBrainzArtistCredit(artistCredit, Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun MusicBrainzArtistCredit(artistCredit: MusicBrainzArtistCredit, modifier: Modifier = Modifier) {
    LinkableItem(Target.Artist, artistCredit.artist?.id) {
        Text(
            artistCredit.name,
            Modifier,
            fontSize = 14.sp,
            textAlign = TextAlign.Start
        )
        if (artistCredit.artist != null) {
            Text(
                artistCredit.artist.name,
                Modifier,
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
            Text(
                artistCredit.artist.id,
                fontSize = 8.sp,
                fontWeight = FontWeight.Thin
            )
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
        CascadeVerticalItem("Media") {
            Item(stringResource(R.string.title), media.title)
            Item("Format", media.format)
            Item("Count", "${media.discCount} * ${media.trackCount}")
            if (!media.tracks.isNullOrEmpty()) {
                CascadeVerticalItem("Tracks", collapsible = true, collapsed = true) {
                    for (track in media.tracks) {
                        CascadeVerticalItem(
                            "Track ${track.number}: ${track.title}",
                            collapsible = true,
                            collapsed = true
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

@Composable
private fun MusicBrainzTags(tags: List<MusicBrainzTag>?) {
    if (!tags.isNullOrEmpty()) {
        CascadeFlowRow("Tags") {
            for (tag in tags) {
                Chip(tag.name)
            }
        }
    }
}

@Composable
private fun MusicBrainzGenres(genres: List<MusicBrainzGenre>?) {
    if (!genres.isNullOrEmpty()) {
        CascadeFlowRow(stringResource(R.string.genres)) {
            for (genre in genres) {
                Chip("${genre.name} ${genre.disambiguation.bracketedIfAny()}")
            }
        }
    }
}
@Composable
fun MusicBrainzMultipleTypes(primaryType: String, secondaryTypes: List<String>?) {
    val text = if (!secondaryTypes.isNullOrEmpty()) {
        primaryType + secondaryTypes.joinToString(", ", " (", ")")
    } else {
        primaryType
    }
    Item("Type", text)
}


@Composable
private fun MusicBrainzDisambiguation(string: String?) {
    if (!string.isNullOrEmpty()) {
        Item(stringResource(R.string.comment), string)
    }
}

@Composable
private fun EntityTitle(
    target: Target,
    mbid: String,
    title: String,
    modifier: Modifier = Modifier,
    titleMode: Boolean = true,
) {
    LinkableItem(target, mbid) {
        Text(
            target.displayName,
            Modifier,
            fontSize = if (titleMode) 17.sp else 14.sp,
            fontWeight = if (titleMode) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Start
        )
        Text(
            title,
            Modifier,
            fontSize = if (titleMode) 17.sp else 14.sp,
            textAlign = TextAlign.Start
        )
        Text(mbid, fontSize = 8.sp, fontWeight = FontWeight.Thin)
    }
}

@Composable
private fun LinkableItem(
    target: Target,
    mbid: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            target.icon(), null,
            Modifier.weight(1f)
        )
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .weight(7f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            content()
        }
        if (mbid != null) {
            LinkIconMusicbrainz(
                target, mbid,
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            LinkIconMusicbrainzWebsite(
                target, mbid,
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun LinkIconMusicbrainz(target: Target, mbid: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalPageNavigator.current
    Icon(
        Icons.Outlined.ArrowForward, stringResource(R.string.web_search),
        Modifier
            .clickable {
                jumpMusicbrainz(context, navigator, target, mbid)
            }
            .padding(8.dp)
    )
}

@Composable
fun LinkIconMusicbrainzWebsite(target: Target, mbid: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Icon(
        painterResource(R.drawable.ic_open_in_browser_white_24dp), stringResource(R.string.website),
        Modifier
            .clickable {
                clickLink(context, target.link(mbid))
            }
            .padding(8.dp)
    )
}

@Composable
private fun Item(label: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        Item(label) {
            ValueText(value)
        }
    }
}

@Composable
private fun Item(label: String, values: Collection<String>?) {
    if (!values.isNullOrEmpty()) {
        Item(label) {
            Column {
                for (value in values) {
                    ValueText(value)
                }
            }
        }
    }
}

@Composable
private fun Item(label: String, content: @Composable () -> Unit) {
    LabeledItemLayout(
        Modifier.padding(vertical = 4.dp),
        label = label,
        labelModifier = Modifier.padding(end = 12.dp)
    ) {
        SelectionContainer {
            content()
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
        modifier = Modifier.wrapContentSize()
    )
}


@Composable
private fun CascadeVerticalItem(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerColumnModifier: Modifier = Modifier,
    collapsible: Boolean = true,
    collapsed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    CascadeItem(modifier.padding(vertical = 8.dp), title, textStyle, Modifier, collapsible, collapsed) {
        Column(innerColumnModifier.padding(start = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun CascadeHorizontalItem(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerRowModifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CascadeItem(modifier.padding(vertical = 4.dp), title, textStyle, Modifier, collapsible = false, collapsed = false) {
        Row(
            innerRowModifier
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}

@Composable
private fun CascadeItem(
    modifier: Modifier,
    title: String,
    textStyle: TextStyle,
    textModifier: Modifier,
    collapsible: Boolean,
    collapsed: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier, verticalArrangement = Arrangement.SpaceEvenly
    ) {
        var collapseState by remember { mutableStateOf(collapsed) }
        Row(
            Modifier
                .clickable {
                    collapseState = !collapseState
                }
        ) {
            if (collapsible) {
                Icon(
                    if (collapseState) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                    "Collapse",
                    Modifier.align(Alignment.CenterVertically)
                )
            }
            Text(
                title,
                modifier = textModifier
                    .padding(vertical = 2.dp)
                    .align(Alignment.Top)
                    .fillMaxWidth(),
                style = textStyle,
            )
        }
        if (!collapsible || !collapseState) content() else Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CascadeFlowRow(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LabeledItemLayoutDefault.titleStyle,
    innerRowModifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CascadeItem(
        modifier = modifier,
        title = title,
        textStyle = textStyle,
        textModifier = innerRowModifier,
        collapsible = true,
        collapsed = false
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun Target.icon(): Painter {
    return when (this) {
        Target.ReleaseGroup -> painterResource(R.drawable.ic_library_music_white_24dp)
        Target.Release      -> painterResource(R.drawable.ic_album_white_24dp)
        Target.Artist       -> painterResource(R.drawable.ic_person_white_24dp)
        Target.Recording    -> painterResource(R.drawable.ic_music_note_white_24dp)
    }
}