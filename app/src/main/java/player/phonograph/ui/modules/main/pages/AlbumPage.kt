/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader.allSongs
import player.phonograph.ui.adapter.AlbumBasicDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope

class AlbumPage : AbsDisplayPage<Album, DisplayAdapter<Album>>() {

    private val _viewModel: AlbumPageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Album> get() = _viewModel


    class AlbumPageViewModel : AbsDisplayPageViewModel<Album>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Album> {
            return Albums.all(context)
        }

        override suspend fun collectAllSongs(context: Context): List<Song> = dataset.value.toList().allSongs(context)

        override val headerTextRes: Int get() = R.plurals.item_albums
    }


    override val displayConfig: PageDisplayConfig get() = AlbumPageDisplayConfig(requireContext())

    override fun createAdapter(): DisplayAdapter<Album> {
        return DisplayAdapter(requireActivity(), AlbumDisplayPresenter.from(displayConfig))
    }

    override fun updateDisplayedItems(items: List<Album>) {
        adapter.dataset = items
    }

    override fun updatePresenterSettings(
        sortMode: SortMode,
        usePalette: Boolean,
        layoutStyle: ItemLayoutStyle,
    ) {
        adapter.presenter = AlbumDisplayPresenter.from(sortMode, usePalette, layoutStyle)
    }


    class AlbumDisplayPresenter(
        sortMode: SortMode,
        override val usePalette: Boolean,
        override val layoutStyle: ItemLayoutStyle,
    ) : AlbumBasicDisplayPresenter(sortMode) {

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

        companion object {

            fun from(displayConfig: PageDisplayConfig): AlbumDisplayPresenter =
                AlbumDisplayPresenter(displayConfig.sortMode, displayConfig.colorFooter, displayConfig.layout)

            fun from(sortMode: SortMode, usePalette: Boolean, layoutStyle: ItemLayoutStyle): AlbumDisplayPresenter =
                AlbumDisplayPresenter(sortMode, usePalette, layoutStyle)
        }
    }

}