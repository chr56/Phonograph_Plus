/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.web.MusicBrainzQuery.Target
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.launchIntent
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.launchIntentMusicBrainzArtist
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.launchIntentMusicBrainzRecording
import player.phonograph.ui.compose.web.WebSearchActivity.Companion.launchIntentMusicBrainzRelease
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.net.Uri

@Composable
fun Detail(viewModel: WebSearchViewModel) {
    Column {
        val queryState by viewModel.query.collectAsState()
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val query = queryState) {
                is LastFmQuery      -> DetailLastFm(viewModel, query)
                is MusicBrainzQuery -> DetailMusicBrainz(viewModel, query)
                else                -> {}
            }
        }
    }
}

@Composable
fun JumpMusicBrainz(modifier: Modifier, type: Target, mbid: String?) {
    if (!mbid.isNullOrEmpty()) {
        val context = LocalContext.current
        TextButton(
            onClick = {
                context.startActivity(
                    when (type) {
                        Target.Artist    -> launchIntentMusicBrainzArtist(context, mbid)
                        Target.Recording -> launchIntentMusicBrainzRecording(context, mbid)
                        Target.Release   -> launchIntentMusicBrainzRelease(context, mbid)
                        else             -> launchIntent(context)
                    }
                )
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


fun clickLink(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}