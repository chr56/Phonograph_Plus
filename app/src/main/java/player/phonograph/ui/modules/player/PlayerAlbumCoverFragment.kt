package player.phonograph.ui.modules.player

import coil.request.Disposable
import coil.request.Parameters
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.PARAMETERS_KEY_PALETTE
import player.phonograph.coil.PARAMETERS_KEY_QUICK_CACHE
import player.phonograph.coil.loadImage
import player.phonograph.coil.palette.PaletteColorTarget
import player.phonograph.databinding.FragmentAlbumCoverBinding
import player.phonograph.databinding.FragmentPlayerAlbumCoverBinding
import player.phonograph.foundation.compat.parcelable
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.component.MusicProgressUpdateDelegate
import player.phonograph.util.observe
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.SimpleAnimatorListener
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayerAlbumCoverFragment :
        AbsMusicServiceFragment() {

    private var _viewBinding: FragmentPlayerAlbumCoverBinding? = null
    private val binding: FragmentPlayerAlbumCoverBinding get() = _viewBinding!!

    private val playerViewModel: PlayerFragmentViewModel by viewModels({ requireParentFragment() })
    private val lyricsViewModel: LyricsViewModel by viewModels({ requireActivity() })
    private val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    private var albumCoverPagerAdapter: AlbumCoverPagerAdapter? = null

    private val musicProgressUpdateDelegate =
        MusicProgressUpdateDelegate(::onUpdateProgress)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(musicProgressUpdateDelegate)
        defaultColor = themeFooterColor(requireContext())
    }

    private fun observeState() {
        observe(queueViewModel.queue) { queue ->
            val position = MusicPlayerRemote.position
            refreshAdapter(queue, position)
        }
        observe(queueViewModel.shuffleMode) {
            val queue = MusicPlayerRemote.playingQueue
            val position = MusicPlayerRemote.position
            refreshAdapter(queue, position)
        }
        observe(queueViewModel.position) { position ->
            val songs = albumCoverPagerAdapter?.dataSet
            if (songs != null) {
                if (position in songs.indices) {
                    updateCurrentItemPosition(position)
                    updateCurrentPaletteColor(songs[position])
                }
            }
        }
        observe(lyricsViewModel.lyricsInfo) {
            val lyricsShow = Setting(App.instance)[Keys.synchronizedLyricsShow].data
            withContext(Dispatchers.Main) {
                if (lyricsShow) {
                    resetLyricsLayout()
                } else {
                    hideLyricsLayout()
                }
            }
        }
        observe(playerViewModel.favoriteState) { newState ->
            if (newState.first == lastFavoriteState.first && newState.second && !lastFavoriteState.second)
                showHeartAnimation()
            lastFavoriteState = newState
        }
    }

    private var lastFavoriteState: Pair<Song?, Boolean> = null to false

    @MainThread
    private suspend fun resetLyricsLayout() {
        lifecycle.withStarted {
            binding.playerLyricsLine1.text = null
            binding.playerLyricsLine2.text = null
            binding.playerLyrics.apply {
                visibility = View.VISIBLE
                animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
            }
        }
    }

    @MainThread
    private suspend fun hideLyricsLayout() {
        lifecycle.withStarted {
            binding.playerLyrics
                .animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
                .withEndAction {
                    lifecycleScope.launch {
                        lifecycle.withResumed {
                            binding.playerLyrics.visibility = View.GONE
                            binding.playerLyricsLine1.text = null
                            binding.playerLyricsLine2.text = null
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

    private suspend fun refreshAdapter(queue: List<Song>, position: Int) {
        lifecycle.withCreated {
            val adapter = AlbumCoverPagerAdapter(this@PlayerAlbumCoverFragment, queue)
            albumCoverPagerAdapter = adapter
            binding.playerCoverViewpager.adapter = adapter
            if (position >= 0 && position in queue.indices) {
                updateCurrentItemPosition(position)
                updateCurrentPaletteColor(queue[position])
            }
        }
    }

    private fun updateCurrentItemPosition(position: Int) {
        binding.playerCoverViewpager.setCurrentItem(position, false)
    }

    private fun updateCurrentPaletteColor(song: Song?) {
        refreshPaletteColor(requireContext(), song)
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position != MusicPlayerRemote.position) {
                MusicPlayerRemote.playSongAt(position)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUpdateProgress(progress: Int, total: Int) {
        lifecycleScope.launch {
            updateLyrics(progress)
        }
    }

    private suspend fun updateLyrics(progress: Int) {
        val lyrics = lyricsViewModel.lyricsInfo.value?.activatedLyrics
        if (lyrics != null && lyrics is LrcLyrics) {
            lifecycle.withResumed {
                binding.playerLyrics.apply {
                    visibility = View.VISIBLE
                    alpha = 1f
                }
                val oldLine = binding.playerLyricsLine2.text.toString()
                val line = lyrics.getLine(progress).first

                if (oldLine != line || oldLine.isEmpty()) {
                    updateLyricsImpl(oldLine, line)
                }
            }
        } else {
            hideLyricsLayout()
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

    private var disposable: Disposable? = null
    private fun refreshPaletteColor(context: Context, song: Song?) {
        if (song != null && song.data.isNotEmpty()) {
            disposable?.dispose()
            disposable = loadImage(context)
                .from(song)
                .parameters(
                    Parameters.Builder()
                        .set(PARAMETERS_KEY_PALETTE, true)
                        .set(PARAMETERS_KEY_QUICK_CACHE, true)
                        .build()
                )
                .into(
                    PaletteColorTarget(
                        error = { _, color ->
                            panelViewModel.updateHighlightColor(color)
                        },
                        success = { _, color ->
                            panelViewModel.updateHighlightColor(color)
                        },
                        defaultColor = defaultColor,
                    )
                )
                .enqueue()
        } else {
            panelViewModel.updateHighlightColor(defaultColor)
        }
    }

    var defaultColor: Int = -1

    companion object {
        const val VISIBILITY_ANIM_DURATION = 300L
    }
}

private class AlbumCoverPagerAdapter(
    fragment: Fragment,
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

        private lateinit var song: Song
        private var disposable: Disposable? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            song = requireArguments().parcelable(SONG_ARG)!!
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
            disposable = loadImage(view.context)
                .from(song)
                .parameters(
                    Parameters.Builder()
                        .set(PARAMETERS_KEY_PALETTE, true)
                        .set(PARAMETERS_KEY_QUICK_CACHE, true)
                        .build()
                )
                .default(R.drawable.default_album_art)
                .into(binding.playerImage)
                .enqueue()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            disposable?.dispose()
            _binding = null
        }

        private fun forceSquareAlbumCover(@Suppress("SameParameterValue") forceSquareAlbumCover: Boolean) {
            binding.playerImage.scaleType =
                if (forceSquareAlbumCover) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
        }
    }

    companion object {
        private const val SONG_ARG = "song"
    }
}
