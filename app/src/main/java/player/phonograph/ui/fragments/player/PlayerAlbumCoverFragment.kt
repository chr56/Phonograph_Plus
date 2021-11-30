package player.phonograph.ui.fragments.player

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import player.phonograph.App
import player.phonograph.App.Companion.instance
import player.phonograph.R
import player.phonograph.adapter.AlbumCoverPagerAdapter
import player.phonograph.adapter.AlbumCoverPagerAdapter.AlbumCoverFragment.ColorReceiver
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.MusicProgressViewUpdateHelper
import player.phonograph.misc.SimpleAnimatorListener
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsParsedSynchronized
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.util.PreferenceUtil.Companion.getInstance
import player.phonograph.util.ViewUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayerAlbumCoverFragment :
    AbsMusicServiceFragment(),
    OnPageChangeListener,
    MusicProgressViewUpdateHelper.Callback {

    private lateinit var viewPager: ViewPager /**[onViewCreated]*/
    private lateinit var favoriteIcon: ImageView /**[onViewCreated]*/
    private lateinit var lyricsLayout: FrameLayout /**[onViewCreated]*/

    private lateinit var lyricsLine1: TextView /**[onViewCreated]*/
    private lateinit var lyricsLine2: TextView /**[onViewCreated]*/

    private var callbacks: Callbacks? = null
    private var currentPosition = 0
    private var lyrics: AbsLyrics? = null
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper /**[onViewCreated]*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player_album_cover, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // todo viewBinding
        viewPager = view.findViewById(R.id.player_album_cover_viewpager)
        favoriteIcon = view.findViewById(R.id.player_favorite_icon)
        lyricsLayout = view.findViewById(R.id.player_lyrics)
        lyricsLine1 = view.findViewById(R.id.player_lyrics_line1)
        lyricsLine2 = view.findViewById(R.id.player_lyrics_line2)

        viewPager.addOnPageChangeListener(this)
        viewPager.setOnTouchListener(object : OnTouchListener {
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
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this, 500, 1000).apply { start() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.removeOnPageChangeListener(this)
        progressViewUpdateHelper.stop()
    }

    override fun onServiceConnected() {
        updatePlayingQueue()
    }

    override fun onPlayingMetaChanged() {
        viewPager.currentItem = MusicPlayerRemote.getPosition()
    }

    override fun onQueueChanged() {
        updatePlayingQueue()
    }

    private fun updatePlayingQueue() {
        viewPager.adapter = AlbumCoverPagerAdapter(parentFragmentManager, MusicPlayerRemote.getPlayingQueue())
        viewPager.currentItem = MusicPlayerRemote.getPosition()
        onPageSelected(MusicPlayerRemote.getPosition())
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        currentPosition = position
        (viewPager.adapter as AlbumCoverPagerAdapter).receiveColor(colorReceiver, position)
        if (position != MusicPlayerRemote.getPosition()) {
            MusicPlayerRemote.playSongAt(position)
        }
    }

    private val colorReceiver = ColorReceiver { color, requestCode ->
        if (currentPosition == requestCode) { notifyColorChange(color) }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    fun showHeartAnimation() {
        favoriteIcon.clearAnimation()
        favoriteIcon.alpha = 0f
        favoriteIcon.scaleX = 0f
        favoriteIcon.scaleY = 0f
        favoriteIcon.visibility = View.VISIBLE
        favoriteIcon.pivotX = favoriteIcon.width / 2f
        favoriteIcon.pivotY = favoriteIcon.height / 2f
        favoriteIcon.animate()
            .setDuration((ViewUtil.PHONOGRAPH_ANIM_TIME / 2).toLong())
            .setInterpolator(DecelerateInterpolator())
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setListener(object : SimpleAnimatorListener() {
                override fun onAnimationCancel(animation: Animator) {
                    favoriteIcon.visibility = View.INVISIBLE
                }
            })
            .withEndAction {
                favoriteIcon.animate()
                    .setDuration((ViewUtil.PHONOGRAPH_ANIM_TIME / 2).toLong())
                    .setInterpolator(AccelerateInterpolator())
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .start()
            }
            .start()
    }

    private fun isLyricsLayoutVisible(): Boolean = lyrics != null && getInstance(requireActivity()).synchronizedLyricsShow()

    private fun hideLyricsLayout() {
        lyricsLayout.animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION.toLong())
            .withEndAction {
                lyricsLayout.visibility = View.GONE
                lyricsLine1.text = null
                lyricsLine2.text = null
            }
    }

    fun setLyrics(l: AbsLyrics?) {
        lyrics = l
        if (!isLyricsLayoutVisible()) {
            hideLyricsLayout()
            return
        }
        lyricsLine1.text = null
        lyricsLine2.text = null
        lyricsLayout.visibility = View.VISIBLE
        lyricsLayout.animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION.toLong()
    }

    private fun notifyColorChange(color: Int) { callbacks?.onColorChanged(color) }

    fun setCallbacks(listener: Callbacks) { callbacks = listener }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if ((!isLyricsLayoutVisible()) || (lyrics !is LyricsParsedSynchronized)) {
            hideLyricsLayout()
            return
        }

        // Synchronized lyrics begin
        val lyrics = lyrics as LyricsParsedSynchronized
        lyricsLayout.visibility = View.VISIBLE
        lyricsLayout.alpha = 1f

        val oldLine = lyricsLine2.text.toString()
        val line = lyrics.getLine(progress)

        if (oldLine != line || oldLine.isEmpty()) {

            // for "MIUI StatusBar Lyrics" Xposed module
            if (MusicPlayerRemote.isPlaying()) {
                if (line.isNotEmpty()) {
                    instance.sendBroadcast(
                        Intent().setAction("Lyric_Server")
                            .putExtra("Lyric_Type", "app")
                            .putExtra("Lyric_Data", line)
                            .putExtra("Lyric_PackName", App.PACKAGE_NAME)
                            // Actually, PackName is (music) service name, so we have no suffix (.plus.YOUR_BUILD_TYPE)
                            .putExtra("Lyric_Icon", resources.getString(R.string.icon_base64))
                            .putExtra("Lyric_UseSystemMusicActive", true)
                    )
                } else {
                    instance.sendBroadcast(
                        Intent().setAction("Lyric_Server").putExtra("Lyric_Type", "app_stop")
                    ) // clear, because is null
                }
            }

            lyricsLine1.text = oldLine
            lyricsLine2.text = line
            lyricsLine1.visibility = View.VISIBLE
            lyricsLine2.visibility = View.VISIBLE

            lyricsLine2.measure(
                View.MeasureSpec.makeMeasureSpec(lyricsLine2.measuredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )

            val height = lyricsLine2.measuredHeight

            lyricsLine1.alpha = 1f
            lyricsLine1.translationY = 0f
            lyricsLine1.animate().alpha(0f).translationY(-height.toFloat())
                .duration = VISIBILITY_ANIM_DURATION.toLong()

            lyricsLine2.alpha = 0f
            lyricsLine2.translationY = height.toFloat()
            lyricsLine2.animate().alpha(1f).translationY(0f)
                .duration = VISIBILITY_ANIM_DURATION.toLong()
        }
    }

    interface Callbacks {
        fun onColorChanged(color: Int)
        fun onFavoriteToggled()
        fun onToolbarToggled()
    }

    companion object {
        const val VISIBILITY_ANIM_DURATION = 300
    }
}
