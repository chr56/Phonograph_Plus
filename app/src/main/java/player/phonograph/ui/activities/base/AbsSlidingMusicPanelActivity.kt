package player.phonograph.ui.activities.base

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.PathInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.MiniPlayerFragment
import player.phonograph.ui.fragments.player.NowPlayingScreen
import player.phonograph.ui.fragments.player.card.CardPlayerFragment
import player.phonograph.ui.fragments.player.flat.FlatPlayerFragment
import player.phonograph.util.ViewUtil

// TODO: Move smooth AnimateColorChange to ThemeActivity
/**
 * @author Karim Abou Zeid (kabouzeid)
 *
 *
 * Do not use [.setContentView]. Instead wrap your layout with
 * [.wrapSlidingMusicPanel] first and then return it in [.createContentView]
 */
abstract class AbsSlidingMusicPanelActivity :
    AbsMusicServiceActivity(),
    SlidingUpPanelLayout.PanelSlideListener,
    AbsPlayerFragment.Callbacks {

    private lateinit var playerFragment: AbsPlayerFragment
    private lateinit var miniPlayerFragment: MiniPlayerFragment

    private var slidingUpPanelLayout: SlidingUpPanelLayout? = null

    private var colorChangeAnimator: ValueAnimator? = null
    private val argbEvaluator = ArgbEvaluator()

    private var playerColor: Int = 0
    protected var activityColor: Int = 0 // original color of this activity

    protected abstract fun createContentView(): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())

        // add fragment
        supportFragmentManager.apply {
            beginTransaction().replace(
                R.id.player_fragment_container,
                when (Setting.instance.nowPlayingScreen) {
                    NowPlayingScreen.FLAT -> FlatPlayerFragment()
                    NowPlayingScreen.CARD -> CardPlayerFragment()
                },
                NOW_PLAYING_FRAGMENT
            ).commit()
            executePendingTransactions()
        }

        playerFragment = supportFragmentManager.findFragmentById(R.id.player_fragment_container) as AbsPlayerFragment
        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.mini_player_fragment) as MiniPlayerFragment
        miniPlayerFragment.requireView().setOnClickListener { expandPanel() }

        playerColor =
            if (playerFragment.paletteColor != 0) playerFragment.paletteColor
            else getColor(R.color.defaultFooterColor)
        activityColor = primaryColor

        // set panel
        slidingUpPanelLayout =
            findViewById<SlidingUpPanelLayout?>(R.id.sliding_layout).also { layout ->
                layout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        when (panelState) {
                            PanelState.EXPANDED -> {
                                onPanelSlide(layout, 1f)
                                onPanelExpanded(layout)
                            }
                            PanelState.COLLAPSED -> onPanelCollapsed(layout)
                            else -> playerFragment.onHide()
                        }
                    }
                })
                layout.addPanelSlideListener(this)
            }

        // preference
        Setting.instance.registerOnSharedPreferenceChangedListener(
            sharedPreferenceChangeListener
        )
    }

    fun setAntiDragView(antiDragView: View?) {
        slidingUpPanelLayout?.setAntiDragView(antiDragView)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
            slidingUpPanelLayout!!.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    slidingUpPanelLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    hideBottomBar(false)
                }
            })
        } // don't call hideBottomBar(true) here as it causes a bug with the SlidingUpPanelLayout
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        hideBottomBar(MusicPlayerRemote.playingQueue.isEmpty())
    }

    override fun onPanelSlide(panel: View, @FloatRange(from = 0.0, to = 1.0) slideOffset: Float) {
        setMiniPlayerAlphaProgress(slideOffset)
        colorChangeAnimator?.cancel()
        val color: Int =
            argbEvaluator.evaluate(slideOffset, activityColor, playerFragment.paletteColor) as Int
        super.setStatusbarColor(color)
        super.setNavigationbarColor(color)
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.EXPANDED -> onPanelExpanded(panel)
            PanelState.ANCHORED -> collapsePanel() // this fixes a bug where the panel would get stuck for some reason
            else -> {}
        }
    }

    open fun onPanelCollapsed(panel: View?) {
        // restore values
        playerFragment.setMenuVisibility(false)
        playerFragment.userVisibleHint = false
        playerFragment.onHide()
    }

    open fun onPanelExpanded(panel: View?) {
        // setting fragments values
        playerFragment.setMenuVisibility(true)
        playerFragment.userVisibleHint = true
        playerFragment.onShow()
    }

    private fun setMiniPlayerAlphaProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        if (miniPlayerFragment.view == null) return
        val alpha = 1 - progress
        miniPlayerFragment.requireView().alpha = alpha
        // necessary to make the views below clickable
        miniPlayerFragment.requireView().visibility = if (alpha == 0f) View.GONE else View.VISIBLE
    }

    val panelState: PanelState?
        get() = slidingUpPanelLayout?.panelState

    fun collapsePanel() {
        slidingUpPanelLayout?.panelState = PanelState.COLLAPSED
    }

    fun expandPanel() {
        slidingUpPanelLayout?.panelState = PanelState.EXPANDED
    }

    fun hideBottomBar(hide: Boolean) {
        if (hide) {
            slidingUpPanelLayout?.panelHeight = 0
            collapsePanel()
        } else {
            slidingUpPanelLayout?.panelHeight =
                resources.getDimensionPixelSize(R.dimen.mini_player_height)
        }
    }

    protected fun wrapSlidingMusicPanel(view: View?): View {
        @SuppressLint("InflateParams")
        val slidingMusicPanelLayout = layoutInflater.inflate(
            R.layout.sliding_music_panel_layout,
            null
        )
        val contentContainer = slidingMusicPanelLayout.findViewById<ViewGroup>(
            R.id.content_container
        )
        contentContainer.addView(view)
        //        getLayoutInflater().inflate(resId, contentContainer);
        return slidingMusicPanelLayout
    }

    override fun onBackPressed() {
        if (!handleBackPress()) super.onBackPressed()
    }

    open fun handleBackPress(): Boolean {
        if (slidingUpPanelLayout!!.panelHeight != 0 && playerFragment.onBackPressed()) return true
        if (panelState == PanelState.EXPANDED) {
            collapsePanel()
            return true
        }
        return false
    }

    override fun onPaletteColorChanged() {
        if (panelState == PanelState.EXPANDED) {
            animateColorChange(playerColor, playerFragment.paletteColor)
            playerColor = playerFragment.paletteColor
        }
    }

    private fun animateColorChange(oldColor: Int, newColor: Int) {
        colorChangeAnimator?.cancel()
        colorChangeAnimator = ValueAnimator
            .ofArgb(oldColor, newColor)
            .setDuration(ViewUtil.PHONOGRAPH_ANIM_TIME.toLong())
            .also { animator ->
                animator.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
                animator.addUpdateListener { animation: ValueAnimator ->
                    setStatusbarColor(animation.animatedValue as Int)
                    setNavigationbarColor(animation.animatedValue as Int)
                }
                animator.start()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        colorChangeAnimator?.cancel() // just in case
        colorChangeAnimator = null
        // preference
        Setting.instance.unregisterOnSharedPreferenceChangedListener(
            sharedPreferenceChangeListener
        )
    }

    override fun setTaskDescriptionColor(@ColorInt color: Int) {
        when (panelState) {
            PanelState.EXPANDED -> {
                super.setTaskDescriptionColor(playerFragment.paletteColor)
            }
            else -> {
                super.setTaskDescriptionColor(color)
            }
        }
    }

    private val sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Setting.NOW_PLAYING_SCREEN_ID -> recreate()
            }
        }

    override val snackBarContainer: View get() = findViewById(R.id.content_container)

    companion object {
        const val NOW_PLAYING_FRAGMENT = "NowPlayingPlayerFragment"
    }
}
