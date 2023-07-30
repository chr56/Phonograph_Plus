/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_extension.add
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.Menu.NONE
import android.view.MenuItem
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope

class SongPage : AbsDisplayPage<Song, DisplayAdapter<Song>>() {

    override val viewModel: AbsDisplayPageViewModel<Song> get() = _viewModel

    private val _viewModel: SongPageViewModel by viewModels()

    class SongPageViewModel : AbsDisplayPageViewModel<Song>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Song> {
            return SongLoader.all(App.instance)
        }

        override val headerTextRes: Int get() = R.plurals.item_songs
    }

    override val displayConfigTarget get() = DisplayConfigTarget.SongPage

    override fun initAdapter(): DisplayAdapter<Song> {
        val displayConfig = DisplayConfig(displayConfigTarget)

        val layoutRes =
            if (displayConfig.gridSize > displayConfig.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG,
            "layoutRes: ${if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN"}"
        )

        return SongDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            ArrayList(), // empty until songs loaded
            layoutRes
        ) {
            usePalette = displayConfig.colorFooter
        }
    }


    override fun updateDataset(dataSet: List<Song>) {
        adapter.dataset = dataSet
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshDataSet() {
        adapter.notifyDataSetChanged()
    }

    override fun setupSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup,
    ) {

        val currentSortMode = displayConfig.sortMode
        if (BuildConfig.DEBUG) Log.d(GenrePage.TAG, "Read cfg: sortMode $currentSortMode")

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable =
            arrayOf(
                SortRef.SONG_NAME,
                SortRef.ALBUM_NAME,
                SortRef.ARTIST_NAME,
                SortRef.ALBUM_ARTIST_NAME,
                SortRef.COMPOSER,
                SortRef.YEAR,
                SortRef.ADDED_DATE,
                SortRef.MODIFIED_DATE,
                SortRef.DURATION,
            )
    }

    override fun saveSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup,
    ) {
        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayConfig.sortMode != selected) {
            displayConfig.sortMode = selected
            viewModel.loadDataset(requireContext())
            Log.d(AlbumPage.TAG, "Write cfg: sortMode $selected")
        }
    }

    override fun configAppBar(panelToolbar: Toolbar) {
        val context = hostFragment.mainActivity
        attach(context, panelToolbar.menu) {
            rootMenu.add(this, NONE, NONE, 1, getString(R.string.action_play)) {
                icon = context
                    .getTintedDrawable(
                        R.drawable.ic_play_arrow_white_24dp,
                        context.primaryTextColor(context.nightMode)
                    )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    val allSongs = SongLoader.all(context)
                    allSongs.actionPlay(ShuffleMode.NONE, 0)
                    true
                }
            }
            rootMenu.add(this, NONE, NONE, 2, getString(R.string.action_shuffle_all)) {
                icon = context
                    .getTintedDrawable(
                        R.drawable.ic_shuffle_white_24dp,
                        context.primaryTextColor(context.nightMode)
                    )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    val allSongs = SongLoader.all(context)
                    allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                    true
                }
            }
        }
    }

    companion object {
        const val TAG = "SongPage"
    }
}
