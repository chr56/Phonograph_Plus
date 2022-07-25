package player.phonograph.adapter

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.Song
import player.phonograph.settings.Setting

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AlbumCoverPagerAdapter(
    val fragment: Fragment,
    dataSet: List<Song>,
    var onColorReady: (Long, Int) -> Unit
) :
    FragmentStateAdapter(fragment) {

    var dataSet: List<Song> = dataSet
        set(value) {
            fragments.clear()
            field = value
            notifyDataSetChanged()
        }

    private val fragments = SparseArray<AlbumCoverFragment>(dataSet.size)

    override fun createFragment(position: Int): Fragment =
        AlbumCoverFragment.newInstance(dataSet[position], onColorReady).also {
            fragments.put(
                position,
                it
            )
        }

    override fun getItemCount(): Int = dataSet.size

    /**
     * request load album manually
     */
    fun requestLoadCover(position: Int) {
        fragments[position].loadAlbumCover()
    }

    class AlbumCoverFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var _binding: FragmentAlbumCoverBinding? = null
        val binding get() = _binding!!

        private var isColorReady = false
        private var color = 0

        private var song: Song? = null

        var onColorReadyCallback: ((Long, Int) -> Unit)? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            song = requireArguments().getParcelable(SONG_ARG)
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
            forceSquareAlbumCover(false)
            // forceSquareAlbumCover(Setting.instance.forceSquareAlbumCover);
            // TODO
            Setting.instance.registerOnSharedPreferenceChangedListener(this)
            loadAlbumCover()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
            Setting.instance.unregisterOnSharedPreferenceChangedListener(this)
        }

        fun loadAlbumCover() {
            loadImage(
                requireContext(),
                song ?: return,
                binding.playerImage,
                ::setColor
            )
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                Setting.FORCE_SQUARE_ALBUM_COVER -> {
                    // todo
                }
            }
        }

        internal fun forceSquareAlbumCover(forceSquareAlbumCover: Boolean) {
            binding.playerImage.scaleType =
                if (forceSquareAlbumCover) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        }

        private fun setColor(songId: Long, color: Int) {
            this.color = color
            this.isColorReady = true
            onColorReadyCallback?.let { it(songId, color) }
        }

        companion object {
            private const val SONG_ARG = "song"
            fun newInstance(song: Song?, callback: (Long, Int) -> Unit): AlbumCoverFragment {
                return AlbumCoverFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG_ARG, song)
                    }
                    onColorReadyCallback = callback
                }
            }

            fun loadImage(
                context: Context,
                song: Song,
                target: ImageView,
                colorCallback: (Long, Int) -> Unit
            ) {
                SongGlideRequest.Builder.from(Glide.with(context), song)
                    .checkIgnoreMediaStore(context)
                    .generatePalette(context).build()
                    .into(object : PhonographColoredTarget(target) {
                        override fun onColorReady(color: Int) {
                            colorCallback(song.id, color)
                        }
                    })
            }
        }
    }
}
