/*
 *  Copyright (c) 2022~2025 chr_56, kabouzeid
 */

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentCardPlayerBinding
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.modules.player.controller.PlayerControllerFragment
import player.phonograph.util.observe
import player.phonograph.util.text.infoString
import player.phonograph.util.theme.themeCardBackgroundColor
import player.phonograph.util.ui.isLandscape
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import kotlin.math.max
import kotlinx.coroutines.launch

class CardPlayerFragment : AbsPlayerFragment() {

    private var _viewBinding: FragmentCardPlayerBinding? = null
    private val viewBinding: FragmentCardPlayerBinding get() = _viewBinding!!

    override fun requireToolBarContainer(): View? = viewBinding.toolbarContainer
    override fun requireToolbar(): Toolbar = viewBinding.playerToolbar

    override val slidingUpPanel: SlidingUpPanelLayout get() = viewBinding.playerSlidingLayout

    private lateinit var impl: CardImpl

    private interface CardImpl {
        fun init()
        fun adjustHeight()
        fun applyWindowInsect()
    }

    override val controllerPosition: Point
        get() = Point(
            viewBinding.playbackControlsFragment.left,
            viewBinding.playbackControlsFragment.top
        )

    override fun inflateView(inflater: LayoutInflater): View {
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        _viewBinding = FragmentCardPlayerBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()

        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (_viewBinding == null) return // for somehow, especially settings changed, view is still not ready
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                impl.applyWindowInsect()
                fixPanelNestedScrolling()
            }
        })

        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        impl.adjustHeight()
                    }
                }
            }
        })

        // for some reason the xml attribute doesn't get applied here.
        viewBinding.playingQueueCard.setCardBackgroundColor(themeCardBackgroundColor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.playerSlidingLayout.removePanelSlideListener(this)
        _viewBinding = null
    }

    private fun fixPanelNestedScrolling() {
        viewBinding.playerSlidingLayout.setScrollableView(queueFragment.scrollableArea)

        val fragmentActivity = activity
        if (fragmentActivity is UnarySlidingUpPanelProvider) {
            fragmentActivity.requestToSetAntiDragView(viewBinding.playerPanel)
        }
    }

    override fun requestToCollapse(): Boolean {
        with(viewBinding.playerSlidingLayout) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    override fun requestToExpand(): Boolean {
        with(viewBinding.playerSlidingLayout) {
            if (panelState != PanelState.EXPANDED) panelState = PanelState.EXPANDED
        }
        return true
    }

    override fun requestToSwitchState() {
        with(viewBinding.playerSlidingLayout) {
            if (panelState == PanelState.EXPANDED) {
                panelState = PanelState.COLLAPSED
            } else if (panelState == PanelState.COLLAPSED) {
                panelState = PanelState.EXPANDED
            }
        }
    }

    override fun requestToSetAntiDragView(view: View?): Boolean {
        val slidingLayout = viewBinding.playerSlidingLayout
        slidingLayout.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        val slidingLayout = viewBinding.playerSlidingLayout
        slidingLayout.setScrollableView(view)
        return true
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        updateElevation(panel, slideOffset)
    }

    private fun updateElevation(view: View, slide: Float) {
        val density = resources.displayMetrics.density

        val cardElevation = (6 * slide + 2) * density
        if (!isValidElevation(cardElevation)) return // we have received some crash reports in setCardElevation()
        viewBinding.playingQueueCard.cardElevation = cardElevation

        val buttonElevation = (2 * max(0f, 1 - slide * 16) + 2) * density
        if (!isValidElevation(buttonElevation)) return
        (playbackControlsFragment as? PlayerControllerFragment.ClassicStyled)?.fabElevation = buttonElevation
    }

    private fun isValidElevation(elevation: Float): Boolean {
        return elevation >= -Float.MAX_VALUE && elevation <= Float.MAX_VALUE
    }

    override val useTransparentStatusbar: Boolean = true

    override val playerColoredBackground: View get() = viewBinding.colorBackground
    override val playerColoredBackgroundOverlay: View get() = viewBinding.colorBackgroundOverlay
    override val coloredToolbar: Boolean get() = impl is LandscapeImpl

    override fun collapseToNormal() {
        viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
    }

    private class PortraitImpl(val fragment: CardPlayerFragment) : CardImpl {
        override fun init() {}

        override fun adjustHeight() {
            with(fragment) {
                val statusBar = viewBinding.statusBarPadding
                if (statusBar != null) {
                    ViewCompat.setOnApplyWindowInsetsListener(statusBar) { view, windowInsets ->
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                        view.updateLayoutParams<MarginLayoutParams> {
                            height = insets.top
                        }
                        WindowInsetsCompat.CONSUMED
                    }
                }
            }
        }

        override fun applyWindowInsect() {
            with(fragment) {
                val queueSlidingLayout: SlidingUpPanelLayout = viewBinding.playerSlidingLayout
                val basicPlayer: View = viewBinding.coverContainer

                val availablePanelHeight = queueSlidingLayout.height - basicPlayer.height
                val minPanelHeight = resources.getDimensionPixelSize(R.dimen.player_queue_panel_height_min)

                if (availablePanelHeight < minPanelHeight) {
                    // shrink AlbumCover
                    val albumCoverContainer = viewBinding.playerAlbumCoverFragment
                    val albumCoverHeight = albumCoverContainer.height - (minPanelHeight - availablePanelHeight)
                    albumCoverContainer.layoutParams.height = albumCoverHeight
                }
                queueSlidingLayout.panelHeight = max(minPanelHeight, availablePanelHeight)
            }
        }
    }

    private class LandscapeImpl(val fragment: CardPlayerFragment) : CardImpl {
        override fun init() {
            with(fragment) {
                // Current Song
                observe(queueViewModel.currentSong, state = Lifecycle.State.STARTED) { song: Song? ->
                    with(viewBinding) {
                        playerToolbar.title = song?.title ?: "-"
                        playerToolbar.subtitle = song?.infoString() ?: "-"
                    }
                }
            }
        }

        override fun applyWindowInsect() {
            with(fragment) {
                ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    view.updateLayoutParams<MarginLayoutParams> {
                        bottomMargin = insets.bottom
                    }
                    windowInsets
                }
                ViewCompat.setOnApplyWindowInsetsListener(viewBinding.playerToolbar) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                    view.updateLayoutParams<MarginLayoutParams> {
                        height = resources.getDimensionPixelSize(R.dimen.mini_player_height) + insets.top
                    }
                    view.updatePadding(top = insets.top)
                    WindowInsetsCompat.CONSUMED
                }
            }
        }

        override fun adjustHeight() {
            with(fragment) {
                // Height
                val controllerHeight = playbackControlsFragment.requireView().height
                val playerSlidingLayout = viewBinding.playerSlidingLayout
                playerSlidingLayout.panelHeight = playerSlidingLayout.height - controllerHeight
            }
        }
    }

}
