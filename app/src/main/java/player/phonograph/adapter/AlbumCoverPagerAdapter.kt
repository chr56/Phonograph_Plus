package player.phonograph.adapter

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.audiocover.AudioFileCover
import player.phonograph.glide.audiocover.AudioFileCoverFetchLogics
import player.phonograph.model.Song
import player.phonograph.settings.Setting
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PhonographColorUtil.generatePalette
import util.mddesign.util.Util

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AlbumCoverPagerAdapter(val fragment: Fragment, dataSet: List<Song>) :
    FragmentStateAdapter(fragment) {

    var dataSet: List<Song> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
            songColorCacheMap.clear()
        }

    private val songColorCacheMap: SparseIntArray = SparseIntArray(1)

    @ColorInt
    fun getColor(songId: Long): Int {
        val cache = songColorCacheMap[songIdToKey(songId)]
        return if (cache != 0) {
            cache
        } else {
            // fetch instantly
            fetchColor(songId)
        }
    }

    internal fun putColorCache(songId: Long, @ColorInt color: Int) =
        songColorCacheMap.put(songIdToKey(songId), color)

    @ColorInt
    private fun fetchColor(songId: Long): Int {
        val color: Int = fetchColorInstantly(songId)
        putColorCache(songId, color)
        return color
    }

    @ColorInt
    private fun fetchColorInstantly(songId: Long): Int {
        if (DEBUG) Log.i("AlbumCoverPagerAdapter", "directly fetching palette!!")
        val palette = runCatching {
            val song = dataSet.first { it.id == songId }
            val resultStream = AudioFileCoverFetchLogics(AudioFileCover(song.data)).fetch(null)
            val bitmap =
                Bitmap.createScaledBitmap(BitmapFactory.decodeStream(resultStream), 512, 512, true)
            return@runCatching generatePalette(bitmap)
        }.getOrNull()

        return PhonographColorUtil.getColor(
            palette,
            Util.resolveColor(fragment.requireContext(), R.attr.defaultFooterColor)
        )
    }

    override fun createFragment(position: Int): Fragment =
        AlbumCoverFragment.newInstance(dataSet[position], ::putColorCache)

    override fun getItemCount(): Int = dataSet.size

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

        private fun loadAlbumCover() {
            val song = song ?: return
            SongGlideRequest.Builder.from(Glide.with(this), song)
                .checkIgnoreMediaStore(requireActivity())
                .generatePalette(requireActivity()).build()
                .into(object : PhonographColoredTarget(binding.playerImage) {
                    override fun onColorReady(color: Int) {
                        setColor(song.id, color)
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
        }
    }
    companion object {
        private fun songIdToKey(songId: Long): Int = (songId % Int.MAX_VALUE).toInt()
    }
}
