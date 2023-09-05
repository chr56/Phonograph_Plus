/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun Home(viewModel: WebSearchViewModel, pageState: WebSearchViewModel.Page) {
    val navigator = viewModel.navigator
    Column(Modifier.fillMaxSize()) {
        Item(navigator, WebSearchViewModel.Page.Search)
    }
}

@Composable
private fun Item(navigator: WebSearchViewModel.Navigator, page: WebSearchViewModel.Page) {
    Text(
        stringResource(page.nameRes),
        modifier = Modifier
            .padding(32.dp)
            .clickable {
                navigator.navigateTo(page)
            }
            .fillMaxWidth(),
        style = MaterialTheme.typography.h4
    )
}