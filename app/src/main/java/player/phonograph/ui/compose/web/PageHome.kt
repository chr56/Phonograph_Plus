/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object PageHome : Page(R.string.intro_label)

@Composable
fun Home(viewModel: WebSearchViewModel, pageState: Page) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        HomeItem(PageSearch.LastFmSearch(viewModel.queryFactory.lastFmQuery(context)))
        HomeItem(PageSearch.MusicBrainzSearch(viewModel.queryFactory.musicBrainzQuery(context)))
    }
}

@Composable
private fun HomeItem(page: PageSearch<*>) {
    val navigator = LocalPageNavigator.current
    Text(
        page.source,
        modifier = Modifier
            .padding(32.dp)
            .clickable {
                navigator?.navigateTo(page)
            }
            .fillMaxWidth(),
        style = MaterialTheme.typography.h4
    )
}