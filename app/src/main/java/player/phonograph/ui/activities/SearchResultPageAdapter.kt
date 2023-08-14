/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.R
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchResultPageAdapter(
    searchActivity: SearchActivity,
) : FragmentStateAdapter(searchActivity) {

    enum class TabType(@StringRes val nameRes: Int) {
        SONG(R.string.song),
        ALBUM(R.string.album),
        ARTIST(R.string.artist),
        PLAYLIST(R.string.playlists),
        QUEUE(R.string.label_playing_queue);
    }

    override fun getItemCount(): Int = TabType.values().size

    override fun createFragment(position: Int): Fragment {
        return when (TabType.values()[position]) {
            TabType.SONG     -> SongResultFragment()
            TabType.ALBUM    -> AlbumResultFragment()
            TabType.ARTIST   -> ArtistResultFragment()
            TabType.PLAYLIST -> PlaylistResultFragment()
            TabType.QUEUE    -> QueueResultFragment()
        }
    }


    /**
     * Fragment to display result with recycler view
     * **NOTE**: must create from [SearchActivity] (as host activity)
     */
    abstract class ResultFragment : Fragment() {

        private var _viewBinding: FragmentMainActivityRecyclerViewBinding? = null
        private val binding get() = _viewBinding!!

        val viewModel: SearchActivityViewModel by viewModels(ownerProducer = { requireActivity() })

        private lateinit var adapter: SearchResultAdapter

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            _viewBinding = FragmentMainActivityRecyclerViewBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val activity = requireActivity()
            adapter = adapter(activity)
            with(binding) {
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = adapter
                // noinspection ClickableViewAccessibility
                recyclerView.setOnTouchListener { _, _ ->
                    // hideSoftKeyboard() //todo
                    false
                }
            }
            observeData(flow())
        }

        protected open fun adapter(activity: ComponentActivity): SearchResultAdapter = SearchResultAdapter(activity)

        protected abstract fun flow(): StateFlow<List<Any>>

        private fun observeData(flow: StateFlow<List<Any>>) {
            lifecycleScope.launch {
                flow.collect { data ->
                    binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                    adapter.dataSet = data
                }
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _viewBinding = null
        }
    }


    class SongResultFragment : ResultFragment() {
        override fun flow(): StateFlow<List<Any>> = viewModel.songs
    }

    class AlbumResultFragment : ResultFragment() {
        override fun flow(): StateFlow<List<Any>> = viewModel.albums
    }

    class ArtistResultFragment : ResultFragment() {
        override fun flow(): StateFlow<List<Any>> = viewModel.artists
    }

    class PlaylistResultFragment : ResultFragment() {
        override fun flow(): StateFlow<List<Any>> = viewModel.playlists
    }

    class QueueResultFragment : ResultFragment() {
        override fun flow(): StateFlow<List<Any>> = viewModel.songsInQueue
    }

}