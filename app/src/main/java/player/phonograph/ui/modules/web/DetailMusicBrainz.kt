/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import player.phonograph.R
import player.phonograph.ui.compose.components.CascadeFlowRow
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.Chip
import player.phonograph.ui.compose.components.Item
import player.phonograph.util.text.bracketedIfAny
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
@Suppress("UNUSED_PARAMETER")
fun DetailMusicBrainz(
    viewModel: WebSearchViewModel,
    musicBrainzDetail: PageDetail.MusicBrainzDetail,
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
    Item(stringResource(R.string.key_date), releaseGroup.firstReleaseDate)

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
            MusicBrainzLinkableItem(Target.ReleaseGroup, release.releaseGroup.id, Modifier.padding(vertical = 4.dp)) {
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
        Item(stringResource(R.string.status), release.status)
        Item(stringResource(R.string.key_country), release.country)
        MusicBrainzMedias(release.media)
        Item(stringResource(R.string.key_barcode), release.barcode)
        if (release.labelInfo.isNotEmpty()) {
            CascadeVerticalItem(stringResource(R.string.key_record_label)) {
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
        Item(stringResource(R.string.key_barcode), release.barcode)
        Item(stringResource(R.string.key_country), release.country)
    }
}

@Composable
fun ColumnScope.MusicBrainzArtist(artist: MusicBrainzArtist) {
    EntityTitle(Target.Artist, artist.id, artist.name)
    Item(stringResource(R.string.type), artist.type)
    Item(stringResource(R.string.key_gender), artist.gender)
    MusicBrainzLifeSpan(artist.lifeSpan)
    Item(stringResource(R.string.key_country), artist.country)
    Item(stringResource(R.string.key_area), artist.area?.name)
    MusicBrainzDisambiguation(artist.disambiguation)
    MusicBrainzTags(artist.tags)
    if (artist.aliases.isNotEmpty()) {
        CascadeVerticalItem(stringResource(R.string.alias), collapsible = true, collapsed = true) {
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
        Item(stringResource(R.string.key_date), recording.firstReleaseDate)
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
        CascadeVerticalItem(stringResource(R.string.key_media)) {
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
@Suppress("UNUSED_PARAMETER")
fun MusicBrainzArtistCredit(artistCredit: MusicBrainzArtistCredit, modifier: Modifier = Modifier) {
    MusicBrainzLinkableItem(Target.Artist, artistCredit.artist?.id) {
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
        CascadeVerticalItem(stringResource(R.string.key_media)) {
            Item(stringResource(R.string.title), media.title)
            Item(stringResource(R.string.format), media.format)
            Item(stringResource(R.string.count), "${media.discCount} * ${media.trackCount}")
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

        Item(stringResource(R.string.key_lifespan), text)
    }
}

@Composable
private fun MusicBrainzTags(tags: List<MusicBrainzTag>?) {
    if (!tags.isNullOrEmpty()) {
        CascadeFlowRow(stringResource(R.string.key_tags)) {
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
private fun MusicBrainzMultipleTypes(primaryType: String, secondaryTypes: List<String>?) {
    val text = if (!secondaryTypes.isNullOrEmpty()) {
        primaryType + secondaryTypes.joinToString(", ", " (", ")")
    } else {
        primaryType
    }
    Item(stringResource(R.string.type), text)
}


@Composable
private fun MusicBrainzDisambiguation(string: String?) {
    if (!string.isNullOrEmpty()) {
        Item(stringResource(R.string.comment), string)
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun EntityTitle(
    target: Target,
    mbid: String,
    title: String,
    modifier: Modifier = Modifier,
    titleMode: Boolean = true,
) {
    MusicBrainzLinkableItem(target, mbid) {
        Text(
            stringResource(target.displayTextRes()),
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
@Suppress("UNUSED_PARAMETER")
fun LinkIconMusicbrainz(target: Target, mbid: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalPageNavigator.current
    Icon(
        Icons.AutoMirrored.Outlined.ArrowForward, stringResource(R.string.search_online),
        Modifier
            .clickable {
                jumpMusicbrainz(context, navigator, target, mbid)
            }
            .padding(8.dp)
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
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
private fun Target.icon(): Painter = when (this) {
    Target.ReleaseGroup -> painterResource(R.drawable.ic_library_music_white_24dp)
    Target.Release      -> painterResource(R.drawable.ic_album_white_24dp)
    Target.Artist       -> painterResource(R.drawable.ic_person_white_24dp)
    Target.Recording    -> painterResource(R.drawable.ic_music_note_white_24dp)
}

@Composable
fun MusicBrainzLinkableItem(
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
