/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Songs
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.adapter.AlbumDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayOptions
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class AlbumPage : AbsDisplayPage<Album, DisplayAdapter<Album>>() {

    private val _viewModel: AlbumPageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Album> get() = _viewModel


    class AlbumPageViewModel : AbsDisplayPageViewModel<Album>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Album> {
            return Albums.all(context, Settings(context)[Keys.albumSortMode].read())
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            dataset.value.toList().flatMap { artist -> Songs.album(context, artist.id) }

        override val headerTextRes: Int get() = R.plurals.item_albums
    }


    override val displayConfig: PageDisplayConfig get() = AlbumPageDisplayConfig(requireContext())

    override fun createAdapter(): DisplayAdapter<Album> {
        return DisplayAdapter(
            requireActivity(),
            AlbumDisplayPresenter(),
            DisplayOptions(
                displayConfig.layout,
                displayConfig.sortMode,
                displayConfig.colorFooter
            )
        )
    }

    override fun updateDisplayedItems(items: List<Album>) {
        adapter.dataset = items
    }
}