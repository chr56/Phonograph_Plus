/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader.allSongs
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.modules.main.pages.adapter.AlbumDisplayAdapter
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

        override suspend fun collectAllSongs(context: Context): List<Song> = dataSet.value.toList().allSongs(context)

        override val headerTextRes: Int get() = R.plurals.item_albums
    }

    override fun displayConfig(): PageDisplayConfig = AlbumPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Album> {
        val displayConfig = displayConfig()
        return AlbumDisplayAdapter(
            mainActivity,
            ConstDisplayConfig(layoutStyle = displayConfig.layout, usePalette = displayConfig.colorFooter),
        )
    }

    companion object {
        const val TAG = "AlbumPage"
    }
}
