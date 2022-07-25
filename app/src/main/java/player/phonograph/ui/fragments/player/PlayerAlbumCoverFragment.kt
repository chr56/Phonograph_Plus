package player.phonograph.ui.fragments.player

import android.animation.Animator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.*
import lib.phonograph.misc.SimpleAnimatorListener
import player.phonograph.adapter.AlbumCoverPagerAdapter
import player.phonograph.databinding.FragmentPlayerAlbumCoverBinding
import player.phonograph.helper.MusicProgressViewUpdateHelper
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.util.ViewUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayerAlbumCoverFragment :
    AbsMusicServiceFragment(),
    MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: FragmentPlayerAlbumCoverBinding? = null
    private val binding: FragmentPlayerAlbumCoverBinding get() = _viewBinding!!

    private var callbacks: Callbacks? = null
    private var currentPosition = 0
    private var lyrics: LrcLyrics? = null

    private var albumCoverPagerAdapter: AlbumCoverPagerAdapter? = null

    /**[onViewCreated]*/
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
                            callbacks?.let { callbacks ->
                                callbacks.onToolbarToggled()
                                return true
                            }
                            return super.onSingleTapConfirmed(e)
                        }
                    }
                )

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    return gestureDetector.onTouchEvent(event)
                }
            })
        }
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this, 500, 1000).apply { start() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerCoverViewpager.unregisterOnPageChangeCallback(pageChangeListener)
        progressViewUpdateHelper.stop()
        _viewBinding = null
    }

    override fun onServiceConnected() {
        updatePlayingQueue()
    }

    override fun onQueueChanged() {
        updatePlayingQueue()
    }
    override fun onPlayingMetaChanged() {
        handler.sendEmptyMessage(MSG_UPDATE_POSITION)
    }

    /**
     * Set queue to adapter (existing or to crate a new)
     */
    private fun setAdapter(queue: List<Song>) {
        val adapter = albumCoverPagerAdapter
        if (adapter == null) {
            this.albumCoverPagerAdapter =
                AlbumCoverPagerAdapter(this, queue, ::deployColor)
            binding.playerCoverViewpager.adapter = this.albumCoverPagerAdapter
        } else {
            adapter.dataSet = queue
        }
    }

    private fun deployColor(songId: Long, color: Int) {
        if (songId == MusicPlayerRemote.currentSong.id) {
            notifyColorChange(color)
        } else {
            albumCoverPagerAdapter?.requestLoadCover(currentPosition)
        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            MSG_UPDATE_QUEUE -> {
                setAdapter(MusicPlayerRemote.playingQueue)
                binding.playerCoverViewpager.setCurrentItem(MusicPlayerRemote.position, false)
                onPageSelected(MusicPlayerRemote.position)
            }
            MSG_UPDATE_POSITION -> {
                binding.playerCoverViewpager.setCurrentItem(MusicPlayerRemote.position, false)
            }
        }
        false
    }

    private fun updatePlayingQueue() {
        handler.sendEmptyMessage(MSG_UPDATE_QUEUE)
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            this@PlayerAlbumCoverFragment.onPageSelected(position)
        }
    }

    private fun onPageSelected(position: Int) {
        currentPosition = position
        if (position != MusicPlayerRemote.position) {
            MusicPlayerRemote.playSongAt(position)
        }
    }

    fun showHeartAnimation() {
        binding.playerFavoriteIcon.apply {
            clearAnimation()
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
            visibility = View.VISIBLE
            pivotX = width / 2f
            pivotY = height / 2f
            animate()
                .setDuration((ViewUtil.PHONOGRAPH_ANIM_TIME / 2).toLong())
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
                        .setDuration((ViewUtil.PHONOGRAPH_ANIM_TIME / 2).toLong())
                        .setInterpolator(AccelerateInterpolator())
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .start()
                }
                .start()
        }
    }

    private fun isLyricsAvailable(): Boolean = lyrics != null && Setting.instance.synchronizedLyricsShow

    private fun hideLyricsLayout() {
        if (isBindingAccessible() && isVisible) {
            binding.playerLyrics
                .animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
                .withEndAction {
                    if (isBindingAccessible()) {
                        binding.playerLyrics.visibility = View.GONE
                        binding.playerLyricsLine1.text = null
                        binding.playerLyricsLine2.text = null
                    }
                }
        }
    }

    fun setLyrics(l: LrcLyrics) {
        if (Setting.instance.synchronizedLyricsShow && this.isVisible) {
            lyrics = l
            binding.playerLyricsLine1.text = null
            binding.playerLyricsLine2.text = null
            binding.playerLyrics.apply {
                visibility = View.VISIBLE
                animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
            }
        } else {
            clearLyrics()
        }
    }

    fun clearLyrics() {
        lyrics = null
        hideLyricsLayout()
    }

    private fun notifyColorChange(color: Int) {
        callbacks?.onColorChanged(color)
    }

    fun setCallbacks(listener: Callbacks) {
        callbacks = listener
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if (!isBindingAccessible()) return

        if (!isLyricsAvailable()) {
            hideLyricsLayout()
            return
        }

        // Synchronized lyrics begin
        val lyrics = lyrics!!
        binding.playerLyrics.apply {
            visibility = View.VISIBLE
            alpha = 1f
        }

        val oldLine = binding.playerLyricsLine2.text.toString()
        val line = lyrics.getLine(progress).first

        if (oldLine != line || oldLine.isEmpty()) {
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
                animate().alpha(0f).translationY(-height.toFloat()).duration = VISIBILITY_ANIM_DURATION
            }
            binding.playerLyricsLine2.apply {
                alpha = 0f
                translationY = height.toFloat()
                animate().alpha(1f).translationY(0f).duration = VISIBILITY_ANIM_DURATION
            }
        }
    }

    interface Callbacks {
        fun onColorChanged(color: Int)
        fun onFavoriteToggled()
        fun onToolbarToggled()
    }

    private fun isBindingAccessible(): Boolean = _viewBinding != null

    companion object {
        const val VISIBILITY_ANIM_DURATION = 300L

        private const val MSG_UPDATE_QUEUE = 2
        private const val MSG_UPDATE_POSITION = 4
    }
}
