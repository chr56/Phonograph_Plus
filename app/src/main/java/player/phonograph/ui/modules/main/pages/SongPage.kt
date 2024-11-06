/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.modules.main.pages.adapter.SongDisplayAdapter
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class SongPage : AbsDisplayPage<Song, DisplayAdapter<Song>>() {

    override val viewModel: AbsDisplayPageViewModel<Song> get() = _viewModel

    private val _viewModel: SongPageViewModel by viewModels()

    class SongPageViewModel : AbsDisplayPageViewModel<Song>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Song> {
            return Songs.all(App.instance)
        }

        override suspend fun collectAllSongs(context: Context): List<Song> = dataSet.value.toList()

        override val headerTextRes: Int get() = R.plurals.item_songs
    }

    override fun displayConfig(): PageDisplayConfig = SongPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Song> {
        val displayConfig = displayConfig()
        return SongDisplayAdapter(
            mainActivity,
            adapterDisplayConfig.copy(layoutStyle = displayConfig.layout, usePalette = displayConfig.colorFooter),
        )
    }

    companion object {
        const val TAG = "SongPage"
    }
}
