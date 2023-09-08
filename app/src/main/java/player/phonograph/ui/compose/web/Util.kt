/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.base.Navigator
import player.phonograph.ui.compose.web.MusicBrainzQuery.QueryAction.ViewArtist
import player.phonograph.ui.compose.web.MusicBrainzQuery.QueryAction.ViewRecording
import player.phonograph.ui.compose.web.MusicBrainzQuery.QueryAction.ViewRelease
import player.phonograph.ui.compose.web.MusicBrainzQuery.QueryAction.ViewReleaseGroup
import player.phonograph.ui.compose.web.MusicBrainzQuery.Target
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.launchIntent
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.viewIntentMusicBrainzArtist
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.viewIntentMusicBrainzRecording
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.viewIntentMusicBrainzRelease
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch

@Composable
fun JumpAndLinkMusicBrainz(modifier: Modifier, type: Target, mbid: String?) {
    Row(horizontalArrangement = Arrangement.SpaceAround) {
        JumpMusicBrainz(modifier, type, mbid)
        LinkMusicBrainz(modifier, type, mbid)
    }
}


@Composable
fun JumpMusicBrainz(modifier: Modifier, type: Target, mbid: String?) {
    if (!mbid.isNullOrEmpty()) {
        val context = LocalContext.current
        val navigator = LocalPageNavigator.current
        TextButton(
            onClick = {
                jumpMusicbrainz(context, navigator, type, mbid)
            },
            modifier = modifier
        ) {
            Text("MusicBrainz")
        }
    }
}

@Composable
fun LinkMusicBrainz(modifier: Modifier, type: Target, mbid: String?) {
    if (!mbid.isNullOrEmpty()) {
        val context = LocalContext.current
        TextButton(
            onClick = {
                clickLink(context, "https://musicbrainz.org/${type.urlName}/$mbid")
            },
            modifier = modifier
        ) {
            Text("MusicBrainz(${stringResource(id = R.string.website)})")
        }
    }
}

@Composable
fun LinkLastFm(modifier: Modifier, lastFmUri: String?) {
    if (lastFmUri != null) {
        val context = LocalContext.current
        TextButton(
            onClick = {
                clickLink(context, lastFmUri)
            },
            modifier = modifier
        ) {
            Text("Last.FM(${stringResource(id = R.string.website)})")
        }
    }
}


fun jumpMusicbrainz(context: Context, navigator: Navigator<Page>?, type: Target, mbid: String) {
    if (context is WebSearchActivity && navigator != null) {
        context.lifecycleScope.launch {
            val query = context.queryFactory.musicBrainzQuery(context)
            val result = when (type) {
                Target.ReleaseGroup -> query.query(context, ViewReleaseGroup(mbid))
                Target.Release      -> query.query(context, ViewRelease(mbid))
                Target.Artist       -> query.query(context, ViewArtist(mbid))
                Target.Recording    -> query.query(context, ViewRecording(mbid))
            }
            val page = Page.Detail.MusicBrainzDetail(
                result.await() ?: Any()
            )
            navigator.navigateTo(page)
        }
    } else {
        context.startActivity(
            when (type) {
                Target.Artist -> viewIntentMusicBrainzArtist(context, mbid)
                Target.Recording -> viewIntentMusicBrainzRecording(context, mbid)
                Target.Release -> viewIntentMusicBrainzRelease(context, mbid)
                else -> launchIntent(context)
            }
        )
    }
}

fun clickLink(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

@Composable
fun Line(name: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            name,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(2f)
                .align(Alignment.CenterVertically)
        )
        Box(
            Modifier
                .weight(9f)
        ) {
            content()
        }
    }
}