package player.phonograph.ui.activities.base

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import mt.tint.setNavigationBarColor
import player.phonograph.R
import player.phonograph.mechanism.setting.NowPlayingScreenConfig
import player.phonograph.model.NowPlayingScreen
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.settings.SettingFlowStore
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.MiniPlayerFragment
import player.phonograph.ui.fragments.player.card.CardPlayerFragment
import player.phonograph.ui.fragments.player.flat.FlatPlayerFragment
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mt.tint.setTaskDescriptionColor as setTaskDescriptionColorEXt

/**
 * @author Karim Abou Zeid (kabouzeid)
 *
 *
 * Do not use [.setContentView]. Instead wrap your layout with
 * [.wrapSlidingMusicPanel] first and then return it in [.createContentView]
 */
abstract class AbsSlidingMusicPanelActivity :
        AbsMusicServiceActivity(),
        SlidingUpPanelLayout.PanelSlideListener {

    private lateinit var playerFragment: AbsPlayerFragment
    private lateinit var miniPlayerFragment: MiniPlayerFragment

    private var slidingUpPanelLayout: SlidingUpPanelLayout? = null

    private val argbEvaluator = ArgbEvaluator()

    private var playerColor: Int = 0
    protected var activityColor: Int = 0 // original color of this activity
    private var nowPlayingScreenId = -1

    protected abstract fun createContentView(): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())

        // add fragment
        switchNowPlayingScreen(NowPlayingScreenConfig.nowPlayingScreen)

        val flow = SettingFlowStore(this).nowPlayingScreenId
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                flow.distinctUntilChanged().collect {
                    // Log.w("NowPlayingScreen", "nowPlayingScreen $it")
                    if (nowPlayingScreenId >= 0) {
                        whenStarted {
                            switchNowPlayingScreen(NowPlayingScreenConfig.nowPlayingScreen)
                        }
                    }
                    nowPlayingScreenId = it
                }
            }
        }

        playerFragment = supportFragmentManager.findFragmentById(R.id.player_fragment_container) as AbsPlayerFragment
        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.mini_player_fragment) as MiniPlayerFragment
        miniPlayerFragment.requireView().setOnClickListener { expandPanel() }

        playerColor =
            if (playerFragment.paletteColorState.value != 0) playerFragment.paletteColorState.value
            else getColor(R.color.defaultFooterColor)
        activityColor = primaryColor

        // set panel
        slidingUpPanelLayout =
            findViewById<SlidingUpPanelLayout?>(R.id.sliding_layout).also { layout ->
                layout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        when (panelState) {
                            PanelState.EXPANDED  -> {
                                onPanelSlide(layout, 1f)
                                onPanelExpanded(layout)
                            }
                            PanelState.COLLAPSED -> onPanelCollapsed(layout)
                            else                 -> playerFragment.onHide()
                        }
                    }
                })
                layout.addPanelSlideListener(this)
            }

        setupPaletteColorObserver()
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.queue.collect { queue ->
                    hideBottomBar(queue.get()?.isEmpty() ?: false)
                }
            }
        }
    }

    private fun switchNowPlayingScreen(nowPlayingScreen: NowPlayingScreen) {
        supportFragmentManager.apply {
            beginTransaction().replace(
                R.id.player_fragment_container,
                when (nowPlayingScreen) {
                    NowPlayingScreen.FLAT -> FlatPlayerFragment()
                    NowPlayingScreen.CARD -> CardPlayerFragment()
                },
                NOW_PLAYING_FRAGMENT
            ).commit()
            executePendingTransactions()
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

    override fun onPanelSlide(panel: View, @FloatRange(from = 0.0, to = 1.0) slideOffset: Float) {
        setMiniPlayerAlphaProgress(slideOffset)
        cancelThemeColorChange()
        val color: Int =
            argbEvaluator.evaluate(slideOffset, activityColor, playerFragment.paletteColorState.value) as Int
        super.setStatusbarColor(color)
        setNavigationBarColor(color)
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.EXPANDED  -> onPanelExpanded(panel)
            PanelState.ANCHORED  -> collapsePanel() // this fixes a bug where the panel would get stuck for some reason
            else                 -> {}
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
        if (!handleBackPress()) onBackPressedDispatcher.onBackPressed()
    }

    open fun handleBackPress(): Boolean {
        if (slidingUpPanelLayout!!.panelHeight != 0 && playerFragment.onBackPressed()) return true
        if (panelState == PanelState.EXPANDED) {
            collapsePanel()
            return true
        }
        return false
    }

    private fun setupPaletteColorObserver() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                playerFragment.paletteColorState.collect { color ->
                    if (panelState == PanelState.EXPANDED) {
                        animateThemeColorChange(playerColor, color)
                    }
                    playerColor = color
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelThemeColorChange() // just in case
    }

    fun setTaskDescriptionColor(@ColorInt color: Int) {
        when (panelState) {
            PanelState.EXPANDED -> {
                setTaskDescriptionColorEXt(playerFragment.paletteColorState.value)
            }
            else                -> {
                setTaskDescriptionColorEXt(color)
            }
        }
    }

    override val snackBarContainer: View get() = findViewById(R.id.content_container)

    companion object {
        const val NOW_PLAYING_FRAGMENT = "NowPlayingPlayerFragment"
    }
}
