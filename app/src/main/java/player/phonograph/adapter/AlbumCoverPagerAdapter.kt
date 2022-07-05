package player.phonograph.adapter

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import lib.phonograph.misc.CustomFragmentStatePagerAdapter
import player.phonograph.adapter.AlbumCoverPagerAdapter.AlbumCoverFragment.ColorReceiver
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.Song
import player.phonograph.settings.Setting

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AlbumCoverPagerAdapter(fm: FragmentManager, dataSet: List<Song>) :
    CustomFragmentStatePagerAdapter(fm) {

    var dataSet = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var currentColorReceiver: ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun getItem(position: Int): Fragment =
        AlbumCoverFragment.newInstance(dataSet[position])

    override fun getCount(): Int = dataSet.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val o = super.instantiateItem(container, position)
        if (currentColorReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentColorReceiver!!, currentColorReceiverPosition)
        }
        return o
    }

    /**
     * Only the latest passed [AlbumCoverFragment.ColorReceiver] is guaranteed to receive a response
     */
    fun receiveColor(colorReceiver: ColorReceiver, position: Int) {
        val fragment = getFragment(position)
        if (fragment != null) {
            currentColorReceiver = null
            currentColorReceiverPosition = -1
            (fragment as AlbumCoverFragment).receiveColor(colorReceiver, position)
        } else {
            currentColorReceiver = colorReceiver
            currentColorReceiverPosition = position
        }
    }

    class AlbumCoverFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

        var _binding: FragmentAlbumCoverBinding? = null
        val binding get() = _binding!!

        private var isColorReady = false
        private var color = 0

        private var song: Song? = null
        private var colorReceiver: ColorReceiver? = null
        private var request = 0

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
            colorReceiver = null
        }

        private fun loadAlbumCover() {
            SongGlideRequest.Builder.from(Glide.with(this), song)
                .checkIgnoreMediaStore(requireActivity())
                .generatePalette(requireActivity()).build()
                .into(object : PhonographColoredTarget(binding.playerImage) {
                    override fun onColorReady(color: Int) {
                        setColor(color)
                    }
                })
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                Setting.FORCE_SQUARE_ALBUM_COVER -> {
                    // todo
                }
            }
        }

        fun forceSquareAlbumCover(forceSquareAlbumCover: Boolean) {
            binding.playerImage.scaleType =
                if (forceSquareAlbumCover) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        }

        private fun setColor(color: Int) {
            this.color = color
            this.isColorReady = true
            colorReceiver?.let {
                it.onColorReady(color, request)
                colorReceiver = null
            }
        }

        fun receiveColor(colorReceiver: ColorReceiver, request: Int) {
            if (isColorReady) {
                colorReceiver.onColorReady(color, request)
            } else {
                this.colorReceiver = colorReceiver
                this.request = request
            }
        }

        interface ColorReceiver {
            fun onColorReady(color: Int, request: Int)
        }

        companion object {
            private const val SONG_ARG = "song"
            fun newInstance(song: Song?): AlbumCoverFragment {
                return AlbumCoverFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(SONG_ARG, song)
                    }
                }
            }
        }
    }
}
