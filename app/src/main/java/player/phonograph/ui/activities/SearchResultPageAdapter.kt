/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.R
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            _viewBinding = FragmentMainActivityRecyclerViewBinding.inflate(inflater, container, false)
            return binding.root
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