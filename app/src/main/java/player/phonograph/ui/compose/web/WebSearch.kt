/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.WebSearchViewModel.Page
import player.phonograph.ui.compose.web.WebSearchViewModel.Page.Search.LastFmSearch
import player.phonograph.ui.compose.web.WebSearchViewModel.Page.Search.MusicBrainzSearch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerState
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun Home(viewModel: WebSearchViewModel, pageState: Page) {
    val navigator = viewModel.navigator
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        HomeItem(navigator, LastFmSearch(viewModel.queryFactory.lastFm(context)))
        HomeItem(navigator, MusicBrainzSearch(viewModel.queryFactory.musicBrainzQuery(context)))
    }
}

@Composable
private fun HomeItem(navigator: WebSearchViewModel.Navigator, page: Page.Search<*>) {
    Text(
        page.source,
        modifier = Modifier
            .padding(32.dp)
            .clickable {
                navigator.navigateTo(page)
            }
            .fillMaxWidth(),
        style = MaterialTheme.typography.h4
    )
}
@Composable
fun NavigateButton(drawerState: DrawerState, navigator: WebSearchViewModel.Navigator) {
    val pageState by navigator.currentPage.collectAsState()
    if (pageState.isRoot()) {
        val coroutineScope = rememberCoroutineScope()
        Icon(
            Icons.Default.Menu, null,
            Modifier.clickable {
                coroutineScope.launch {
                    drawerState.open()
                }
            }
        )
    } else {
        Icon(
            Icons.Default.ArrowBack, null,
            Modifier.clickable {
                navigator.navigateUp()
            }
        )
    }
}

@Composable
fun ColumnScope.Drawer(navigator: WebSearchViewModel.Navigator, viewModel: WebSearchViewModel) {
    val page by navigator.currentPage.collectAsState()
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.h6
    ) {
        Column(Modifier.weight(9f).padding(vertical = 12.dp)) {
            Pages(navigator, page)
        }
        Column(Modifier.weight(4f)) {
            val context = LocalContext.current
            Switcher(navigator, LastFmSearch(viewModel.queryFactory.lastFm(context)))
            Switcher(navigator, MusicBrainzSearch(viewModel.queryFactory.musicBrainzQuery(context)))
            Switcher(navigator, Page.Home)
        }
    }
}

@Composable
private fun Pages(navigator: WebSearchViewModel.Navigator, currentPage: Page) {
    val pages = remember(currentPage) { navigator.pages }
    for ((index, page) in pages.reversed().withIndex()) {
        Text(
            text = stringResource(page.nameRes),
            Modifier
                .padding(12.dp)
                .clickable {
                    navigator.navigateUp(index)
                }
        )
    }
}

@Composable
private fun Switcher(navigator: WebSearchViewModel.Navigator, page: Page) {
    val text = "${stringResource(page.nameRes)} ${if (page is Page.Search<*>) page.source else ""}"
    Text(
        text,
        Modifier
            .clickable {
                navigator.navigateTo(page)
            }
            .padding(12.dp)
    )
}