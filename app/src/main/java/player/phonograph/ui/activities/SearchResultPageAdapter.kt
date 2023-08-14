/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.R
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding
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
import kotlinx.coroutines.launch

class SearchResultPageAdapter(
    searchActivity: SearchActivity,
) : FragmentStateAdapter(searchActivity) {

    override fun getItemCount(): Int = TabType.values().size

    override fun createFragment(position: Int): Fragment {
        return ResultFragment(TabType.values()[position])
    }

    class ResultFragment(val type: TabType) : Fragment() {

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
            adapter = SearchResultAdapter(activity)
            with(binding) {
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = adapter
                // noinspection ClickableViewAccessibility
                recyclerView.setOnTouchListener { _, _ ->
                    // hideSoftKeyboard() //todo
                    false
                }
            }
            val flow = flow()
            lifecycleScope.launch {
                flow.collect { data ->
                    binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                    adapter.dataSet = data
                }
            }
        }

        private fun flow() =
            when (type) {
                TabType.SONG     -> viewModel.songs
                TabType.ALBUM    -> viewModel.albums
                TabType.ARTIST   -> viewModel.artists
                TabType.PLAYLIST -> viewModel.playlists
                TabType.QUEUE    -> viewModel.songsInQueue
            }

        override fun onDestroyView() {
            super.onDestroyView()
            _viewBinding = null
        }
    }


    enum class TabType(@StringRes val nameRes: Int) {
        SONG(R.string.song),
        ALBUM(R.string.album),
        ARTIST(R.string.artist),
        PLAYLIST(R.string.playlists),
        QUEUE(R.string.label_playing_queue);
    }

}