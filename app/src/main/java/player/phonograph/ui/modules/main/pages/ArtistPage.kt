/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader.allSongs
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.modules.main.pages.adapter.ArtistDisplayAdapter
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

        override suspend fun collectAllSongs(context: Context): List<Song> = dataSet.value.toList().allSongs(context)

        override val headerTextRes: Int get() = R.plurals.item_artists
    }

    override fun displayConfig(): PageDisplayConfig = ArtistPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Artist> {
        val displayConfig = displayConfig()
        return ArtistDisplayAdapter(
            mainActivity,
            ConstDisplayConfig(layoutStyle = displayConfig.layout, usePalette = displayConfig.colorFooter),
        )
    }

    companion object {
        const val TAG = "ArtistPage"
    }
}
