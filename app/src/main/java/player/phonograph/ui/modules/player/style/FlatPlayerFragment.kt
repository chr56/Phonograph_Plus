/*
 *  Copyright (c) 2022~2025 chr_56, kabouzeid
 */

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentFlatPlayerBinding
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.util.text.infoString
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.backgroundColorTransitionAnimator
import player.phonograph.util.ui.convertDpToPixel
import player.phonograph.util.ui.isLandscape
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarTextColor
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.animation.Animator
import android.animation.AnimatorSet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import kotlin.math.max
import kotlinx.coroutines.launch

class FlatPlayerFragment : AbsPlayerFragment() {

    private var _viewBinding: FragmentFlatPlayerBinding? = null
    private val viewBinding: FragmentFlatPlayerBinding get() = _viewBinding!!

    override fun requireToolBarContainer(): View? = viewBinding.toolbarContainer
    override fun requireToolbar(): Toolbar = viewBinding.playerToolbar
    override val slidingUpPanel: SlidingUpPanelLayout? get() = viewBinding.playerSlidingLayout

    private lateinit var impl: FlatImpl

    private interface FlatImpl {
        fun init()
        fun postInit()
        fun forceChangeColor(@ColorInt newColor: Int)
        fun generateAnimators(@ColorInt oldColor: Int, @ColorInt newColor: Int): AnimatorSet
    }

    override fun inflateView(inflater: LayoutInflater): View {
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        _viewBinding = FragmentFlatPlayerBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()

        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        // view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        impl.postInit()
                        fixPanelNestedScrolling()
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.playerSlidingLayout?.removePanelSlideListener(this)
        currentAnimatorSet?.end()
        currentAnimatorSet?.cancel()
        _viewBinding = null
    }

    private fun fixPanelNestedScrolling() {
        val parent = parentFragment ?: activity
        val slidingLayout = viewBinding.playerSlidingLayout
        if (slidingLayout != null) {
            slidingLayout.setScrollableView(queueFragment.scrollableArea)
        } else if (parent is UnarySlidingUpPanelProvider) {
            parent.requestToSetScrollableView(queueFragment.scrollableArea)
        }

        val fragmentActivity = activity
        if (fragmentActivity is UnarySlidingUpPanelProvider) {
            fragmentActivity.requestToSetAntiDragView(viewBinding.playerPanel)
        }
    }


