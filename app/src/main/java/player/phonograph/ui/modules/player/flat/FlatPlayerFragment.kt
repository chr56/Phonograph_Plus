/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.player.flat

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentFlatPlayerBinding
import player.phonograph.databinding.ItemListBinding
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.util.text.infoString
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.requireDarkenColor
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.themeIconColor
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.backgroundColorTransitionAnimator
import player.phonograph.util.ui.convertDpToPixel
import player.phonograph.util.ui.isLandscape
import player.phonograph.util.ui.textColorTransitionAnimator
import util.theme.color.darkenColor
import util.theme.color.lightenColor
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
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import kotlin.math.max
import kotlinx.coroutines.launch

class FlatPlayerFragment : AbsPlayerFragment() {

    private var _viewBinding: FragmentFlatPlayerBinding? = null
    private val viewBinding: FragmentFlatPlayerBinding get() = _viewBinding!!

    override fun requireQueueRecyclerView(): FastScrollRecyclerView = viewBinding.playerRecyclerView
    override fun requireToolBarContainer(): View? = viewBinding.toolbarContainer
    override fun requireToolbar(): Toolbar = viewBinding.playerToolbar

    private lateinit var impl: Impl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        _viewBinding = FragmentFlatPlayerBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()

        viewBinding.playerSlidingLayout?.let { slidingLayout ->
            slidingLayout.addPanelSlideListener(this)
            slidingLayout.setAntiDragView(view.findViewById(R.id.draggable_area))
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        prepareHeight()
                    }
                }
            }

            fun prepareHeight() {
                // view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                impl.setUpPanelAndAlbumCoverHeight()
            }
        })
    }

    override fun onDestroyView() {
        if (viewBinding.playerSlidingLayout != null) {
            viewBinding.playerSlidingLayout!!.removePanelSlideListener(this)
        }
        viewBinding.playerRecyclerView.itemAnimator = null
        viewBinding.playerRecyclerView.adapter = null
        viewBinding.playerRecyclerView.layoutManager = null
        super.onDestroyView()
        _viewBinding = null
    }


    override fun onPanelSlide(view: View, slide: Float) {}

    override fun forceChangeColor(newColor: Int) = impl.forceChangeColor(newColor)

    override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
        impl.generateAnimators(oldColor, newColor)

    override fun updateCurrentSong(song: Song?): Unit = impl.updateCurrentSong(song)

    override fun collapseToNormal() {
        viewBinding.playerSlidingLayout?.panelState = PanelState.COLLAPSED
    }

    override fun resetToCurrentPosition(force: Boolean) {
        val condition =
            viewBinding.playerSlidingLayout == null || viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED
        if (condition || force) {
            viewBinding.playerRecyclerView.stopScroll()
            layoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
        }
    }

    override fun updateQueueTime(position: Int) {
        with(viewBinding) {
            playerQueueSubHeader.text = buildUpNextAndQueueTimeText(position)
        }
    }

    private abstract class BaseImpl(val fragment: FlatPlayerFragment) : Impl {
        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
            with(fragment.viewBinding) {
                playerStatusBar.setBackgroundColor(newColor)
                playerQueueSubHeader.setTextColor(requireDarkenColor(newColor))
            }
        }

        override fun init() {}
    }

    private class PortraitImpl(fragment: FlatPlayerFragment) : BaseImpl(fragment) {
        lateinit var currentSongBinding: ItemListBinding
        override fun init() {
            super.init()
            currentSongBinding = ItemListBinding.bind(fragment.requireView().findViewById(R.id.current_song))
            with(currentSongBinding) {
                title.isSingleLine = false
                title.maxLines = 2
                text.ellipsize = TextUtils.TruncateAt.MARQUEE
                text.isSelected = true
                separator.visibility = View.VISIBLE
                shortSeparator.visibility = View.GONE
                image.scaleType = ImageView.ScaleType.CENTER
                image.setColorFilter(
                    themeIconColor(image.context),
                    PorterDuff.Mode.SRC_IN
                )
                image.setImageResource(R.drawable.ic_volume_up_white_24dp)
                root.setOnClickListener {
                    // toggle the panel
                    if (fragment.viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED) {
                        fragment.viewBinding.playerSlidingLayout!!.panelState = PanelState.EXPANDED
                    } else if (fragment.viewBinding.playerSlidingLayout!!.panelState == PanelState.EXPANDED) {
                        fragment.viewBinding.playerSlidingLayout!!.panelState = PanelState.COLLAPSED
                    }
                }
                menu.setOnClickListener {
                    val song: Song? = MusicPlayerRemote.currentSong
                    if (song != null)
                        ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = MusicPlayerRemote.position)
                            .prepareMenu(it, song)
                }
            }
        }

        override fun setUpPanelAndAlbumCoverHeight() {

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
            (fragment.activity as AbsSlidingMusicPanelActivity).setAntiDragView(
                fragment.viewBinding.playerSlidingLayout!!.findViewById(R.id.player_panel)
            )
        }

        override fun updateCurrentSong(song: Song?) {
            with(currentSongBinding) {
                if (song != null) {
                    title.text = song.title
                    text.text = song.infoString()
                } else {
                    title.text = "-"
                    text.text = "-"
                }
            }
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            fragment.defaultColorChangeAnimatorSet(oldColor, newColor)
    }

    private class LandscapeImpl(fragment: FlatPlayerFragment) : BaseImpl(fragment) {
        override fun init() {
            super.init()
            ViewCompat.setOnApplyWindowInsetsListener(fragment.viewBinding.playerFragmentRoot) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    rightMargin = insets.right
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        override fun setUpPanelAndAlbumCoverHeight() {
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                fragment.requireView().findViewById(R.id.player_panel)
            )
        }

        override fun updateCurrentSong(song: Song?) {
            with(fragment.viewBinding.playerToolbar) {
                if (song != null) {
                    title = song.title
                    subtitle = song.infoString()
                } else {
                    title = "-"
                    subtitle = "-"
                }
            }
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            fragment.defaultColorChangeAnimatorSet(
                oldColor, newColor,
                fragment.viewBinding.playerToolbar.backgroundColorTransitionAnimator(oldColor, newColor)
            ) {
                setToolbarWidgetColor(newColor)
            }

        override fun forceChangeColor(newColor: Int) {
            super.forceChangeColor(newColor)
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
        val oldTextColor: Int = textColor(oldColor)
        val newTextColor: Int = textColor(newColor)
        val subHeaderAnimator = viewBinding.playerQueueSubHeader.textColorTransitionAnimator(oldTextColor, newTextColor)
        return AnimatorSet().apply {
            duration = PHONOGRAPH_ANIM_TIME / 2
            play(backgroundAnimator).with(statusBarAnimator).with(subHeaderAnimator).apply {
                for (animator in animators) {
                    with(animator)
                }
            }

            if (onEnd != null) {
                doOnEnd(onEnd)
            }
        }
    }

    private fun textColor(@ColorInt color: Int): Int {
        val defaultFooterColor = themeFooterColor(requireContext())
        val nightMode = requireContext().nightMode
        return if (color == defaultFooterColor) requireContext().secondaryTextColor(nightMode)
        else if (nightMode) lightenColor(color) else darkenColor(color)
    }

}
