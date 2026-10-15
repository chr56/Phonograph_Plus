/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.repo.loader.Genres
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayOptions
import player.phonograph.ui.adapter.GenreDisplayPresenter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class GenrePage : AbsDisplayPage<Genre, DisplayAdapter<Genre>>() {

    private val _viewModel: GenrePageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Genre> get() = _viewModel


    class GenrePageViewModel : AbsDisplayPageViewModel<Genre>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Genre> {
            return Genres.all(context, Settings(context)[Keys.genreSortMode].read())
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            dataset.value.toList().flatMap { Genres.songs(context, it.id) }

        override val headerTextRes: Int get() = R.plurals.item_genres
    }


    override val displayConfig: PageDisplayConfig get() = GenrePageDisplayConfig(requireContext())

    override fun createAdapter(): DisplayAdapter<Genre> {
        return DisplayAdapter(
            requireActivity(),
            GenreDisplayPresenter(),
            DisplayOptions(
                displayConfig.layout,
                displayConfig.sortMode,
                displayConfig.colorFooter
            )
        )
    }

    override fun updateDisplayedItems(items: List<Genre>) {
        adapter.dataset = items
    }
}