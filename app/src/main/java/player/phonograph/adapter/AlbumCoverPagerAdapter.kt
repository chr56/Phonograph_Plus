package player.phonograph.adapter

import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.model.Song
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AlbumCoverPagerAdapter(
    val fragment: Fragment,
    dataSet: List<Song>,
) :
    FragmentStateAdapter(fragment) {

    var dataSet: List<Song> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun createFragment(position: Int): Fragment =
        AlbumCoverFragment.newInstance(dataSet[position], ::putColor)

    override fun getItemCount(): Int = dataSet.size

    private val colorCache: LruCache<Song, Int> = LruCache(6)
    internal fun putColor(song: Song, color: Int) {
        colorCache.put(song, color)
    }

    suspend fun getPaletteColor(song: Song): Int {
        return colorCache.get(song)
            ?: AlbumCoverFragment.loadImageForColor(fragment.requireContext(), song, null).second
    }

    class AlbumCoverFragment : Fragment() {

        private var _binding: FragmentAlbumCoverBinding? = null
        val binding get() = _binding!!

        private lateinit var song: Song

        lateinit var onColorReadyCallback: (Song, Int) -> Unit

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
            loadAlbumCover()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        fun loadAlbumCover() {
            loadImage(requireContext(), song, binding.playerImage, onColorReadyCallback)
        }

        internal fun forceSquareAlbumCover(forceSquareAlbumCover: Boolean) {
            binding.playerImage.scaleType =
                if (forceSquareAlbumCover) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        }

        companion object {
            private const val SONG_ARG = "song"
            fun newInstance(song: Song?, callback: (Song, Int) -> Unit): AlbumCoverFragment {
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
                target: ImageView?,
                colorCallback: (Song, Int) -> Unit,
            ) {
                loadImage(context)
                    .from(song)
                    .into(
                        PaletteTargetBuilder(context)
                            .onStart {
                                target?.setImageResource(R.drawable.default_album_art)
                            }
                            .onResourceReady { result, palette ->
                                target?.setImageDrawable(result)
                                colorCallback(song, palette)
                            }
                            .build()
                    )
                    .enqueue()
            }

            @OptIn(ExperimentalCoroutinesApi::class)
            suspend fun loadImageForColor(
                context: Context,
                song: Song,
                target: ImageView?,
            ): Pair<Song, Int> =
                try {
                    withTimeout(1200) {
                        suspendCancellableCoroutine { continuation ->
                            loadImage(context, song, target) { song, color ->
                                continuation.resume(Pair(song, color)) {
                                    cancel()
                                }
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Pair(song, context.getColor(R.color.defaultFooterColor))
                }
        }
    }
}
