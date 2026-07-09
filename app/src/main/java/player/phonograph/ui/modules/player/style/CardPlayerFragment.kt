/*
 *  Copyright (c) 2022~2025 chr_56, kabouzeid
 */

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentPlayerCardLandBinding
import player.phonograph.databinding.FragmentPlayerCardPortraitBinding
import player.phonograph.databinding.FragmentPlayerCardSquareBinding
import player.phonograph.foundation.isValidFloatValue
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.ui.resource.infoString
import player.phonograph.ui.theme.themeCardBackgroundColor
import player.phonograph.ui.util.SCREEN_CATEGORY_PORTRAIT
import player.phonograph.ui.util.SCREEN_CATEGORY_SQUARE
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

class CardPlayerFragment : AbsPlayerFragment() {

    private var _impl: CardImpl? = null
    private val impl: CardImpl get() = _impl!!

    override val frame: ViewElementsContainer get() = impl

    private interface CardImpl : ViewElementsContainer {
        fun init()
        fun adjustHeight()
        fun applyWindowInsect()
        fun updateCurrentSong(song: Song?)
        fun updateElevation(value: Float)
    }

    override fun inflatePlayerFrame(inflater: LayoutInflater, screenCategory: Int): View {
        _impl = when (screenCategory) {
            SCREEN_CATEGORY_PORTRAIT -> CardPortraitImpl(FragmentPlayerCardPortraitBinding.inflate(inflater))
            SCREEN_CATEGORY_SQUARE   -> CardSquareImpl(FragmentPlayerCardSquareBinding.inflate(inflater))
            else                     -> CardLandImpl(FragmentPlayerCardLandBinding.inflate(inflater))
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
        impl.slidingUpPanel?.setScrollableView(queueFragment.scrollableArea)

        val fragmentActivity = activity
        if (fragmentActivity is UnarySlidingUpPanelProvider) {
            fragmentActivity.requestToSetAntiDragView(impl.playerPanel)
        }
    }

    override fun requestToCollapse(): Boolean {
        with(impl.slidingUpPanel ?: return true) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    override fun requestToExpand(): Boolean {
        with(impl.slidingUpPanel ?: return true) {
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
        impl.slidingUpPanel?.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        impl.slidingUpPanel?.setScrollableView(view)
        return true
    }

    override fun updateElevation(slideOffset: Float, density: Float) {
        val cardElevation: Float = (6 * slideOffset + 2) * density
        if (isValidFloatValue(cardElevation)) {
            impl.updateElevation(cardElevation)
        }
    }

    override fun onCurrentSongChanged(song: Song?) {
        _impl?.updateCurrentSong(song)
    }

    override fun collapseToNormal() {
        impl.slidingUpPanel?.panelState = PanelState.COLLAPSED
    }

    private class CardPortraitImpl(private val binding: FragmentPlayerCardPortraitBinding) : CardImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer get() = binding.toolbarContainer
        override val slidingUpPanel get() = binding.playerSlidingLayout
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val coloredBackground get() = binding.colorBackground
        override val coloredBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel get() = binding.playerPanel

        override val preferTransparentStatusbar: Boolean = true
        override val preferColoredToolbar: Boolean = false

        private lateinit var panelHeightAdjuster: QueuePanelHeightAdjuster
        override fun init() {
            panelHeightAdjuster = QueuePanelHeightAdjuster(binding.root.resources)
            // for some reason, the XML attribute doesn't get applied here.
            binding.playingQueueCard.setCardBackgroundColor(themeCardBackgroundColor(binding.root.context))
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

        override fun updateCurrentSong(song: Song?) {}

        override fun updateElevation(value: Float) {
            binding.playingQueueCard.elevation = value
        }
    }

    private class CardLandImpl(private val binding: FragmentPlayerCardLandBinding) : CardImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer: View? = null
        override val slidingUpPanel get() = binding.playerSlidingLayout
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val coloredBackground get() = binding.colorBackground
        override val coloredBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel get() = binding.playerPanel

        override val preferTransparentStatusbar: Boolean = true
        override val preferColoredToolbar: Boolean = true

        override fun init() {
            // for some reason, the XML attribute doesn't get applied here.
            binding.playingQueueCard.setCardBackgroundColor(themeCardBackgroundColor(binding.root.context))
        }

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
                binding.playerSlidingLayout.panelHeight = binding.playerSlidingLayout.height - currentControllerHeight
            }
        }

        override fun updateCurrentSong(song: Song?) {
            with(binding) {
                playerToolbar.title = song?.title ?: "-"
                playerToolbar.subtitle = song?.infoString() ?: "-"
            }
        }

        override fun updateElevation(value: Float) {
            binding.playingQueueCard.elevation = value
        }
    }

    private class CardSquareImpl(private val binding: FragmentPlayerCardSquareBinding) : CardImpl {
        override val root get() = binding.root
        override val toolbar get() = binding.playerToolbar
        override val toolbarContainer get() = binding.toolbarContainer
        override val slidingUpPanel = null
        override val playbackControlsContainer get() = binding.playbackControlsFragment
        override val coloredBackground get() = binding.colorBackground
        override val coloredBackgroundOverlay get() = binding.colorBackgroundOverlay
        override val playerPanel get() = binding.playerPanel

        override val preferTransparentStatusbar: Boolean = true
        override val preferColoredToolbar: Boolean = false

        override fun init() {
            binding.playingQueueCard.setCardBackgroundColor(themeCardBackgroundColor(binding.root.context))
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

        override fun updateElevation(value: Float) {
            binding.playingQueueCard.elevation = value
        }
    }

}
