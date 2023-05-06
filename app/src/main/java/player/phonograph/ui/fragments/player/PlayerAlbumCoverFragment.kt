package player.phonograph.ui.fragments.player

import lib.phonograph.misc.SimpleAnimatorListener
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.databinding.FragmentPlayerAlbumCoverBinding
import player.phonograph.misc.MusicProgressViewUpdateHelperDelegate
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenResumed
import androidx.lifecycle.whenStarted
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import kotlinx.coroutines.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayerAlbumCoverFragment :
        AbsMusicServiceFragment() {

    private var _viewBinding: FragmentPlayerAlbumCoverBinding? = null
    private val binding: FragmentPlayerAlbumCoverBinding get() = _viewBinding!!

    private val viewModel: AlbumCoverViewModel by viewModels()
    private val playerViewModel: PlayerFragmentViewModel by viewModels({ requireParentFragment() })

    private var albumCoverPagerAdapter: AlbumCoverPagerAdapter? = null

    private val progressViewUpdateHelperDelegate =
        MusicProgressViewUpdateHelperDelegate(::updateProgressViews)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(progressViewUpdateHelperDelegate)
    }

    private fun observeState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.queue.collect {
                    updateAdapter()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.shuffleMode.collect {
                    updateAdapter()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.position.collect { position ->
                    refreshCurrentPosition(position)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                playerViewModel.lyrics.collect {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                        Setting.instance.synchronizedLyricsShow
                    ) {
                        resetLyricsLayout()
                    } else {
                        hideLyricsLayout()
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                playerViewModel.favoriteState.collect { newState ->
                    if (newState.first == lastFavoriteState.first && newState.second && !lastFavoriteState.second)
                        showHeartAnimation()
                    lastFavoriteState = newState
                }
            }
        }
    }

    private var lastFavoriteState: Pair<Song, Boolean> = Song.EMPTY_SONG to false

    private suspend fun resetLyricsLayout() {
        lifecycle.whenResumed {
            withContext(Dispatchers.Main) {
                binding.playerLyricsLine1.text = null
                binding.playerLyricsLine2.text = null
                binding.playerLyrics.apply {
                    visibility = View.VISIBLE
                    animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
                }
            }
        }
    }

    private suspend fun hideLyricsLayout() {
        lifecycle.whenResumed {
            withContext(Dispatchers.Main) {
                binding.playerLyrics
                    .animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
                    .withEndAction {
                        lifecycleScope.launch {
                            lifecycle.whenResumed {
                                binding.playerLyrics.visibility = View.GONE
                                binding.playerLyricsLine1.text = null
                                binding.playerLyricsLine2.text = null
                            }
                        }
                    }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentPlayerAlbumCoverBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playerCoverViewpager.apply {
            registerOnPageChangeCallback(pageChangeListener)
            setOnTouchListener(object : OnTouchListener {
                val gestureDetector = GestureDetector(
                    activity,
                    object : SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            return playerViewModel.toggleToolbar()
                        }
                    }
                )

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    return gestureDetector.onTouchEvent(event)
                }
            })
            offscreenPageLimit = 1
        }
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerCoverViewpager.unregisterOnPageChangeCallback(pageChangeListener)
        _viewBinding = null
    }

    private suspend fun updateAdapter() {
        lifecycle.whenStarted {
            val queue = MusicPlayerRemote.playingQueue
            val position = CurrentQueueState.position.value
            albumCoverPagerAdapter = AlbumCoverPagerAdapter(this@PlayerAlbumCoverFragment, queue)
            binding.playerCoverViewpager.adapter = albumCoverPagerAdapter
            refreshCurrentPosition(position)
        }
    }

    private fun refreshCurrentPosition(position: Int) {
        if (position >= 0) {
            binding.playerCoverViewpager.setCurrentItem(MusicPlayerRemote.position, false)
            refreshPaletteColor(position)
        }
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position != MusicPlayerRemote.position) {
                MusicPlayerRemote.playSongAt(position)
            }
        }
    }

    private fun refreshPaletteColor(position: Int) {
        val adapter = albumCoverPagerAdapter
        if (adapter != null) {
            lifecycleScope.launch(Dispatchers.Default) {
                val song = adapter.dataSet.getOrElse(position) { return@launch }
                val color = viewModel.getPaletteColor(requireContext(), song)
                playerViewModel.updatePaletteColor(color)
            }
        }
    }

    private fun updateProgressViews(progress: Int, total: Int) {
        lifecycleScope.launch(Dispatchers.Unconfined) {
            updateLyrics(progress)
        }
    }

    private suspend fun updateLyrics(progress: Int) {
        lifecycle.whenResumed {
            val lyrics = playerViewModel.lyrics.value
            if (lyrics != null) {
                binding.playerLyrics.apply {
                    visibility = View.VISIBLE
                    alpha = 1f
                }
                val oldLine = binding.playerLyricsLine2.text.toString()
                val line = lyrics.getLine(progress).first

                if (oldLine != line || oldLine.isEmpty()) {
                    updateLyricsImpl(oldLine, line)
                }
            } else {
                hideLyricsLayout()
            }
        }
    }

    private fun updateLyricsImpl(oldLine: String, line: String) {
        binding.playerLyricsLine1.text = oldLine
        binding.playerLyricsLine2.text = line
        binding.playerLyricsLine1.visibility = View.VISIBLE
        binding.playerLyricsLine2.visibility = View.VISIBLE

        binding.playerLyricsLine2.measure(
            View.MeasureSpec.makeMeasureSpec(
                binding.playerLyricsLine2.measuredWidth,
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.UNSPECIFIED
        )

        val height = binding.playerLyricsLine2.measuredHeight

        binding.playerLyricsLine1.apply {
            alpha = 1f
            translationY = 0f
            animate().alpha(0f).translationY(-height.toFloat()).duration =
                VISIBILITY_ANIM_DURATION
        }
        binding.playerLyricsLine2.apply {
            alpha = 0f
            translationY = height.toFloat()
            animate().alpha(1f).translationY(0f).duration = VISIBILITY_ANIM_DURATION
        }
    }

    private fun showHeartAnimation() {
        binding.playerFavoriteIcon.apply {
            clearAnimation()
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
            visibility = View.VISIBLE
            pivotX = width / 2f
            pivotY = height / 2f
            animate()
                .setDuration(PHONOGRAPH_ANIM_TIME / 2)
                .setInterpolator(DecelerateInterpolator())
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationCancel(animation: Animator) {
                        visibility = View.INVISIBLE
                    }
                })
                .withEndAction {
                    animate()
                        .setDuration(PHONOGRAPH_ANIM_TIME / 2)
                        .setInterpolator(AccelerateInterpolator())
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .start()
                }
                .start()
        }
    }

    companion object {
        const val VISIBILITY_ANIM_DURATION = 300L
    }
}


class AlbumCoverViewModel : ViewModel() {
    private val colorCache: LruCache<Song, PaletteBitmap> = LruCache(6)

    private fun putColor(song: Song, bitmap: Bitmap, color: Int) {
        colorCache.put(song, PaletteBitmap(bitmap, color))
    }

    private fun getPaletteColorFromCache(song: Song) = colorCache[song]?.paletteColor
    private fun getImageFromCache(song: Song) = colorCache[song]?.bitmap

    suspend fun getPaletteColor(context: Context, song: Song): Int {
        val cached = getPaletteColorFromCache(song)
        return if (cached == null) {
            val loaded = loadImage(context, song)
            putColor(song, loaded.bitmap, loaded.paletteColor)
            loaded.paletteColor
        } else {
            cached
        }
    }

    suspend fun getImage(context: Context, song: Song): Bitmap {
        val cached = getImageFromCache(song)
        return if (cached == null) {
            val loaded = loadImage(context, song)
            putColor(song, loaded.bitmap, loaded.paletteColor)
            loaded.bitmap
        } else {
            cached
        }
    }
}

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
