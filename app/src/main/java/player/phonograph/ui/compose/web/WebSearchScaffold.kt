/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun NavigateButton(drawerState: DrawerState, navigator: WebSearchViewModel.Navigator) {
    val pageState by navigator.page.collectAsState()
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
fun ColumnScope.Drawer(navigator: WebSearchViewModel.Navigator) {
    val pageState by navigator.page.collectAsState()
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.h6
    ) {
        Spacer(
            modifier = Modifier
                .height(32.dp)
                .weight(WEIGHT_SPACE_TOP)
        )
        Text(
            text = stringResource(pageState.nameRes),
            Modifier
                .padding(12.dp)
                .weight(WEIGHT_CURRENT)
        )
        Spacer(
            modifier = Modifier
                .height(32.dp)
                .weight(WEIGHT_SPACE_GAP)
        )
        Switcher(navigator, WebSearchViewModel.Page.Search)
        Switcher(navigator, WebSearchViewModel.Page.Detail)
    }
}

@Composable
fun ColumnScope.Switcher(navigator: WebSearchViewModel.Navigator, page: WebSearchViewModel.Page) {
    Text(
        text = stringResource(page.nameRes),
        Modifier
            .clickable {
                navigator.navigateTo(page)
            }
            .padding(12.dp)
            .weight(WEIGHT_SWITCHER)
    )
}

private const val WEIGHT_CURRENT = 2f
private const val WEIGHT_SWITCHER = 2f
private const val WEIGHT_SPACE_TOP = 3f
private const val WEIGHT_SPACE_GAP = 20f