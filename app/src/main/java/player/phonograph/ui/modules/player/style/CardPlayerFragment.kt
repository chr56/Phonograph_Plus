/*
 *  Copyright (c) 2022~2025 chr_56, kabouzeid
 */

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentPlayerCardLandBinding
import player.phonograph.databinding.FragmentPlayerCardPortraitBinding
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.modules.player.controller.PlayerControllerFragment
import player.phonograph.ui.resource.infoString
import player.phonograph.ui.theme.themeCardBackgroundColor
import player.phonograph.ui.util.isNotPortrait
import player.phonograph.ui.util.observe
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import kotlin.math.max
import kotlinx.coroutines.launch

class CardPlayerFragment : AbsPlayerFragment() {

    override fun requireToolBarContainer(): View? = impl.toolbarContainer
    override fun requireToolbar(): Toolbar = impl.toolbar

    override val slidingUpPanel: SlidingUpPanelLayout get() = impl.slidingUpPanel

    private var _impl: CardImpl? = null
    private val impl: CardImpl get() = _impl!!

    private interface CardImpl {
        val root: View
        val toolbar: Toolbar
        val toolbarContainer: View?
        val slidingUpPanel: SlidingUpPanelLayout
        val playbackControlsContainer: View
        val colorBackground: View
        val colorBackgroundOverlay: View
        val playingQueueCard: CardView
        val playerPanel: View
        val coloredToolbar: Boolean
        fun init()
        fun adjustHeight()
        fun applyWindowInsect()
    }

    override val controllerPosition: Point
        get() = Point(
            impl.playbackControlsContainer.left,
            impl.playbackControlsContainer.top
        )

    override fun inflateView(inflater: LayoutInflater): View {
        _impl = if (isNotPortrait(resources)) {
            CardLandImpl(FragmentPlayerCardLandBinding.inflate(inflater), this)
        } else {
            CardPortraitImpl(FragmentPlayerCardPortraitBinding.inflate(inflater))
        }
        return impl.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()

        lifecycleScope.launch {
            onLayoutChangedEffect.collect { count ->
                val impl = _impl
                if (count >= 0 && impl != null) {
                    if (count == 0) {
                        impl.applyWindowInsect()
                        fixPanelNestedScrolling()
                    } else {
                        impl.adjustHeight()
                    }
                }
            }
        }

        // for some reason the xml attribute doesn't get applied here.
        impl.playingQueueCard.setCardBackgroundColor(themeCardBackgroundColor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _impl?.slidingUpPanel?.removePanelSlideListener(this)
        _impl = null
    }

    private fun fixPanelNestedScrolling() {
        impl.slidingUpPanel.setScrollableView(queueFragment.scrollableArea)

        val fragmentActivity = activity
        if (fragmentActivity is UnarySlidingUpPanelProvider) {
            fragmentActivity.requestToSetAntiDragView(impl.playerPanel)
        }
    }

    override fun requestToCollapse(): Boolean {
        with(impl.slidingUpPanel) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    override fun requestToExpand(): Boolean {
        with(impl.slidingUpPanel) {
            if (panelState != PanelState.EXPANDED) panelState = PanelState.EXPANDED
        }
        return true
    }

    override fun requestToSwitchState() {
        with(impl.slidingUpPanel) {
            if (panelState == PanelState.EXPANDED) {
                panelState = PanelState.COLLAPSED
            } else if (panelState == PanelState.COLLAPSED) {
                panelState = PanelState.EXPANDED
            }
        }
    }

    override fun requestToSetAntiDragView(view: View?): Boolean {
        impl.slidingUpPanel.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        impl.slidingUpPanel.setScrollableView(view)
        return true
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        updateElevation(panel, slideOffset)
    }

    private fun updateElevation(view: View, slide: Float) {
        val density = resources.displayMetrics.density

        val cardElevation = (6 * slide + 2) * density
        if (!isValidElevation(cardElevation)) return // we have received some crash reports in setCardElevation()
        impl.playingQueueCard.cardElevation = cardElevation

        val buttonElevation = (2 * max(0f, 1 - slide * 16) + 2) * density
        if (!isValidElevation(buttonElevation)) return
        (playbackControlsFragment as? PlayerControllerFragment.ClassicStyled)?.fabElevation = buttonElevation
    }

    private fun isValidElevation(elevation: Float): Boolean {
        return elevation >= -Float.MAX_VALUE && elevation <= Float.MAX_VALUE
    }

    override val useTransparentStatusbar: Boolean = true

    override val playerColoredBackground: View get() = impl.colorBackground
    override val playerColoredBackgroundOverlay: View get() = impl.colorBackgroundOverlay
    override val coloredToolbar: Boolean get() = impl.coloredToolbar

    override fun collapseToNormal() {
        impl.slidingUpPanel.panelState = PanelState.COLLAPSED
    }

    private class CardPortraitImpl(private val binding: FragmentPlayerCardPortraitBinding) : CardImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer get() = binding.toolbarContainer
        override val slidingUpPanel get() = binding.playerSlidingLayout
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val colorBackground get() = binding.colorBackground
        override val colorBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playingQueueCard get() = binding.playingQueueCard
        override val playerPanel get() = binding.playerPanel
        override val coloredToolbar = false

        private lateinit var panelHeightAdjuster: QueuePanelHeightAdjuster
        override fun init() {
            panelHeightAdjuster = QueuePanelHeightAdjuster(binding.root.resources)
        }

        override fun applyWindowInsect() {
            ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarPadding) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    height = insets.top
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        override fun adjustHeight() {
            panelHeightAdjuster.adjust(
                basicPlayer = binding.coverContainer,
                queuePanel = binding.playerSlidingLayout,
                albumCoverContainer = binding.playerAlbumCoverFragment,
            )
        }
    }

    private class CardLandImpl(
        private val binding: FragmentPlayerCardLandBinding,
        private val fragment: CardPlayerFragment,
    ) : CardImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer: View? = null
        override val slidingUpPanel get() = binding.playerSlidingLayout
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val colorBackground get() = binding.colorBackground
        override val colorBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playingQueueCard get() = binding.playingQueueCard
        override val playerPanel get() = binding.playerPanel
        override val coloredToolbar = true

        override fun init() {
            with(fragment) {
                // Current Song
                observe(queueViewModel.currentSong, state = Lifecycle.State.STARTED) { song: Song? ->
                    with(binding) {
                        playerToolbar.title = song?.title ?: "-"
                        playerToolbar.subtitle = song?.infoString() ?: "-"
                    }
                }
            }
        }

        override fun applyWindowInsect() {
            with(fragment) {
                ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    view.updateLayoutParams<MarginLayoutParams> {
                        bottomMargin = insets.bottom
                    }
                    windowInsets
                }
                ViewCompat.setOnApplyWindowInsetsListener(binding.playerToolbar) { view, windowInsets ->
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
                binding.playerSlidingLayout.panelHeight = binding.playerSlidingLayout.height - controllerHeight
            }
        }
    }

}
