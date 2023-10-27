package player.phonograph.ui.fragments.player.flat

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import mt.util.color.darkenColor
import mt.util.color.lightenColor
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.menu.ActionMenuProviders
import player.phonograph.databinding.FragmentFlatPlayerBinding
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.util.theme.isWindowBackgroundDarkSafe
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.requireDarkenColor
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.backgroundColorTransitionAnimator
import player.phonograph.util.ui.convertDpToPixel
import player.phonograph.util.ui.isLandscape
import player.phonograph.util.ui.textColorTransitionAnimator
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withCreated
import androidx.lifecycle.withStarted
import android.animation.AnimatorSet
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import kotlin.math.max
import kotlinx.coroutines.launch

class FlatPlayerFragment :
        AbsPlayerFragment(),
        SlidingUpPanelLayout.PanelSlideListener {

    private var _viewBinding: FragmentFlatPlayerBinding? = null
    private val viewBinding: FragmentFlatPlayerBinding get() = _viewBinding!!

    override fun getToolBarContainer(): View? = viewBinding.toolbarContainer


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

        (activity as AbsSlidingMusicPanelActivity).viewModel.enableTransparentStatusbar(false)

        observeState()
    }

    private fun observeState() {
        observe(CurrentQueueState.position) {
            withStarted {
                viewBinding.playerQueueSubHeader.text = viewModel.upNextAndQueueTime(resources)
                if (viewBinding.playerSlidingLayout == null ||
                    viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED
                ) {
                    resetToCurrentPosition()
                }
            }
        }
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


    @MainThread
    override suspend fun updateAdapter() {
        super.updateAdapter()
        lifecycle.withCreated {
            viewBinding.playerQueueSubHeader.text = viewModel.upNextAndQueueTime(resources)
            if (viewBinding.playerSlidingLayout == null || viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED) {
                resetToCurrentPosition()
            }
        }
    }

    override fun fetchRecyclerView(): FastScrollRecyclerView = viewBinding.playerRecyclerView

    override fun getImplToolbar(): Toolbar = viewBinding.playerToolbar

    override fun onBackPressed(): Boolean {
        var wasExpanded = false
        if (viewBinding.playerSlidingLayout != null) {
            wasExpanded = viewBinding.playerSlidingLayout!!.panelState == PanelState.EXPANDED
            viewBinding.playerSlidingLayout!!.panelState = PanelState.COLLAPSED
        }
        return wasExpanded
    }

    override fun onPanelSlide(view: View, slide: Float) {}
    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.ANCHORED  ->
                viewBinding.playerSlidingLayout!!.panelState = PanelState.COLLAPSED
            // this fixes a bug where the panel would get stuck for some reason
            else                 -> Unit
        }
    }

    private fun onPanelCollapsed(@Suppress("UNUSED_PARAMETER") panel: View?) {
        resetToCurrentPosition()
    }

    private fun resetToCurrentPosition() {
        viewBinding.playerRecyclerView.stopScroll()
        layoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
    }

    private abstract class BaseImpl(protected var fragment: FlatPlayerFragment) : Impl {

        private fun textColor(@ColorInt color: Int): Int {
            val context = fragment.requireContext()
            val defaultFooterColor = fragment.resources.getColor(R.color.defaultFooterColor, null)
            val nightMode = context.nightMode
            return if (color == defaultFooterColor) context.secondaryTextColor(nightMode)
            else if (nightMode) lightenColor(color) else darkenColor(color)
        }

        fun defaultColorChangeAnimatorSet(@ColorInt oldColor: Int, @ColorInt newColor: Int): AnimatorSet {
            val backgroundAnimator =
                fragment.playbackControlsFragment.requireView().backgroundColorTransitionAnimator(oldColor, newColor)
            val statusBarAnimator =
                fragment.viewBinding.playerStatusBar.backgroundColorTransitionAnimator(oldColor, newColor)
            val oldTextColor: Int = textColor(oldColor)
            val newTextColor: Int = textColor(newColor)
            val subHeaderAnimator =
                fragment.viewBinding.playerQueueSubHeader.textColorTransitionAnimator(oldTextColor, newTextColor)
            return AnimatorSet().apply {
                duration = PHONOGRAPH_ANIM_TIME / 2
                play(backgroundAnimator).with(statusBarAnimator).apply {
                    with(subHeaderAnimator)
                }
            }
        }

        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
            with(fragment.viewBinding) {
                playerStatusBar.setBackgroundColor(newColor)
                playerQueueSubHeader.setTextColor(requireDarkenColor(newColor))
                playerToolbar.setBackgroundColor(newColor)
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
                    resolveColor(
                        fragment.requireContext(),
                        R.attr.iconColor,
                        fragment.requireContext().secondaryTextColor(
                            !isWindowBackgroundDarkSafe(fragment.requireActivity())
                        )
                    ),
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
                ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = MusicPlayerRemote.position)
                    .prepareActionMenu(menu, MusicPlayerRemote.currentSong)
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

        override fun updateCurrentSong(song: Song) {
            currentSongBinding.title.text = song.title
            currentSongBinding.text.text = song.infoString()
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            defaultColorChangeAnimatorSet(oldColor, newColor)
    }

    private class LandscapeImpl(fragment: FlatPlayerFragment) : BaseImpl(fragment) {
        override fun setUpPanelAndAlbumCoverHeight() {
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                fragment.requireView().findViewById(R.id.player_panel)
            )
        }

        override fun updateCurrentSong(song: Song) {

            fragment.viewBinding.playerToolbar.title = song.title
            fragment.viewBinding.playerToolbar.subtitle = song.infoString()
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            defaultColorChangeAnimatorSet(oldColor, newColor).also {
                it.play(
                    fragment.viewBinding.playerToolbar.backgroundColorTransitionAnimator(oldColor, newColor)
                )
            }
    }
}
