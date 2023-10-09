/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Artist
import player.phonograph.repo.loader.Artists
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.ArtistDisplayAdapter
import player.phonograph.ui.fragments.pages.util.ArtistPageDisplayConfig
import player.phonograph.ui.fragments.pages.util.PageDisplayConfig
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class ArtistPage : AbsDisplayPage<Artist, DisplayAdapter<Artist>>() {

    override val viewModel: AbsDisplayPageViewModel<Artist> get() = _viewModel

    private val _viewModel: ArtistPageViewModel by viewModels()

    class ArtistPageViewModel : AbsDisplayPageViewModel<Artist>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Artist> {
            return Artists.all(App.instance)
        }

        override val headerTextRes: Int get() = R.plurals.item_artists
    }

    override fun displayConfig(): PageDisplayConfig = ArtistPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Artist> {
        val displayConfig = displayConfig()
        return ArtistDisplayAdapter(
            hostFragment.mainActivity,
            adapterDisplayConfig.copy(layoutStyle = displayConfig.layout, usePalette = displayConfig.colorFooter),
        )
    }

    companion object {
        const val TAG = "ArtistPage"
    }
}
