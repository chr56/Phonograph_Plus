/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.base.Navigator
import player.phonograph.ui.compose.web.PageSearch.LastFmSearch
import player.phonograph.ui.compose.web.PageSearch.MusicBrainzSearch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.DrawerState
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun WebSearch(viewModel: WebSearchViewModel, scaffoldState: ScaffoldState, page: Page) {
    Scaffold(
        Modifier.statusBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(page.title(LocalContext.current)) },
                navigationIcon = {
                    Box(Modifier.padding(16.dp)) {
                        NavigateButton(scaffoldState.drawerState, viewModel.navigator)
                    }
                }
            )
        },
        drawerContent = {
            Drawer(viewModel)
        }
    ) {
        CompositionLocalProvider(LocalPageNavigator provides viewModel.navigator) {
            Box(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth()
            ) {
                when (page) {
                    PageHome -> Home(viewModel, page)
                    is LastFmSearch -> LastFmSearch(viewModel, page)
                    is MusicBrainzSearch -> MusicBrainzSearch(viewModel, page)
                    is PageDetail.LastFmDetail -> DetailLastFm(viewModel, page)
                    is PageDetail.MusicBrainzDetail -> DetailMusicBrainz(viewModel, page)
                }
            }
        }
    }
}

@Composable
fun NavigateButton(drawerState: DrawerState, navigator: Navigator<Page>) {
    val pageState by navigator.currentPage.collectAsState()
    if (navigator.isRoot(pageState)) {
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
fun ColumnScope.Drawer(viewModel: WebSearchViewModel) {
    val navigator = viewModel.navigator
    val page by navigator.currentPage.collectAsState()
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.h6
    ) {
        Column(
            Modifier
                .weight(9f)
                .padding(vertical = 12.dp)
        ) {
            Pages(navigator, page)
        }
        Column(Modifier.weight(4f)) {
            Switcher(navigator, LastFmSearch())
            Switcher(navigator, MusicBrainzSearch())
            Switcher(navigator, PageHome)
        }
    }
}

@Composable
private fun Pages(navigator: Navigator<Page>, currentPage: Page) {
    val pages = remember(currentPage) { navigator.pages }
    val context = LocalContext.current
    for ((index, page) in pages.reversed().withIndex()) {
        Text(
            text = page.title(context),
            Modifier
                .padding(12.dp)
                .clickable {
                    navigator.navigateUp(index)
                }
        )
    }
}

@Composable
private fun Switcher(navigator: Navigator<Page>, page: Page) {
    Text(
        page.title(LocalContext.current),
        Modifier
            .clickable {
                navigator.navigateTo(page)
            }
            .padding(12.dp)
    )
}