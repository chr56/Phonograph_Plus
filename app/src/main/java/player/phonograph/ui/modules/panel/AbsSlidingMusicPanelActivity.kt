package player.phonograph.ui.modules.panel

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import player.phonograph.R
import player.phonograph.databinding.SlidingMusicPanelLayoutBinding
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.model.ui.PanelAction
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.modules.player.MiniPlayerFragment
import player.phonograph.ui.modules.player.style.buildPlayerFragment
import player.phonograph.ui.theme.SystemBarsControllerDelegate
import player.phonograph.ui.theme.SystemBarsControllerDelegate.translucentScrim
import player.phonograph.ui.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.ui.theme.themeFooterColor
import player.phonograph.ui.util.SlidingUpPanelSwitchHelper
import player.phonograph.ui.util.isOrientationLandscape
import player.phonograph.ui.util.observe
import util.theme.color.darkenColor
import androidx.activity.OnBackPressedCallback
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.PathInterpolator
import kotlinx.coroutines.launch

/**
 *
 *
 * Do not use [setContentView]. Instead, wrap your layout with
 * [wrapSlidingMusicPanel] first and then return it in [createContentView]
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsSlidingMusicPanelActivity :
        AbsMusicServiceActivity(),
        UnarySlidingUpPanelProvider,
        SlidingUpPanelLayout.PanelSlideListener {

    private var playerFragment: AbsPlayerFragment? = null
    private var miniPlayerFragment: MiniPlayerFragment? = null
    private var miniPlayerHeight: Int = 0

    private var _panelBinding: SlidingMusicPanelLayoutBinding? = null
    private val panelBinding: SlidingMusicPanelLayoutBinding get() = _panelBinding!!

    private var bottomNavigationBarHeight: Int = 0

    private val slidingUpPanelLayout: SlidingUpPanelLayout get() = panelBinding.slidingLayout

    val panelViewModel: PanelViewModel by viewModel { parametersOf(primaryColor(), themeFooterColor(this)) }

    /**
     * See [wrapSlidingMusicPanel]
     */
    protected abstract fun createContentView(): View

    /**
     * create the actual view (wrapped with panel layout)
     * @param view the "main" view to be wrapped
     * @return actual view that should be the "root" view for [setContentView]
     */
    protected fun wrapSlidingMusicPanel(view: View?): View {
        _panelBinding =
            SlidingMusicPanelLayoutBinding.inflate(layoutInflater, null, false).also { binding ->
                binding.contentContainer.addView(view)
            }
        return panelBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setup panel
        setContentView(createContentView())
        miniPlayerFragment = panelBinding.miniPlayerFragment.getFragment()
        miniPlayerHeight = resources.getDimensionPixelSize(R.dimen.mini_player_height)
        bottomNavigationBarHeight = resources.getDimensionPixelSize(R.dimen.navigation_bar_height)
        panelBinding.slidingLayout.also { layout ->
            layout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    when (slidingUpPanelLayout.panelState) {
                        PanelState.EXPANDED  -> {
                            onPanelSlide(layout, 1f)
                            onPanelExpanded(layout)
                        }

                        PanelState.COLLAPSED -> onPanelCollapsed(layout)
                        else                 -> playerFragment?.onHide()
                    }
                }
            })
            layout.addPanelSlideListener(this)
        }
        panelBinding.navigationBar.setOnClickListener {
            lifecycleScope.launch { panelViewModel.expandPanel() }
        }

        // add fragment
        val playerStyle = Settings(this)[Keys.nowPlayingScreenStyle].flow
        observe(playerStyle, distinctive = true) { screen ->
            setupPlayerFragment(screen)
        }


        // insets
        ViewCompat.setOnApplyWindowInsetsListener(panelBinding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            bottomNavigationBarHeight = insets.bottom
            updatePanelHiddenState(panelViewModel.isMiniPlayerHidden.value)
            view.updateLayoutParams<MarginLayoutParams> {
                rightMargin = insets.right
                leftMargin = insets.left
            }
            windowInsets
        }

        // states
        SystemBarsControllerDelegate.updateSystemBarsColor(this, darkenColor(primaryColor()), primaryColor())
        observe(queueViewModel.queue) { queue -> panelViewModel.updateMiniPlayerVisibility(hidden = queue.isEmpty()) }
        observe(panelViewModel.colorChange) { (oldColor, newColor) ->
            if (slidingUpPanelLayout.panelState == PanelState.EXPANDED) {
                animateSystemBarsColor(oldColor, newColor)
            }
        }
        observe(panelViewModel.isMiniPlayerHidden, state = Lifecycle.State.STARTED) { hidden ->
            updatePanelHiddenState(hidden)
        }
        observe(panelViewModel.playerPanelEffects, state = Lifecycle.State.STARTED) { action ->
            when (action) {
                PanelAction.Collapse -> panelSwitcher.collapse(slidingUpPanelLayout)
                PanelAction.Expand   -> panelSwitcher.expand(slidingUpPanelLayout)
                PanelAction.Toggle   -> panelSwitcher.toggle(slidingUpPanelLayout)
            }
        }
    }

    override fun onDestroy() {
        cancelSystemBarsColorAnimation() // just in case
        super.onDestroy()
    }

    private fun setupPlayerFragment(style: NowPlayingScreenStyle) {
        val fragment = buildPlayerFragment(style)
        supportFragmentManager.apply {
            commit {
                replace(R.id.player_fragment_container, fragment, NOW_PLAYING_FRAGMENT)
            }
            executePendingTransactions()
        }
        playerFragment = fragment
    }

    //region PanelSlideListener
    override fun onPanelSlide(panel: View, @FloatRange(from = 0.0, to = 1.0) slideOffset: Float) {
        setMiniPlayerFadingProgress(slideOffset)
        cancelSystemBarsColorAnimation()
        val from = panelViewModel.activityColor.value
        val to = panelViewModel.highlightColor.value
        val statusbarColor: Int = argbEvaluator.evaluate(slideOffset, from, to) as Int
        val navigationbarColor: Int =
            if (panelViewModel.isMiniPlayerHidden.value && isOrientationLandscape(resources)) {
                translucentScrim
            } else {
                argbEvaluator.evaluate(slideOffset, from, to) as Int
            }
        SystemBarsControllerDelegate.updateSystemBarsColor(this, statusbarColor, navigationbarColor)
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.EXPANDED  -> onPanelExpanded(panel)
            PanelState.ANCHORED  -> panelSwitcher.collapse(slidingUpPanelLayout) // avoid getting stuck for some reason
            else                 -> {}
        }
    }
    //endregion

    @Suppress("DEPRECATION")
    open fun onPanelCollapsed(panel: View?) {
        // restore values
        playerFragment?.setMenuVisibility(false)
        playerFragment?.userVisibleHint = false // todo: remove legacy userVisibleHint
        playerFragment?.onHide()
        panelBackPressedCallback.remove()
    }

    @Suppress("DEPRECATION")
    open fun onPanelExpanded(panel: View?) {
        // setting fragments values
        playerFragment?.setMenuVisibility(true)
        playerFragment?.userVisibleHint = true // todo: remove legacy userVisibleHint
        playerFragment?.onShow()
        onBackPressedDispatcher.addCallback(this, panelBackPressedCallback)
    }

    private fun updatePanelHiddenState(hidden: Boolean) {
        if (hidden) panelSwitcher.collapse(slidingUpPanelLayout)
        val targetPanelHeight: Int = if (!hidden) {
            bottomNavigationBarHeight + miniPlayerHeight
        } else {
            0
        }
        if (targetPanelHeight != slidingUpPanelLayout.panelHeight) {
            slidingUpPanelLayout.panelHeight = targetPanelHeight
        }
    }

    private val panelBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            lifecycleScope.launch { panelViewModel.collapsePanel() }
        }
    }

    //region UnarySlidingUpPanelProvider

    protected val panelSwitcher: SlidingUpPanelSwitchHelper = SlidingUpPanelSwitchHelper()

    override fun requestToSetAntiDragView(view: View?): Boolean {
        slidingUpPanelLayout.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        slidingUpPanelLayout.setScrollableView(view)
        return true
    }
    //endregion

    private fun setMiniPlayerFadingProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        val alpha = 1 - progress
        panelBinding.miniPlayerFragment.also {
            it.alpha = alpha
            // necessary to make the views below clickable
            it.visibility = if (alpha == 0f) View.GONE else View.VISIBLE
        }
        panelBinding.navigationBar.also {
            it.visibility = if (progress == 0f) View.VISIBLE else View.GONE
        }
    }

    //region Color Animation
    private var animator: ValueAnimator? = null
    private fun animateSystemBarsColor(oldColor: Int, newColor: Int) {
        cancelSystemBarsColorAnimation()
        animator = ValueAnimator.ofFloat(0f, 1f)
            .also { animator ->
                animator.duration = 600L
                animator.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
                animator.addUpdateListener {
                    val progress = animator.animatedValue as Float
                    val statusbarColor: Int = if (!panelViewModel.useTransparentStatusbar.value) {
                        argbEvaluator.evaluate(progress, oldColor, newColor) as Int
                    } else {
                        Color.TRANSPARENT
                    }
                    SystemBarsControllerDelegate.updateSystemBarsColor(this, statusbarColor, translucentScrim)
                }
                animator.start()
            }
    }

    private fun cancelSystemBarsColorAnimation() {
        animator?.end()
        animator?.cancel()
        animator = null
    }
    //endregion

    override val snackBarContainer: View get() = panelBinding.contentContainer
    override val snackBarAnchor: View get() = panelBinding.miniPlayerDocker

    companion object {
        const val NOW_PLAYING_FRAGMENT = "NowPlayingPlayerFragment"

        private val argbEvaluator = ArgbEvaluator()
    }
}
