package player.phonograph.ui.activities.base

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.PathInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
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

    private lateinit var currentNowPlayingScreen: NowPlayingScreen
    private var slidingUpPanelLayout: SlidingUpPanelLayout? = null

    private var navigationbarColor = 0
    private var taskColor = 0
    private var lightStatusbar = false

    var playerFragment: AbsPlayerFragment? = null
        private set
    var miniPlayerFragment: MiniPlayerFragment? = null
        private set

    private var navigationBarColorAnimator: ValueAnimator? = null
    private val argbEvaluator = ArgbEvaluator()

    protected abstract fun createContentView(): View // ?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createContentView())

        slidingUpPanelLayout = findViewById(R.id.sliding_layout)
        currentNowPlayingScreen = Setting.instance.nowPlayingScreen

        val fragment: Fragment =
            when (currentNowPlayingScreen) {
                NowPlayingScreen.FLAT -> FlatPlayerFragment()
                NowPlayingScreen.CARD -> CardPlayerFragment()
                else -> CardPlayerFragment()
            } // must implement AbsPlayerFragment

        supportFragmentManager.beginTransaction().replace(R.id.player_fragment_container, fragment, NOW_PLAYING_FRAGMENT).commit()
        supportFragmentManager.executePendingTransactions()

        playerFragment = supportFragmentManager.findFragmentById(R.id.player_fragment_container) as AbsPlayerFragment?

        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.mini_player_fragment) as MiniPlayerFragment?
        miniPlayerFragment!!.requireView().setOnClickListener { expandPanel() }

        slidingUpPanelLayout!!.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                slidingUpPanelLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                when (panelState) {
                    PanelState.EXPANDED -> {
                        onPanelSlide(slidingUpPanelLayout!!, 1f)
                        onPanelExpanded(slidingUpPanelLayout)
                    }
                    PanelState.COLLAPSED -> onPanelCollapsed(slidingUpPanelLayout)
                    else -> playerFragment!!.onHide()
                }
            }
        })
        slidingUpPanelLayout!!.addPanelSlideListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (currentNowPlayingScreen != Setting.instance.nowPlayingScreen) {
            postRecreate()
        }
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
        if (navigationBarColorAnimator != null) navigationBarColorAnimator!!.cancel()
        super.setNavigationbarColor(argbEvaluator.evaluate(slideOffset, navigationbarColor, playerFragment!!.paletteColor) as Int)
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
        super.setLightStatusbar(lightStatusbar)
        super.setTaskDescriptionColor(taskColor)
        super.setNavigationbarColor(navigationbarColor)
        playerFragment!!.setMenuVisibility(false)
        playerFragment!!.userVisibleHint = false
        playerFragment!!.onHide()
    }

    open fun onPanelExpanded(panel: View?) {
        // setting fragments values
        val playerFragmentColor = playerFragment!!.paletteColor
        super.setLightStatusbar(false)
        super.setTaskDescriptionColor(playerFragmentColor)
        super.setNavigationbarColor(playerFragmentColor)
        playerFragment!!.setMenuVisibility(true)
        playerFragment!!.userVisibleHint = true
        playerFragment!!.onShow()
    }

    private fun setMiniPlayerAlphaProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        if (miniPlayerFragment!!.view == null) return
        val alpha = 1 - progress
        miniPlayerFragment!!.requireView().alpha = alpha
        // necessary to make the views below clickable
        miniPlayerFragment!!.requireView().visibility = if (alpha == 0f) View.GONE else View.VISIBLE
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
            slidingUpPanelLayout?.panelHeight = resources.getDimensionPixelSize(R.dimen.mini_player_height)
        }
    }

    protected fun wrapSlidingMusicPanel(@LayoutRes resId: Int): View {
        @SuppressLint("InflateParams") val slidingMusicPanelLayout = layoutInflater.inflate(R.layout.sliding_music_panel_layout, null)
        val contentContainer = slidingMusicPanelLayout.findViewById<ViewGroup>(R.id.content_container)
        layoutInflater.inflate(resId, contentContainer)
        return slidingMusicPanelLayout
    }

    protected fun wrapSlidingMusicPanel(view: View?): View {
        @SuppressLint("InflateParams") val slidingMusicPanelLayout = layoutInflater.inflate(R.layout.sliding_music_panel_layout, null)
        val contentContainer = slidingMusicPanelLayout.findViewById<ViewGroup>(R.id.content_container)
        contentContainer.addView(view)
        //        getLayoutInflater().inflate(resId, contentContainer);
        return slidingMusicPanelLayout
    }

    override fun onBackPressed() {
        if (!handleBackPress()) super.onBackPressed()
    }

    open fun handleBackPress(): Boolean {
        if (slidingUpPanelLayout!!.panelHeight != 0 && playerFragment!!.onBackPressed()) return true
        if (panelState == PanelState.EXPANDED) {
            collapsePanel()
            return true
        }
        return false
    }

    override fun onPaletteColorChanged() {
        if (panelState == PanelState.EXPANDED) {
            val playerFragmentColor = playerFragment!!.paletteColor
            super.setTaskDescriptionColor(playerFragmentColor)
            animateNavigationBarColor(playerFragmentColor)
        }
    }

    override fun setLightStatusbar(enabled: Boolean) {
        lightStatusbar = enabled
        if (panelState == PanelState.COLLAPSED) {
            super.setLightStatusbar(enabled)
        }
    }

    override fun setNavigationbarColor(color: Int) {
        navigationbarColor = color
        if (panelState == PanelState.COLLAPSED) {
            navigationBarColorAnimator?.cancel()
            super.setNavigationbarColor(color)
        }
    }

    private fun animateNavigationBarColor(color: Int) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (navigationBarColorAnimator != null) navigationBarColorAnimator!!.cancel()
        navigationBarColorAnimator = ValueAnimator
            .ofArgb(window.navigationBarColor, color)
            .setDuration(ViewUtil.PHONOGRAPH_ANIM_TIME.toLong())
        navigationBarColorAnimator!!.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
        navigationBarColorAnimator!!.addUpdateListener(
            AnimatorUpdateListener { animation: ValueAnimator ->
                super@AbsSlidingMusicPanelActivity.setNavigationbarColor(
                    (animation.animatedValue as Int)
                )
            }
        )
        navigationBarColorAnimator!!.start()
        //        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (navigationBarColorAnimator != null) navigationBarColorAnimator!!.cancel() // just in case
    }

    override fun setTaskDescriptionColor(@ColorInt color: Int) {
        taskColor = color
        if (panelState == null || panelState == PanelState.COLLAPSED) {
            super.setTaskDescriptionColor(color)
        }
    }

    override val snackBarContainer: View get() = findViewById(R.id.content_container)

    companion object {
        const val NOW_PLAYING_FRAGMENT = "NowPlayingPlayerFragment"
    }
}
