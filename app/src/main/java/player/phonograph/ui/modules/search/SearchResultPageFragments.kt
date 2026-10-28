/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import player.phonograph.databinding.RecyclerViewWrappedProperBinding
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.actions.ActionMenus
import player.phonograph.ui.actions.ItemClickActions
import player.phonograph.ui.adapter.AlbumDisplayPresenter
import player.phonograph.ui.adapter.ArtistDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayOptions
import player.phonograph.ui.adapter.GenreDisplayPresenter
import player.phonograph.ui.adapter.PlaylistDisplayPresenter
import player.phonograph.ui.adapter.QueueSongDisplayPresenter
import player.phonograph.ui.adapter.SongDisplayPresenter
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.util.BottomViewWindowInsetsController
import player.phonograph.ui.util.applyControllableWindowInsetsAsBottomView
import player.phonograph.ui.util.observe
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.flow.StateFlow

/**
 * Fragment to display result with recycler view
 *
 * **NOTE**: must create from [SearchActivity] (as host activity)
 */
abstract class SearchResultPageFragment<T> : Fragment() {

    private var _viewBinding: RecyclerViewWrappedProperBinding? = null
    private val binding get() = _viewBinding!!

    val viewModel: SearchActivityViewModel by viewModels(ownerProducer = { requireActivity() })

    protected val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })

    protected lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController

    protected lateinit var actualAdapter: RecyclerView.Adapter<*>

    protected abstract fun createAdapter(activity: AppCompatActivity): RecyclerView.Adapter<*>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = RecyclerViewWrappedProperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        actualAdapter = createAdapter(activity as AppCompatActivity)
        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = actualAdapter
            // noinspection ClickableViewAccessibility
            recyclerView.setOnTouchListener { _, _ ->
                // hideSoftKeyboard() //todo
                false
            }
            bottomViewWindowInsetsController = recyclerView.applyControllableWindowInsetsAsBottomView()
        }
        observe(targetFlow()) { data ->
            binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
            updateDataset(data)
        }
        observe(panelViewModel.isMiniPlayerHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    protected abstract fun targetFlow(): StateFlow<List<T>>

    protected abstract fun updateDataset(newData: List<T>)

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }
}

class SongSearchResultPageFragment : SearchResultPageFragment<Song>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Song>? get() = actualAdapter as? DisplayAdapter<Song>

    override fun createAdapter(activity: AppCompatActivity) =
        DisplayAdapter(
            activity,
            SongDisplayPresenter(),
            DisplayOptions(ItemLayoutStyle.LIST, SortMode(SortRef.DISPLAY_NAME), false)
        )

    override fun targetFlow(): StateFlow<List<Song>> = viewModel.songs

    override fun updateDataset(newData: List<Song>) {
        adapter?.dataset = newData
    }
}

class AlbumSearchResultPageFragment : SearchResultPageFragment<Album>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Album>? get() = actualAdapter as? DisplayAdapter<Album>

    override fun createAdapter(activity: AppCompatActivity) =
        DisplayAdapter(
            activity,
            AlbumDisplayPresenter(),
            DisplayOptions(ItemLayoutStyle.LIST, SortMode(SortRef.DISPLAY_NAME), false)
        )

    override fun targetFlow(): StateFlow<List<Album>> = viewModel.albums

    override fun updateDataset(newData: List<Album>) {
        adapter?.dataset = newData
    }
}

class ArtistSearchResultPageFragment : SearchResultPageFragment<Artist>() {
    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Artist>? get() = actualAdapter as? DisplayAdapter<Artist>

    override fun createAdapter(activity: AppCompatActivity) =
        DisplayAdapter(
            activity,
            ArtistDisplayPresenter(),
            DisplayOptions(ItemLayoutStyle.LIST, SortMode(SortRef.DISPLAY_NAME), false)
        )

    override fun targetFlow(): StateFlow<List<Artist>> = viewModel.artists

    override fun updateDataset(newData: List<Artist>) {
        adapter?.dataset = newData
    }
}

class PlaylistSearchResultPageFragment : SearchResultPageFragment<Playlist>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Playlist>? get() = actualAdapter as? DisplayAdapter<Playlist>

    override fun createAdapter(activity: AppCompatActivity) =
        DisplayAdapter(
            activity,
            PlaylistDisplayPresenter(),
            DisplayOptions(
                ItemLayoutStyle.LIST, SortMode(SortRef.DISPLAY_NAME), false,
                imageryType = DisplayOptions.ICON,
            )
        )


    override fun targetFlow(): StateFlow<List<Playlist>> = viewModel.playlists

    override fun updateDataset(newData: List<Playlist>) {
        adapter?.dataset = newData
    }
}

class GenreSearchResultPageFragment : SearchResultPageFragment<Genre>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Genre>? get() = actualAdapter as? DisplayAdapter<Genre>

    override fun createAdapter(activity: AppCompatActivity) =
        DisplayAdapter(
            activity,
            GenreDisplayPresenter(),
            DisplayOptions(
                ItemLayoutStyle.LIST, SortMode(SortRef.DISPLAY_NAME), false,
                imageryType = DisplayOptions.NONE,
            )
        )


    override fun targetFlow(): StateFlow<List<Genre>> = viewModel.genres

    override fun updateDataset(newData: List<Genre>) {
        adapter?.dataset = newData
    }
}

class QueueSearchResultPageFragment : SearchResultPageFragment<QueueSong>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<QueueSong>? get() = actualAdapter as? DisplayAdapter<QueueSong>

    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<QueueSong> =
        DisplayAdapter(
            activity,
            PlaylistSearchResultDisplayPresenter,
            DisplayOptions(ItemLayoutStyle.LIST, null, true, imageryType = DisplayOptions.ORDINAL_TEXT)
        )

    override fun targetFlow(): StateFlow<List<QueueSong>> = viewModel.songsInQueue

    override fun updateDataset(newData: List<QueueSong>) {
        adapter?.dataset = newData
    }

    object PlaylistSearchResultDisplayPresenter : QueueSongDisplayPresenter() {

        override fun clickAction(context: Context, item: QueueSong, position: Int, list: List<QueueSong>): ActionEntry? =
            ItemClickActions.PlayingQueue(item.index)

        override fun menuEntries(context: Context, item: QueueSong, position: Int): List<ActionEntry> =
            ActionMenus.Song(context, item.song, showPlay = false, index = item.index)

    }

}
