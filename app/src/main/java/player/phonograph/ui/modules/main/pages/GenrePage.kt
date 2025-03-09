/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.loader.Genres
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.GenericDisplayAdapter
import player.phonograph.ui.adapter.GenreBasicDisplayPresenter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope

class GenrePage : AbsDisplayPage<Genre, GenericDisplayAdapter<Genre>>() {

    private val _viewModel: GenrePageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Genre> get() = _viewModel


    class GenrePageViewModel : AbsDisplayPageViewModel<Genre>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Genre> {
            return Genres.all(context)
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            dataSet.value.toList().flatMap { Genres.songs(context, it.id) }

        override val headerTextRes: Int get() = R.plurals.item_genres
    }


    override val displayConfig: PageDisplayConfig get() = GenrePageDisplayConfig(requireContext())

    override fun createAdapter(): GenericDisplayAdapter<Genre> {
        return GenericDisplayAdapter(requireActivity(), GenreDisplayPresenter.from(displayConfig))
    }

    override fun updateDisplayedItems(items: List<Genre>) {
        adapter.dataset = items
    }

    override fun updatePresenterSettings(
        sortMode: SortMode,
        usePalette: Boolean,
        layoutStyle: ItemLayoutStyle,
    ) {
        adapter.presenter = GenreDisplayPresenter.from(sortMode, layoutStyle)
    }


    class GenreDisplayPresenter(
        sortMode: SortMode,
        override val layoutStyle: ItemLayoutStyle,
    ) : GenreBasicDisplayPresenter(sortMode) {

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_NONE
        override val usePalette: Boolean = false

        companion object {

            fun from(displayConfig: PageDisplayConfig): GenreDisplayPresenter =
                GenreDisplayPresenter(displayConfig.sortMode, displayConfig.layout)

            fun from(sortMode: SortMode, layoutStyle: ItemLayoutStyle): GenreDisplayPresenter =
                GenreDisplayPresenter(sortMode, layoutStyle)
        }
    }
}