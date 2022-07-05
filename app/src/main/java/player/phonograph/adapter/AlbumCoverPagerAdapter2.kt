/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter

import android.os.Bundle
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.Song

class AlbumCoverPagerAdapter2(
    dataSet: ArrayList<Song>,
    fragmentManager: FragmentManager,
    hostFragment: Fragment,
    var onColorReadyCallback: ((color: Int, songId: Long) -> Unit)?
) :
    FragmentStateAdapter(fragmentManager, hostFragment.lifecycle) {

    var dataset: MutableList<Song> = dataSet
        set(value) {
            if (value.size > 3) throw IllegalArgumentException("No more than 3 songs")
            field = value
            coversFragment.clear()
            notifyDataSetChanged()
        }
    override fun getItemCount(): Int = 3

    var coversFragment: MutableMap<Int, AlbumCoverFragment> = ArrayMap(3)

    override fun createFragment(position: Int): Fragment {
        return AlbumCoverFragment.newInstance(dataset[position]).also { newFragment ->
            coversFragment[position] = newFragment
            newFragment.onColorReady = { color: Int, id: Long ->
                onColorReadyCallback?.invoke(color, id)
            }
        }
    }

    // TODO SquareAlbumCover
    class AlbumCoverFragment : Fragment() {

        lateinit var song: Song
        var isColorReady: Boolean = false
            private set
        var color: Int = -1
            private set

        var onColorReady: ((color: Int, songId: Long) -> Unit)? = null

        private var _binding: FragmentAlbumCoverBinding? = null
        private val binding get() = _binding!!

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            song = requireArguments().getParcelable(SONG)!!
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentAlbumCoverBinding.inflate(LayoutInflater.from(context))
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            loadCover()
        }

        private fun loadCover() {
            SongGlideRequest.Builder.from(Glide.with(this), song)
                .checkIgnoreMediaStore(requireActivity())
                .generatePalette(requireActivity()).build()
                .into(object : PhonographColoredTarget(binding.playerImage) {
                    override fun onColorReady(color: Int) {
                        this@AlbumCoverFragment.color = color
                        this@AlbumCoverFragment.isColorReady = true
                        onColorReady?.invoke(color, song.id)
                    }
                })
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        companion object {
            private const val SONG = "song"
            fun newInstance(song: Song): AlbumCoverFragment {
                return AlbumCoverFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG, song)
                    }
                }
            }
        }
    }
}
