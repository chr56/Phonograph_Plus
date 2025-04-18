package player.phonograph.ui.modules.panel

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import player.phonograph.R
import player.phonograph.databinding.SlidingMusicPanelLayoutBinding
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.modules.player.MiniPlayerFragment
import player.phonograph.ui.modules.player.style.buildPlayerFragment
import player.phonograph.util.observe
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.updateSystemBarsColor
import util.theme.color.darkenColor
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.PathInterpolator

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

    private var _panelBinding: SlidingMusicPanelLayoutBinding? = null
    private val panelBinding: SlidingMusicPanelLayoutBinding get() = _panelBinding!!

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
        updateSystemBarsColor(darkenColor(primaryColor()), primaryColor()) // initial values
        miniPlayerFragment = panelBinding.miniPlayerFragment.getFragment()
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

        // add fragment
        val nowPlayingScreenStyle = Setting(this)[Keys.nowPlayingScreenStyle].flow
        observe(nowPlayingScreenStyle, state = Lifecycle.State.STARTED, distinctive = true) { screen ->
            // todo
            setupPlayerFragment(screen)
            miniPlayerFragment?.requireView()?.setOnClickListener { requestToExpand() }
            panelBinding.navigationBar.setOnClickListener { requestToExpand() }
        }

        // states
        observe(queueViewModel.queue) { queue -> hideBottomBar(queue.isEmpty() == true) }
        observe(panelViewModel.colorChange) { (oldColor, newColor) ->
            if (slidingUpPanelLayout.panelState == PanelState.EXPANDED) {
                animateSystemBarsColor(oldColor, newColor)
            }
        }

        // insets
        ViewCompat.setOnApplyWindowInsetsListener(panelBinding.miniPlayerDocker) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<MarginLayoutParams> {
                rightMargin = insets.right
                leftMargin = insets.left
            }
            windowInsets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSystemBarsColorAnimation() // just in case
    }

    private fun setupPlayerFragment(style: NowPlayingScreenStyle) {
        supportFragmentManager.commit {
            replace(
                R.id.player_fragment_container,
                buildPlayerFragment(style),
                NOW_PLAYING_FRAGMENT
            )
        }
        supportFragmentManager.executePendingTransactions()

        playerFragment = supportFragmentManager.findFragmentById(R.id.player_fragment_container) as AbsPlayerFragment

    }

    override fun onPanelSlide(panel: View, @FloatRange(from = 0.0, to = 1.0) slideOffset: Float) {
        setMiniPlayerFadingProgress(slideOffset)
        cancelSystemBarsColorAnimation()
        val from = panelViewModel.activityColor.value
        val to = panelViewModel.highlightColor.value
        moveSystemBarsColor(from, to, slideOffset)
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.EXPANDED  -> onPanelExpanded(panel)
            PanelState.ANCHORED  -> requestToCollapse() // this fixes a bug where the panel would get stuck for some reason
            else                 -> {}
        }
    }

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

    private val panelBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requestToCollapse()
        }
    }

    override fun requestToCollapse(): Boolean {
        with(slidingUpPanelLayout) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    override fun requestToExpand(): Boolean {
        with(slidingUpPanelLayout) {
            if (panelState != PanelState.EXPANDED) panelState = PanelState.EXPANDED
        }
        return true
    }

    override fun requestToSwitchState() {
        with(slidingUpPanelLayout) {
            if (panelState == PanelState.EXPANDED) {
                panelState = PanelState.COLLAPSED
            } else if (panelState == PanelState.COLLAPSED) {
                panelState = PanelState.EXPANDED
            }
        }
    }

    override fun requestToSetAntiDragView(view: View?): Boolean {
        slidingUpPanelLayout.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        slidingUpPanelLayout.setScrollableView(view)
        return true
    }

    var isBottomBarHidden: Boolean = false
        private set

    fun hideBottomBar(hide: Boolean) {
        if (hide) {
            slidingUpPanelLayout.panelHeight = 0
            requestToCollapse()
        } else {
            slidingUpPanelLayout.panelHeight =
                resources.getDimensionPixelSize(R.dimen.mini_player_height) + panelBinding.navigationBar.height
        }
        isBottomBarHidden = hide
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
            slidingUpPanelLayout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    slidingUpPanelLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    hideBottomBar(false)
                }
            })
        } // don't call hideBottomBar(true) here as it causes a bug with the SlidingUpPanelLayout
    }

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

    //region SystemBarsColors
    private val argbEvaluator = ArgbEvaluator()
    private fun moveSystemBarsColor(
        @ColorInt from: Int, @ColorInt to: Int,
        @FloatRange(from = 0.0, to = 1.0) progress: Float,
    ) {
        val navigationbarColor: Int =
            argbEvaluator.evaluate(progress, actualNavigationbarColor(from), translucentScrim) as Int
        val statusbarColor: Int =
            argbEvaluator.evaluate(progress, from, actualStatusbarColor(to)) as Int
        updateSystemBarsColor(statusbarColor, navigationbarColor)
    }

    private fun actualStatusbarColor(@ColorInt color: Int): Int =
        if (playerFragment?.useTransparentStatusbar == true) Color.TRANSPARENT else color

    private fun actualNavigationbarColor(@ColorInt color: Int): Int =
        if (isBottomBarHidden) translucentScrim else color

    private var animator: ValueAnimator? = null

    private fun animateSystemBarsColor(oldColor: Int, newColor: Int) {
        cancelSystemBarsColorAnimation()
        animator = ValueAnimator.ofFloat(0f, 1f)
            .also { animator ->
                animator.duration = 600L
                animator.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
                animator.addUpdateListener {
                    val progress = animator.animatedValue as Float
                    moveSystemBarsColor(oldColor, newColor, progress)
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

    companion object {
        const val NOW_PLAYING_FRAGMENT = "NowPlayingPlayerFragment"

        private val translucentScrim = Color.argb(64, 0, 0, 0)
    }
}
