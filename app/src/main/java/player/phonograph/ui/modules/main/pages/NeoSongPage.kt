/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.GenericDisplayAdapter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope

class NeoSongPage : BasicDisplayPage<Song, GenericDisplayAdapter<Song>>() {

    private val _viewModel: SongPageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Song> get() = _viewModel


    class SongPageViewModel : AbsDisplayPageViewModel<Song>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Song> {
            return Songs.all(context)
        }

        override suspend fun collectAllSongs(context: Context): List<Song> = dataSet.value.toList()

        override val headerTextRes: Int get() = R.plurals.item_songs
    }


    override val displayConfig: PageDisplayConfig get() = SongPageDisplayConfig(requireContext())

    override fun createAdapter(): GenericDisplayAdapter<Song> {
        return GenericDisplayAdapter(requireActivity(), SongDisplayPresenter.from(displayConfig))
    }

    override fun updateDisplayedItems(items: List<Song>) {
        adapter.dataset = items
    }

    override fun updatePresenterSettings(
        sortMode: SortMode,
        usePalette: Boolean,
        layoutStyle: ItemLayoutStyle,
    ) {
        adapter.presenter = SongDisplayPresenter.from(sortMode, usePalette, layoutStyle)
    }


    class SongDisplayPresenter(
        sortMode: SortMode,
        override val usePalette: Boolean,
        override val layoutStyle: ItemLayoutStyle,
    ) : SongBasicDisplayPresenter(sortMode) {

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

        companion object {

            fun from(displayConfig: PageDisplayConfig): SongDisplayPresenter =
                SongDisplayPresenter(displayConfig.sortMode, displayConfig.colorFooter, displayConfig.layout)

            fun from(sortMode: SortMode, usePalette: Boolean, layoutStyle: ItemLayoutStyle): SongDisplayPresenter =
                SongDisplayPresenter(sortMode, usePalette, layoutStyle)
        }
    }

}