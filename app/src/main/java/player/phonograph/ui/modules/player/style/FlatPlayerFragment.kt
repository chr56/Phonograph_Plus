/*
*  Copyright (c) 2022~2025 chr_56, kabouzeid
*/

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentPlayerFlatLandBinding
import player.phonograph.databinding.FragmentPlayerFlatPortraitBinding
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.resource.infoString
import player.phonograph.ui.util.isNotPortrait
import player.phonograph.ui.util.observe
import androidx.appcompat.widget.Toolbar
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
import kotlinx.coroutines.launch

class FlatPlayerFragment : AbsPlayerFragment() {

    override fun requireToolBarContainer(): View? = impl.toolbarContainer
    override fun requireToolbar(): Toolbar = impl.toolbar

    override val slidingUpPanel: SlidingUpPanelLayout? get() = impl.slidingUpPanel

    private var _impl: FlatImpl? = null
    private val impl: FlatImpl get() = _impl!!

    private interface FlatImpl {
        val root: View
        val toolbar: Toolbar
        val toolbarContainer: View?
        val slidingUpPanel: SlidingUpPanelLayout?
        val playbackControlsContainer: View
        val colorBackground: View
        val colorBackgroundOverlay: View
        val playerPanel: View?
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
            FlatLandImpl(FragmentPlayerFlatLandBinding.inflate(inflater), this)
        } else {
            FlatPortraitImpl(FragmentPlayerFlatPortraitBinding.inflate(inflater))
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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _impl?.slidingUpPanel?.removePanelSlideListener(this)
        _impl = null
    }

    private fun fixPanelNestedScrolling() {
        val parent = parentFragment ?: activity
        val slidingLayout = impl.slidingUpPanel
        if (slidingLayout != null) {
            slidingLayout.setScrollableView(queueFragment.scrollableArea)
        } else if (parent is UnarySlidingUpPanelProvider) {
            parent.requestToSetScrollableView(queueFragment.scrollableArea)
        }

        val fragmentActivity = activity
        if (fragmentActivity is UnarySlidingUpPanelProvider) {
            fragmentActivity.requestToSetAntiDragView(impl.playerPanel)
        }
    }


    override fun requestToCollapse(): Boolean {
        with(impl.slidingUpPanel ?: return false) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    override fun requestToExpand(): Boolean {
        with(impl.slidingUpPanel ?: return false) {
            if (panelState != PanelState.EXPANDED) panelState = PanelState.EXPANDED
        }
        return true
    }

    override fun requestToSwitchState() {
        with(impl.slidingUpPanel ?: return) {
            if (panelState == PanelState.EXPANDED) {
                panelState = PanelState.COLLAPSED
            } else if (panelState == PanelState.COLLAPSED) {
                panelState = PanelState.EXPANDED
            }
        }
    }

    override fun requestToSetAntiDragView(view: View?): Boolean {
        val slidingLayout = impl.slidingUpPanel ?: return false
        slidingLayout.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        val slidingLayout = impl.slidingUpPanel ?: return false
        slidingLayout.setScrollableView(view)
        return true
    }

    override val useTransparentStatusbar: Boolean get() = impl.coloredToolbar

    override val playerColoredBackground: View get() = impl.colorBackground
    override val playerColoredBackgroundOverlay: View get() = impl.colorBackgroundOverlay
    override val coloredToolbar: Boolean get() = impl.coloredToolbar

    override fun collapseToNormal() {
        impl.slidingUpPanel?.panelState = PanelState.COLLAPSED
    }

    private class FlatPortraitImpl(private val binding: FragmentPlayerFlatPortraitBinding) : FlatImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer get() = binding.toolbarContainer
        override val slidingUpPanel get() = binding.playerSlidingLayout
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val colorBackground get() = binding.colorBackground
        override val colorBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel get() = binding.playerPanel
        override val coloredToolbar = false

        private lateinit var panelHeightAdjuster: QueuePanelHeightAdjuster
        override fun init() {
            panelHeightAdjuster = QueuePanelHeightAdjuster(binding.root.resources)
        }

        override fun applyWindowInsect() {
            ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarOverlay) { view, windowInsets ->
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
            val controllerHeight = binding.playbackControlsFragment.height
            binding.colorBackground.layoutParams.height = controllerHeight
            binding.colorBackgroundOverlay.layoutParams.height = controllerHeight
        }
    }

    private class FlatLandImpl(
        private val binding: FragmentPlayerFlatLandBinding,
        private val fragment: FlatPlayerFragment,
    ) : FlatImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer: View? = null
        override val slidingUpPanel: SlidingUpPanelLayout? = null
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val colorBackground get() = binding.colorBackground
        override val colorBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel: View? = null
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
            val controllerHeight = binding.playbackControlsFragment.height
            binding.colorBackground.layoutParams.height = controllerHeight
            binding.colorBackgroundOverlay.layoutParams.height = controllerHeight
        }
    }

}
