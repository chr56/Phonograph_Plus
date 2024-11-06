/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.repo.loader.Genres
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.modules.main.pages.adapter.GenreDisplayAdapter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class GenrePage : AbsDisplayPage<Genre, DisplayAdapter<Genre>>() {

    override val viewModel: AbsDisplayPageViewModel<Genre> get() = _viewModel

    private val _viewModel: GenrePageViewModel by viewModels()

    class GenrePageViewModel : AbsDisplayPageViewModel<Genre>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Genre> {
            return Genres.all(context)
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            dataSet.value.toList().flatMap { Genres.songs(context, it.id) }

        override val headerTextRes: Int get() = R.plurals.item_genres
    }

    override fun displayConfig(): PageDisplayConfig = GenrePageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Genre> {
        return GenreDisplayAdapter(mainActivity)
    }

    companion object {
        const val TAG = "GenrePage"
    }
}
