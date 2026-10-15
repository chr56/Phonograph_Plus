/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Songs
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.adapter.ArtistDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayOptions
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class ArtistPage : AbsDisplayPage<Artist, DisplayAdapter<Artist>>() {

    private val _viewModel: ArtistPageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Artist> get() = _viewModel


    class ArtistPageViewModel : AbsDisplayPageViewModel<Artist>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Artist> {
            return Artists.all(context, Settings(context)[Keys.artistSortMode].read())
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            dataset.value.toList().flatMap { artist -> Songs.artist(context, artist.id) }

        override val headerTextRes: Int get() = R.plurals.item_artists
    }


    override val displayConfig: PageDisplayConfig get() = ArtistPageDisplayConfig(requireContext())

    override fun createAdapter(): DisplayAdapter<Artist> {
        return DisplayAdapter(
            requireActivity(),
            ArtistDisplayPresenter(),
            DisplayOptions(
                displayConfig.layout,
                displayConfig.sortMode,
                displayConfig.colorFooter
            )
        )
    }

    override fun updateDisplayedItems(items: List<Artist>) {
        adapter.dataset = items
    }

}