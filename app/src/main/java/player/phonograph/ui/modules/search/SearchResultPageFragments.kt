/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import player.phonograph.R
import player.phonograph.databinding.RecyclerViewWrappedProperBinding
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.adapter.AlbumBasicDisplayPresenter
import player.phonograph.ui.adapter.ArtistBasicDisplayPresenter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.GenericDisplayAdapter
import player.phonograph.ui.adapter.OrderedItemAdapter
import player.phonograph.ui.adapter.PlaylistBasicDisplayPresenter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
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
    private val adapter: GenericDisplayAdapter<Song>? get() = actualAdapter as? GenericDisplayAdapter<Song>

    override fun createAdapter(activity: AppCompatActivity) =
        GenericDisplayAdapter(activity, SongSearchResultDisplayPresenter)

    override fun targetFlow(): StateFlow<List<Song>> = viewModel.songs

    override fun updateDataset(newData: List<Song>) {
        adapter?.dataset = newData
    }

    object SongSearchResultDisplayPresenter : SongBasicDisplayPresenter(SortMode(SortRef.DISPLAY_NAME)) {
        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST
        override val usePalette: Boolean get() = false
        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE
    }
}

class AlbumSearchResultPageFragment : SearchResultPageFragment<Album>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: GenericDisplayAdapter<Album>? get() = actualAdapter as? GenericDisplayAdapter<Album>

    override fun createAdapter(activity: AppCompatActivity) =
        GenericDisplayAdapter(activity, AlbumSearchResultDisplayPresenter)

    override fun targetFlow(): StateFlow<List<Album>> = viewModel.albums

    override fun updateDataset(newData: List<Album>) {
        adapter?.dataset = newData
    }

    object AlbumSearchResultDisplayPresenter : AlbumBasicDisplayPresenter(SortMode(SortRef.DISPLAY_NAME)) {
        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST
        override val usePalette: Boolean get() = false
        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE
    }
}

class ArtistSearchResultPageFragment : SearchResultPageFragment<Artist>() {
    @Suppress("UNCHECKED_CAST")
    private val adapter: GenericDisplayAdapter<Artist>? get() = actualAdapter as? GenericDisplayAdapter<Artist>

    override fun createAdapter(activity: AppCompatActivity) =
        GenericDisplayAdapter(activity, ArtistSearchResultDisplayPresenter)

    override fun targetFlow(): StateFlow<List<Artist>> = viewModel.artists

    override fun updateDataset(newData: List<Artist>) {
        adapter?.dataset = newData
    }

    object ArtistSearchResultDisplayPresenter : ArtistBasicDisplayPresenter(SortMode(SortRef.DISPLAY_NAME)) {
        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST
        override val usePalette: Boolean get() = false
        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE
    }
}

class PlaylistSearchResultPageFragment : SearchResultPageFragment<Playlist>() {

    @Suppress("UNCHECKED_CAST")
    private val adapter: GenericDisplayAdapter<Playlist>? get() = actualAdapter as? GenericDisplayAdapter<Playlist>

    override fun createAdapter(activity: AppCompatActivity) =
        GenericDisplayAdapter(activity, PlaylistSearchResultDisplayPresenter)


    override fun targetFlow(): StateFlow<List<Playlist>> = viewModel.playlists

    override fun updateDataset(newData: List<Playlist>) {
        adapter?.dataset = newData
    }

    object PlaylistSearchResultDisplayPresenter : PlaylistBasicDisplayPresenter(SortMode(SortRef.DISPLAY_NAME)) {
        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST
        override val usePalette: Boolean = false
        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_FIXED_ICON
        override fun getIconRes(playlist: Playlist): Int = playlist.iconRes
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
                menuButtonView.setOnClickListener {
                    ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = position)
                        .prepareMenu(menuButtonView, item.song)
                }
            }
        }
    }

}
