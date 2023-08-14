/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities.search

import player.phonograph.R
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.AlbumDisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.ArtistDisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.PlaylistDisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.SongDisplayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

    private var _viewBinding: FragmentMainActivityRecyclerViewBinding? = null
    private val binding get() = _viewBinding!!

    val viewModel: SearchActivityViewModel by viewModels(ownerProducer = { requireActivity() })

    protected lateinit var adapter: DisplayAdapter<T>

    protected abstract fun createAdapter(activity: AppCompatActivity): DisplayAdapter<T>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentMainActivityRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        adapter = createAdapter(activity as AppCompatActivity)
        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = adapter
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
            flow.collect { data ->
                binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                updateDataset(data)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }
}

class SongSearchResultPageFragment : SearchResultPageFragment<Song>() {

    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Song> =
        SongDisplayAdapter(activity, emptyList(), R.layout.item_list)

    override fun targetFlow(): StateFlow<List<Song>> = viewModel.songs

    override fun updateDataset(newData: List<Song>) {
        adapter.dataset = newData
    }
}

class AlbumSearchResultPageFragment : SearchResultPageFragment<Album>() {
    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Album> =
        AlbumDisplayAdapter(activity, emptyList(), R.layout.item_list)

    override fun targetFlow(): StateFlow<List<Album>> = viewModel.albums

    override fun updateDataset(newData: List<Album>) {
        adapter.dataset = newData
    }
}

class ArtistSearchResultPageFragment : SearchResultPageFragment<Artist>() {
    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Artist> =
        ArtistDisplayAdapter(activity, emptyList(), R.layout.item_list)

    override fun targetFlow(): StateFlow<List<Artist>> = viewModel.artists

    override fun updateDataset(newData: List<Artist>) {
        adapter.dataset = newData
    }
}

class PlaylistSearchResultPageFragment : SearchResultPageFragment<Playlist>() {
    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<Playlist> {
        return PlaylistDisplayAdapter(activity)
    }

    override fun targetFlow(): StateFlow<List<Playlist>> = viewModel.playlists

    override fun updateDataset(newData: List<Playlist>) {
        adapter.dataset = newData
    }
}

class QueueSearchResultPageFragment : SearchResultPageFragment<QueueSong>() {

    override fun createAdapter(activity: AppCompatActivity): DisplayAdapter<QueueSong> =
        QueueSongAdapter(activity, emptyList())

    override fun targetFlow(): StateFlow<List<QueueSong>> = viewModel.songsInQueue

    override fun updateDataset(newData: List<QueueSong>) {
        adapter.dataset = newData
    }

    class QueueSongAdapter(
        activity: FragmentActivity, dataSet: List<QueueSong>,
    ) : DisplayAdapter<QueueSong>(activity, dataSet, R.layout.item_list) {

        init {
            useImageText = true
        }

        override fun getSectionNameImp(position: Int): String {
            return dataset[position].index.toString()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<QueueSong> =
            QueueSongViewHolder(inflatedView(layoutRes, parent))

        inner class QueueSongViewHolder(itemView: View) : DisplayViewHolder<QueueSong>(itemView) {

            override fun getRelativeOrdinalText(item: QueueSong): String {
                return item.index.toString()
            }

            override fun onClick(position: Int, dataset: List<QueueSong>, imageView: ImageView?): Boolean {
                MusicPlayerRemote.playSongAt(dataset[position].index)
                return true
            }

        }
    }

}
