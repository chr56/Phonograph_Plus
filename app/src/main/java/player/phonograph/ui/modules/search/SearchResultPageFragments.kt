/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import player.phonograph.R
import player.phonograph.actions.menu.ActionMenuProviders
import player.phonograph.databinding.RecyclerViewWrappedProperBinding
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.ItemLayoutStyle
import player.phonograph.ui.adapter.OrderedItemAdapter
import player.phonograph.ui.fragments.pages.adapter.AlbumDisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.ArtistDisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.PlaylistDisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.SongDisplayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Fragment to display result with recycler view
 *
 * **NOTE**: must create from [SearchActivity] (as host activity)
 */
abstract class SearchResultPageFragment<T : Displayable> : Fragment() {

    private var _viewBinding: RecyclerViewWrappedProperBinding? = null
    private val binding get() = _viewBinding!!

    val viewModel: SearchActivityViewModel by viewModels(ownerProducer = { requireActivity() })

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
        }
        observeData(targetFlow())
    }

    protected abstract fun targetFlow(): StateFlow<List<T>>

    protected abstract fun updateDataset(newData: List<T>)

    private fun observeData(flow: StateFlow<List<T>>) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { data ->
                    binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                    updateDataset(data)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }
}

class SongSearchResultPageFragment : SearchResultPageFragment<Song>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Song>? get() = actualAdapter as? DisplayAdapter<Song>

    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Song> =
        SongDisplayAdapter(activity, ConstDisplayConfig(ItemLayoutStyle.LIST, false))

    override fun targetFlow(): StateFlow<List<Song>> = viewModel.songs

    override fun updateDataset(newData: List<Song>) {
        adapter?.dataset = newData
    }
}

class AlbumSearchResultPageFragment : SearchResultPageFragment<Album>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Album>? get() = actualAdapter as? DisplayAdapter<Album>

    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Album> =
        AlbumDisplayAdapter(activity, ConstDisplayConfig(ItemLayoutStyle.LIST))

    override fun targetFlow(): StateFlow<List<Album>> = viewModel.albums

    override fun updateDataset(newData: List<Album>) {
        adapter?.dataset = newData
    }
}

class ArtistSearchResultPageFragment : SearchResultPageFragment<Artist>() {
    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Artist>? get() = actualAdapter as? DisplayAdapter<Artist>


    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Artist> =
        ArtistDisplayAdapter(activity, ConstDisplayConfig(ItemLayoutStyle.LIST))

    override fun targetFlow(): StateFlow<List<Artist>> = viewModel.artists

    override fun updateDataset(newData: List<Artist>) {
        adapter?.dataset = newData
    }
}

class PlaylistSearchResultPageFragment : SearchResultPageFragment<Playlist>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: DisplayAdapter<Playlist>? get() = actualAdapter as? DisplayAdapter<Playlist>

    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Playlist> {
        return PlaylistDisplayAdapter(activity)
    }

    override fun targetFlow(): StateFlow<List<Playlist>> = viewModel.playlists

    override fun updateDataset(newData: List<Playlist>) {
        adapter?.dataset = newData
    }
}

class QueueSearchResultPageFragment : SearchResultPageFragment<QueueSong>() {

    private val adapter: QueueSongAdapter? get() = actualAdapter as? QueueSongAdapter

    override fun createAdapter(activity: AppCompatActivity): QueueSongAdapter =
        QueueSongAdapter(activity)

    override fun targetFlow(): StateFlow<List<QueueSong>> = viewModel.songsInQueue

    override fun updateDataset(newData: List<QueueSong>) {
        adapter?.dataset = newData
    }

    class QueueSongAdapter(
        activity: FragmentActivity,
    ) : OrderedItemAdapter<QueueSong>(activity, R.layout.item_list, showSectionName = true) {

        override fun getSectionNameImp(position: Int): String {
            return dataset[position].index.toString()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderedItemViewHolder<QueueSong> =
            QueueSongViewHolder(inflatedView(parent, viewType))

        inner class QueueSongViewHolder(itemView: View) : OrderedItemViewHolder<QueueSong>(itemView) {

            override fun getRelativeOrdinalText(item: QueueSong, position: Int): String {
                return item.index.toString()
            }

            override fun onClick(position: Int, dataset: List<QueueSong>, imageView: ImageView?): Boolean {
                MusicPlayerRemote.playSongAt(dataset[position].index)
                return true
            }

            override fun prepareMenu(item: QueueSong, position: Int, menuButtonView: View) {
                ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = position)
                    .prepareActionMenu(menuButtonView, item.song)
            }
        }
    }

}
