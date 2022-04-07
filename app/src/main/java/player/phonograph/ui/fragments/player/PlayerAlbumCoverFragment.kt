package player.phonograph.ui.fragments.player

import android.animation.Animator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import player.phonograph.adapter.AlbumCoverPagerAdapter
import player.phonograph.adapter.AlbumCoverPagerAdapter.AlbumCoverFragment.ColorReceiver
import player.phonograph.databinding.FragmentPlayerAlbumCoverBinding
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.MusicProgressViewUpdateHelper
import player.phonograph.misc.SimpleAnimatorListener
import player.phonograph.model.lyrics2.AbsLyrics
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.util.ViewUtil
import java.lang.IllegalStateException

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayerAlbumCoverFragment :
    AbsMusicServiceFragment(),
    OnPageChangeListener,
    MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: FragmentPlayerAlbumCoverBinding? = null
    private val binding: FragmentPlayerAlbumCoverBinding get() = _viewBinding!!

    private var callbacks: Callbacks? = null
    private var currentPosition = 0
    private var lyrics: LrcLyrics? = null

    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    /**[onViewCreated]*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentPlayerAlbumCoverBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playerCoverViewpager.apply {
            addOnPageChangeListener(this@PlayerAlbumCoverFragment)
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
        binding.playerCoverViewpager.removeOnPageChangeListener(this)
        progressViewUpdateHelper.stop()
        _viewBinding = null
    }

    override fun onServiceConnected() {
        updatePlayingQueue()
    }

    override fun onPlayingMetaChanged() {
        binding.playerCoverViewpager.currentItem = MusicPlayerRemote.getPosition()
    }

    override fun onQueueChanged() {
        updatePlayingQueue()
    }

    private fun updatePlayingQueue() {
        binding.playerCoverViewpager.adapter = AlbumCoverPagerAdapter(parentFragmentManager, MusicPlayerRemote.getPlayingQueue())
        binding.playerCoverViewpager.currentItem = MusicPlayerRemote.getPosition()
        onPageSelected(MusicPlayerRemote.getPosition())
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        currentPosition = position
        (binding.playerCoverViewpager.adapter as AlbumCoverPagerAdapter).receiveColor(colorReceiver, position)
        if (position != MusicPlayerRemote.getPosition()) {
            MusicPlayerRemote.playSongAt(position)
        }
    }

    private val colorReceiver by lazy(LazyThreadSafetyMode.NONE) {
        object : ColorReceiver {
            override fun onColorReady(color: Int, request: Int) {
                if (currentPosition == request) {
                    notifyColorChange(color)
                }
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

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

    private fun isLyricsLayoutVisible(): Boolean = lyrics != null && Setting.instance.synchronizedLyricsShow

    private fun hideLyricsLayout() {
        binding.playerLyrics
            .animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
            .withEndAction {
                if (_viewBinding != null) {
                    binding.playerLyrics.visibility = View.GONE
                    binding.playerLyricsLine1.text = null
                    binding.playerLyricsLine2.text = null
                } else {
                    ErrorNotification.init()
                    ErrorNotification.postErrorNotification(
                        IllegalStateException("_viewBinding of PlayerAlbumCoverFragment is null."),
                        "This may happen when playing lyrics animation after Fragment destroying already.\n${
                        Thread.currentThread().stackTrace.map { it.toString() }.fold("StackTrace:\n") { acc: String, s: String -> "$acc \n$s" }
                        }"
                    )
                }
            }
    }

    fun setLyrics(l: AbsLyrics) {
        if (l is LrcLyrics && Setting.instance.synchronizedLyricsShow) {
            lyrics = l
            binding.playerLyricsLine1.text = null
            binding.playerLyricsLine2.text = null
            binding.playerLyrics.apply {
                visibility = View.VISIBLE
                animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
            }
            return
        } else {
            lyrics = null
            hideLyricsLayout()
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
        if (!isLyricsLayoutVisible()) {
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
        val line = lyrics.getLine(progress)

        if (oldLine != line || oldLine.isEmpty()) {

            binding.playerLyricsLine1.text = oldLine
            binding.playerLyricsLine2.text = line
            binding.playerLyricsLine1.visibility = View.VISIBLE
            binding.playerLyricsLine2.visibility = View.VISIBLE

            binding.playerLyricsLine2.measure(
                View.MeasureSpec.makeMeasureSpec(binding.playerLyricsLine2.measuredWidth, View.MeasureSpec.EXACTLY),
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

    companion object {
        const val VISIBILITY_ANIM_DURATION = 300L
    }
}
