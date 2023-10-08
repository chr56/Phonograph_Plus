/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.repo.loader.Albums
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.AlbumDisplayAdapter
import player.phonograph.ui.fragments.pages.util.AlbumPageDisplayConfig
import player.phonograph.ui.fragments.pages.util.PageDisplayConfig
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class AlbumPage : AbsDisplayPage<Album, DisplayAdapter<Album>>() {

    override val viewModel: AbsDisplayPageViewModel<Album> get() = _viewModel

    private val _viewModel: AlbumPageViewModel by viewModels()

    class AlbumPageViewModel : AbsDisplayPageViewModel<Album>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Album> {
            return Albums.all(context)
        }

        override val headerTextRes: Int get() = R.plurals.item_albums
    }

    override fun displayConfig(): PageDisplayConfig = AlbumPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Album> {
        val displayConfig = displayConfig()
        return AlbumDisplayAdapter(
            hostFragment.mainActivity,
            adapterDisplayConfig.copy(layoutType = displayConfig.layout, usePalette = displayConfig.colorFooter),
        )
    }

    companion object {
        const val TAG = "AlbumPage"
    }
}
