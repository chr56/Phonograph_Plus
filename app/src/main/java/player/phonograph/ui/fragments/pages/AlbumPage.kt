/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.R
import player.phonograph.adapter.display.AlbumDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.model.Album
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import androidx.fragment.app.viewModels
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class AlbumPage : AbsDisplayPage<Album, DisplayAdapter<Album>>() {

    override val viewModel: AbsDisplayPageViewModel<Album> get() = _viewModel

    private val _viewModel: AlbumPageViewModel by viewModels()

    class AlbumPageViewModel : AbsDisplayPageViewModel<Album>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Album> {
            return AlbumLoader.all(context)
        }

        override val headerTextRes: Int get() = R.plurals.item_albums
    }

    override val displayConfigTarget get() = DisplayConfigTarget.AlbumPage

    override fun initAdapter(): DisplayAdapter<Album> {
        val displayConfig = DisplayConfig(displayConfigTarget)

        val layoutRes =
            if (displayConfig.gridSize > displayConfig.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list

        return AlbumDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            ArrayList(), // empty until Albums loaded
            layoutRes
        ) {
            usePalette = displayConfig.colorFooter
        }
    }


    override fun updateDataset(dataSet: List<Album>) {
        adapter.dataset = dataSet
    }


    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.ALBUM_NAME,
            SortRef.ARTIST_NAME,
            SortRef.YEAR,
            SortRef.SONG_COUNT,
        )

    companion object {
        const val TAG = "AlbumPage"
    }
}
