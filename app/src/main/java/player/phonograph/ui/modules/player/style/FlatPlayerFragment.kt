/*
*  Copyright (c) 2022~2025 chr_56, kabouzeid
*/

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentPlayerFlatLandBinding
import player.phonograph.databinding.FragmentPlayerFlatPortraitBinding
import player.phonograph.databinding.FragmentPlayerFlatSquareBinding
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.resource.infoString
import player.phonograph.ui.util.SCREEN_CATEGORY_PORTRAIT
import player.phonograph.ui.util.SCREEN_CATEGORY_SQUARE
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
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
        fun setGradientScrim(show: Boolean)
        fun adjustHeight()
        fun applyWindowInsect()
        fun updateCurrentSong(song: Song?)
    }

    override fun inflatePlayerFrame(inflater: LayoutInflater, screenCategory: Int): ViewElementsContainer {
        _impl = when (screenCategory) {
            SCREEN_CATEGORY_PORTRAIT -> FlatPortraitImpl(FragmentPlayerFlatPortraitBinding.inflate(inflater))
            SCREEN_CATEGORY_SQUARE   -> FlatSquareImpl(FragmentPlayerFlatSquareBinding.inflate(inflater))
            else                     -> FlatLandImpl(FragmentPlayerFlatLandBinding.inflate(inflater))
        }
        return impl
    }

    override fun setupMainContent() {
        impl.init()
        impl.applyWindowInsect()
        impl.setGradientScrim(argumentStyle?.options?.showGradientScrim != false)
        fixPanelNestedScrolling()

        lifecycleScope.launch {
            onLayoutChangedEffect.collect { count ->
                if (count > 0) {
                    _impl?.adjustHeight()
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
        val fragmentActivity = activity
        if (fragmentActivity is UnarySlidingUpPanelProvider) {
            fragmentActivity.requestToSetAntiDragView(impl.playerPanel)
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

        override val displayCurrentSongStandalone: Boolean = false
        override val shadowForQueue: Boolean = true

        private lateinit var panelHeightAdjuster: QueuePanelHeightAdjuster
        override fun init() {
            panelHeightAdjuster = QueuePanelHeightAdjuster(binding.root.resources)
        }

        override fun setGradientScrim(show: Boolean) {
            binding.toolbarContainer.setBackgroundResource(
                if (show) R.drawable.toolbar_gradient else android.R.color.transparent
            )
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

        override val displayCurrentSongStandalone: Boolean = true
        override val shadowForQueue: Boolean = true

        override fun init() {}

        override fun setGradientScrim(show: Boolean) {}

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

    private class FlatSquareImpl(private val binding: FragmentPlayerFlatSquareBinding) : FlatImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer get() = binding.toolbarContainer
        override val slidingUpPanel: SlidingUpPanelLayout? = null
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val coloredBackground get() = binding.colorBackground
        override val coloredBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel get() = binding.playerPanel

        override val preferTransparentStatusbar: Boolean = false
        override val preferColoredToolbar: Boolean = false

        override val displayCurrentSongStandalone: Boolean = false
        override val shadowForQueue: Boolean = true

        override fun init() {}

        override fun setGradientScrim(show: Boolean) {
            binding.toolbarContainer.setBackgroundResource(
                if (show) R.drawable.toolbar_gradient else android.R.color.transparent
            )
        }

        override fun applyWindowInsect() {
            ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarPadding) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    height = insets.top
                }
                WindowInsetsCompat.CONSUMED
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.playerContentContainer) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = insets.bottom
                    leftMargin = insets.left
                    rightMargin = insets.right
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        override fun adjustHeight() {}

        override fun updateCurrentSong(song: Song?) {}
    }

}
