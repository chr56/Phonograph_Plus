/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

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
import androidx.compose.ui.unit.dp

object PageHome : Page(R.string.intro_label)

@Composable
fun Home(viewModel: WebSearchViewModel, pageState: Page) {
    Column(Modifier.fillMaxSize()) {
        HomeItem(PageSearch.LastFmSearch())
        HomeItem(PageSearch.MusicBrainzSearch())
    }
}

@Composable
private fun HomeItem(page: PageSearch<*>) {
    val navigator = LocalPageNavigator.current
    Text(
        page.source.name,
        modifier = Modifier
            .padding(32.dp)
            .clickable {
                navigator?.navigateTo(page)
            }
            .fillMaxWidth(),
        style = MaterialTheme.typography.h4
    )
}