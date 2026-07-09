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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import kotlinx.coroutines.launch

class FlatPlayerFragment : AbsPlayerFragment() {

    private var _impl: FlatImpl? = null
    private val impl: FlatImpl get() = _impl!!

    override val frame: ViewElementsContainer get() = impl

    private interface FlatImpl : ViewElementsContainer {
        fun init()
        fun adjustHeight()
        fun applyWindowInsect()
        fun updateCurrentSong(song: Song?)
    }

    override fun inflatePlayerFrame(inflater: LayoutInflater): View {
        _impl = if (isNotPortrait(resources)) {
            FlatLandImpl(FragmentPlayerFlatLandBinding.inflate(inflater))
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
        val slidingLayout = impl.slidingUpPanel
        if (slidingLayout != null) {
            slidingLayout.setScrollableView(queueFragment.scrollableArea)
        } else {
            val parent = (parentFragment ?: activity) as? UnarySlidingUpPanelProvider
            parent?.requestToSetScrollableView(queueFragment.scrollableArea)
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

    override fun updateElevation(slideOffset: Float, density: Float) {}

    override fun onCurrentSongChanged(song: Song?) {
        _impl?.updateCurrentSong(song)
    }

    override fun collapseToNormal() {
        impl.slidingUpPanel?.panelState = PanelState.COLLAPSED
    }

    private class FlatPortraitImpl(private val binding: FragmentPlayerFlatPortraitBinding) : FlatImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer get() = binding.toolbarContainer
        override val slidingUpPanel get() = binding.playerSlidingLayout
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val coloredBackground get() = binding.colorBackground
        override val coloredBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel get() = binding.playerPanel

        override val preferTransparentStatusbar: Boolean = false
        override val preferColoredToolbar: Boolean = false

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

        private var lastControllerHeight = -1
        override fun adjustHeight() {
            panelHeightAdjuster.adjust(
                basicPlayer = binding.coverContainer,
                queuePanel = binding.playerSlidingLayout,
                albumCoverContainer = binding.playerAlbumCoverFragment,
            )
            val currentControllerHeight = binding.playbackControlsFragment.height
            if (currentControllerHeight != lastControllerHeight) {
                lastControllerHeight = currentControllerHeight
                binding.colorBackground.layoutParams.height = currentControllerHeight
                binding.colorBackgroundOverlay.layoutParams.height = currentControllerHeight
            }

        }

        override fun updateCurrentSong(song: Song?) {}
    }

    private class FlatLandImpl(private val binding: FragmentPlayerFlatLandBinding) : FlatImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer: View? = null
        override val slidingUpPanel: SlidingUpPanelLayout? = null
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val coloredBackground get() = binding.colorBackground
        override val coloredBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel: View? = null

        override val preferTransparentStatusbar: Boolean = true
        override val preferColoredToolbar: Boolean = true

        override fun init() {}

        override fun applyWindowInsect() {
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
                    height = binding.root.resources.getDimensionPixelSize(R.dimen.mini_player_height) + insets.top
                }
                view.updatePadding(top = insets.top)
                WindowInsetsCompat.CONSUMED
            }
        }

        private var lastControllerHeight = -1
        override fun adjustHeight() {
            val currentControllerHeight = binding.playbackControlsFragment.height
            if (currentControllerHeight != lastControllerHeight) {
                lastControllerHeight = currentControllerHeight
                binding.colorBackground.layoutParams.height = currentControllerHeight
                binding.colorBackgroundOverlay.layoutParams.height = currentControllerHeight
            }
        }

        override fun updateCurrentSong(song: Song?) {
            with(binding) {
                playerToolbar.title = song?.title ?: "-"
                playerToolbar.subtitle = song?.infoString() ?: "-"
            }
        }
    }

}
