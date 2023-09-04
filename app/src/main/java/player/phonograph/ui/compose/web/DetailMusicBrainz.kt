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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.DetailMusicBrainz(viewModel: WebSearchViewModel) {

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
        Item(stringResource(R.string.year), release.date)
        Item("Status", release.status)
        Item("Country", release.country)
        Item("Media", release.media?.map { "${it.format}(${it.discCount})" })
        Item("Barcode", release.barcode)
        Item("MarketLabel", release.labelInfo?.map { "${it.label.name}(${it.catalogNumber})" })
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
        Item("Tags", artist.tags?.map { it.name })
    }
}

@Composable
fun MusicBrainzRecording(recording: MusicBrainzRecording) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Item(stringResource(R.string.title), recording.title)
        MusicBrainzArtistCredits(recording.artistCredit)
        Item(stringResource(R.string.year), recording.firstReleaseDate)
        Item(stringResource(R.string.comment), recording.disambiguation)
        Item("Releases", recording.releases?.map { "${it.title} (${it.date}/${it.barcode})" })
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
private fun MusicBrainzLifeSpan(lifeSpan: MusicBrainzArtist.LifeSpan?) {
    if (lifeSpan != null) Item("LifeSpan", lifeSpan.run { "$begin~$end ${if (ended) "ENDED" else ""}" })
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