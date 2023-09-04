/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.activity.ThemeActivity
import player.phonograph.R
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.parcelableExtra
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.os.Bundle
import player.phonograph.model.Album as PhonographAlbum
import player.phonograph.model.Artist as PhonographArtist
import player.phonograph.model.Song as PhonographSong

class WebSearchActivity : ThemeActivity() {

    private val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.useCustomStatusBar = false
        super.onCreate(savedInstanceState)

        val query = checkCommand(this, intent)
        viewModel.prepareQuery(this, query)

        setContent {
            PhonographTheme {

                val pageState by viewModel.page.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(id = R.string.web_search)) }
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth()
                    ) {

                        when (pageState) {
                            WebSearchViewModel.Page.Search -> Search(viewModel)
                            WebSearchViewModel.Page.Detail -> Detail(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (viewModel.page.value != WebSearchViewModel.Page.Search) {
            viewModel.updatePage(WebSearchViewModel.Page.Search)
        } else {
            super.onBackPressed()
        }
    }

    private fun checkCommand(context: Context, intent: Intent): Query<*, *>? {
        val factory = viewModel.queryFactory
        return when (intent.getStringExtra(EXTRA_COMMAND)) {
            EXTRA_QUERY_ALBUM  -> intent.parcelableExtra<PhonographAlbum>(EXTRA_DATA)?.let { factory.from(context, it) }
            EXTRA_QUERY_ARTIST -> intent.parcelableExtra<PhonographArtist>(EXTRA_DATA)?.let { factory.from(context, it) }
            EXTRA_QUERY_SONG   -> intent.parcelableExtra<PhonographSong>(EXTRA_DATA)?.let { factory.from(context, it) }
            else             -> null
        }
    }

    companion object {
        const val EXTRA_COMMAND = "COMMAND"
        const val EXTRA_QUERY_SONG = "Query:Song"
        const val EXTRA_QUERY_ARTIST = "Query:Artist"
        const val EXTRA_QUERY_ALBUM = "Query:Album"
        const val EXTRA_DATA = "DATA"

        fun launchIntent(context: Context, data: PhonographAlbum?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_ALBUM)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context, data: PhonographArtist?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_ARTIST)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context, data: PhonographSong?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_SONG)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_DOCUMENT
            }
    }
}

