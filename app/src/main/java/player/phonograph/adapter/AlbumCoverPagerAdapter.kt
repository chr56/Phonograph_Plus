package player.phonograph.adapter

import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.model.Song
import player.phonograph.ui.fragments.player.AlbumCoverViewModel
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AlbumCoverPagerAdapter(
    val fragment: Fragment,
    dataSet: List<Song>,
) : FragmentStateAdapter(fragment) {

    var dataSet: List<Song> = dataSet
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }


    override fun createFragment(position: Int): Fragment = coverFragment(dataSet[position])

    override fun getItemCount(): Int = dataSet.size

    private fun coverFragment(song: Song): AlbumCoverFragment =
        AlbumCoverFragment().apply {
            arguments = Bundle().apply {
                putParcelable(SONG_ARG, song)
            }
        }

    class AlbumCoverFragment : Fragment() {

        private var _binding: FragmentAlbumCoverBinding? = null
        val binding get() = _binding!!

        private val viewModel: AlbumCoverViewModel by viewModels({ requireParentFragment() })

        private lateinit var song: Song

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            song = requireArguments().getParcelable(SONG_ARG)!!
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            _binding = FragmentAlbumCoverBinding.inflate(LayoutInflater.from(context))
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            forceSquareAlbumCover(false)
            lifecycleScope.launch {
                val bitmap = viewModel.getImage(requireContext(), song)
                withContext(Dispatchers.Main) {
                    binding.playerImage.setImageBitmap(bitmap)
                }
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        private fun forceSquareAlbumCover(forceSquareAlbumCover: Boolean) {
            binding.playerImage.scaleType =
                if (forceSquareAlbumCover) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        }
    }

    companion object {
        private const val SONG_ARG = "song"
    }
}