    override fun requestToCollapse(): Boolean {
        with(viewBinding.playerSlidingLayout ?: return false) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    override fun requestToExpand(): Boolean {
        with(viewBinding.playerSlidingLayout ?: return false) {
            if (panelState != PanelState.EXPANDED) panelState = PanelState.EXPANDED
        }
        return true
    }

    override fun requestToSwitchState() {
        with(viewBinding.playerSlidingLayout ?: return) {
            if (panelState == PanelState.EXPANDED) {
                panelState = PanelState.COLLAPSED
            } else if (panelState == PanelState.COLLAPSED) {
                panelState = PanelState.EXPANDED
            }
        }
    }

    override fun requestToSetAntiDragView(view: View?): Boolean {
        val slidingLayout = viewBinding.playerSlidingLayout ?: return false
        slidingLayout.setAntiDragView(view)
        return true
    }

    override fun requestToSetScrollableView(view: View?): Boolean {
        val slidingLayout = viewBinding.playerSlidingLayout ?: return false
        slidingLayout.setScrollableView(view)
        return true
    }

    override fun forceChangeColor(newColor: Int) = impl.forceChangeColor(newColor)

    private var currentAnimatorSet: AnimatorSet? = null
    override fun changeColorWithAnimations(oldColor: Int, newColor: Int) {
        currentAnimatorSet?.end()
        currentAnimatorSet?.cancel()
        currentAnimatorSet = impl.generateAnimators(oldColor, newColor).also { it.start() }
    }


    override fun collapseToNormal() {
        viewBinding.playerSlidingLayout?.panelState = PanelState.COLLAPSED
    }

    private class PortraitImpl(val fragment: FlatPlayerFragment) : FlatImpl {
        override fun init() {}

        override fun postInit() {

            val albumCoverContainer = fragment.viewBinding.playerAlbumCoverFragment

            val minPanelHeight =
                convertDpToPixel((8 + 72 + 24).toFloat(), fragment.resources).toInt() +
                        fragment.resources.getDimensionPixelSize(R.dimen.progress_container_height) +
                        fragment.resources.getDimensionPixelSize(R.dimen.media_controller_container_height)

            val playerContainer: View = fragment.viewBinding.coverContainer
            val slidingLayout: SlidingUpPanelLayout? = fragment.viewBinding.playerSlidingLayout

            val availablePanelHeight = slidingLayout!!.height - playerContainer.height

            if (availablePanelHeight < minPanelHeight) {
                // shrink AlbumCover
                val albumCoverHeight = albumCoverContainer.height - (minPanelHeight - availablePanelHeight)
                albumCoverContainer.layoutParams.height = albumCoverHeight
            }
            fragment.viewBinding.playerSlidingLayout!!.panelHeight = max(minPanelHeight, availablePanelHeight)
        }

        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
            fragment.viewBinding.playerStatusBar.setBackgroundColor(newColor)
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            fragment.defaultColorChangeAnimatorSet(oldColor, newColor)
    }

    private class LandscapeImpl(val fragment: FlatPlayerFragment) : FlatImpl {
        override fun init() {
            with(fragment) {
                // WindowInsets
                ViewCompat.setOnApplyWindowInsetsListener(viewBinding.playerFragmentRoot) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    view.updateLayoutParams<MarginLayoutParams> {
                        rightMargin = insets.right
                    }
                    WindowInsetsCompat.CONSUMED
                }
                // Current Song
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        queueViewModel.currentSong.collect { song: Song? ->
                            with(viewBinding) {
                                playerToolbar.title = song?.title ?: "-"
                                playerToolbar.subtitle = song?.infoString() ?: "-"
                            }
                        }
                    }
                }
            }
        }

        override fun postInit() {}

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            fragment.defaultColorChangeAnimatorSet(
                oldColor, newColor,
                fragment.viewBinding.playerToolbar.backgroundColorTransitionAnimator(oldColor, newColor)
            ) {
                setToolbarWidgetColor(newColor)
            }

        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
            fragment.viewBinding.playerStatusBar.setBackgroundColor(newColor)
            with(fragment.viewBinding) {
                playerToolbar.setBackgroundColor(newColor)
                setToolbarWidgetColor(newColor)
            }
        }

        private fun setToolbarWidgetColor(newColor: Int) {
            with(fragment.viewBinding) {
                val context = root.context
                val titleTextColor = context.primaryTextColor(newColor)
                val subtitleTextColor = context.secondaryTextColor(newColor)
                playerToolbar.setToolbarTextColor(titleTextColor, titleTextColor, subtitleTextColor)
                tintToolbarMenuActionIcons(playerToolbar.menu, titleTextColor)
                tintOverflowButtonColor(context, titleTextColor)
            }
        }
    }

    fun defaultColorChangeAnimatorSet(
        @ColorInt oldColor: Int,
        @ColorInt newColor: Int,
        vararg animators: Animator,
        onEnd: ((animator: Animator) -> Unit)? = null,
    ): AnimatorSet {
        val backgroundAnimator =
            playbackControlsFragment.requireView().backgroundColorTransitionAnimator(oldColor, newColor)
        val statusBarAnimator =
            viewBinding.playerStatusBar.backgroundColorTransitionAnimator(oldColor, newColor)
        return AnimatorSet().apply {
            duration = PHONOGRAPH_ANIM_TIME / 2
            play(backgroundAnimator).with(statusBarAnimator).apply {
                for (animator in animators) {
                    with(animator)
                }
            }

            if (onEnd != null) {
                doOnEnd(onEnd)
            }
        }
    }

}
