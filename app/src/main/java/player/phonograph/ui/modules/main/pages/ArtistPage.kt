/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader.allSongs
import player.phonograph.ui.adapter.ArtistBasicDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope

class ArtistPage : AbsDisplayPage<Artist, DisplayAdapter<Artist>>() {

    private val _viewModel: ArtistPageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Artist> get() = _viewModel


    class ArtistPageViewModel : AbsDisplayPageViewModel<Artist>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Artist> {
            return Artists.all(context)
        }

        override suspend fun collectAllSongs(context: Context): List<Song> = dataset.value.toList().allSongs(context)

        override val headerTextRes: Int get() = R.plurals.item_artists
    }


    override val displayConfig: PageDisplayConfig get() = ArtistPageDisplayConfig(requireContext())

    override fun createAdapter(): DisplayAdapter<Artist> {
        return DisplayAdapter(requireActivity(), ArtistDisplayPresenter.from(displayConfig))
    }

    override fun updateDisplayedItems(items: List<Artist>) {
        adapter.dataset = items
    }

    override fun updatePresenterSettings(
        sortMode: SortMode,
        usePalette: Boolean,
        layoutStyle: ItemLayoutStyle,
    ) {
        adapter.presenter = ArtistDisplayPresenter.from(sortMode, usePalette, layoutStyle)
    }


    class ArtistDisplayPresenter(
        sortMode: SortMode,
        override val usePalette: Boolean,
        override val layoutStyle: ItemLayoutStyle,
    ) : ArtistBasicDisplayPresenter(sortMode) {

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

        companion object {

            fun from(displayConfig: PageDisplayConfig): ArtistDisplayPresenter =
                ArtistDisplayPresenter(displayConfig.sortMode, displayConfig.colorFooter, displayConfig.layout)

            fun from(sortMode: SortMode, usePalette: Boolean, layoutStyle: ItemLayoutStyle): ArtistDisplayPresenter =
                ArtistDisplayPresenter(sortMode, usePalette, layoutStyle)
        }
    }

}