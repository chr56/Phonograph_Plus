/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import player.phonograph.R
import player.phonograph.ui.compose.base.Navigator
import player.phonograph.ui.modules.web.WebSearchLauncher.launchIntent
import player.phonograph.ui.modules.web.WebSearchLauncher.viewIntentMusicBrainzArtist
import player.phonograph.ui.modules.web.WebSearchLauncher.viewIntentMusicBrainzRecording
import player.phonograph.ui.modules.web.WebSearchLauncher.viewIntentMusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target
import util.phonograph.tagsources.musicbrainz.MusicBrainzClientDelegate
import util.phonograph.tagsources.musicbrainz.MusicBrainzModel
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch

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
            val clientDelegate: MusicBrainzClientDelegate = context.viewModel.clientDelegateMusicBrainz(context)
            val result = clientDelegate.request(context, MusicBrainzAction.View(type, mbid))
            val page = PageDetail.MusicBrainzDetail(result.await() as? MusicBrainzModel)
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