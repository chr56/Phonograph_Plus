/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
        // title
        Row(Modifier.height(56.dp)) {
            Icon(Icons.Default.ArrowBack, stringResource(android.R.string.cancel), modifier = Modifier
                .clickable {
                    viewModel.updatePage(WebSearchViewModel.Page.Search)
                }
                .align(Alignment.CenterVertically)
                .fillMaxHeight()
                .padding(horizontal = 22.dp))
            Text(
                stringResource(R.string.label_details),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.h6
            )
        }
        //content
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val query = queryState) {
                is LastFmQuery -> DetailLastFm(viewModel, query)
                else           -> {}
            }
        }
    }
}

@Composable
fun LinkMusicBrainz(modifier: Modifier, type: String, mbid: String?) {
    if (mbid != null) {
        val context = LocalContext.current
        TextButton(
            onClick = {
                clickLink(context, "https://musicbrainz.org/$type/$mbid")
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
                clickLink(context, "Last.FM")
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