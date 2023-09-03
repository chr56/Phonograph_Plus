/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.dialogs.LastFmAlbum
import player.phonograph.ui.compose.dialogs.LastFmArtist
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import util.phonograph.tagsources.lastfm.LastFmAlbum as LastFmAlbumModel
import util.phonograph.tagsources.lastfm.LastFmArtist as LastFmArtistModel

@Composable
fun Detail(viewModel: WebSearchViewModel) {
    Column() {
        // title
        Row(Modifier.height(56.dp)) {
            Icon(
                Icons.Default.ArrowBack,
                stringResource(android.R.string.cancel),
                modifier = Modifier
                    .clickable {
                        viewModel.updatePage(WebSearchViewModel.Page.Search)
                    }
                    .align(Alignment.CenterVertically)
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp)
            )
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
            val item by viewModel.detail.collectAsState()
            when (val i = item) {
                is LastFmAlbumModel -> LastFmAlbum(i)
                is LastFmArtistModel -> LastFmArtist(i)
                null -> Text(
                    stringResource(R.string.empty),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

