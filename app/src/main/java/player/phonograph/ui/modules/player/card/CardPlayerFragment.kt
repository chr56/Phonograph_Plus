package player.phonograph.ui.modules.player.card

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentCardPlayerBinding
import player.phonograph.databinding.ItemListBinding
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.requireDarkenColor
import player.phonograph.util.theme.themeCardBackgroundColor
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
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withCreated
import androidx.lifecycle.withStarted
import android.animation.Animator
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils.createCircularReveal
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CardPlayerFragment :
        AbsPlayerFragment(),
        SlidingUpPanelLayout.PanelSlideListener {

    private var _viewBinding: FragmentCardPlayerBinding? = null
    private val viewBinding: FragmentCardPlayerBinding get() = _viewBinding!!

    override fun getToolBarContainer(): View? = viewBinding.toolbarContainer


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        _viewBinding = FragmentCardPlayerBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()

        viewBinding.playerSlidingLayout.let { slidingLayout ->
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

        // for some reason the xml attribute doesn't get applied here.
        viewBinding.playingQueueCard.setCardBackgroundColor(
            themeCardBackgroundColor(requireContext())
        )
        observeState()
    }

    private fun observeState() {
        observe(CurrentQueueState.position) {
            withStarted {
                viewBinding.playerQueueSubHeader.text = viewModel.upNextAndQueueTime(resources)
                if (viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
                    resetToCurrentPosition()
                }
            }
        }
    }

    override fun onDestroyView() {
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
            if (viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
                resetToCurrentPosition()
            }
        }
    }

    override fun fetchRecyclerView(): FastScrollRecyclerView = viewBinding.playerRecyclerView

    override fun getImplToolbar(): Toolbar = viewBinding.playerToolbar

    @SuppressLint("ObsoleteSdkInt")
    override fun onPanelSlide(view: View, slide: Float) {
        if (SDK_INT >= LOLLIPOP) {
            val density = resources.displayMetrics.density
            val cardElevation = (6 * slide + 2) * density
            if (!isValidElevation(cardElevation)) return // we have received some crash reports in setCardElevation()
            viewBinding.playingQueueCard.cardElevation = cardElevation
            val buttonElevation = (2 * max(0f, 1 - slide * 16) + 2) * density
            if (!isValidElevation(buttonElevation)) return
            (playbackControlsFragment as CardPlayerControllerFragment).playerPlayPauseFab
                .elevation = buttonElevation
        }
    }

    private fun isValidElevation(elevation: Float): Boolean {
        return elevation >= -Float.MAX_VALUE && elevation <= Float.MAX_VALUE
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.EXPANDED  -> {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, collapseBackPressedCallback)
            }

            PanelState.COLLAPSED -> {
                resetToCurrentPosition()
                collapseBackPressedCallback.remove()
            }

            PanelState.ANCHORED  -> {
                // this fixes a bug where the panel would get stuck for some reason
                collapseToNormal()
            }

            else                 -> Unit
        }
    }

    override fun collapseToNormal() {
        viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
    }

    override fun resetToCurrentPosition() {
        lifecycleScope.launch(Dispatchers.Main) {
            withCreated {
                viewBinding.playerRecyclerView.stopScroll()
                layoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
            }
        }
    }

    private abstract class BaseImpl(protected var fragment: CardPlayerFragment) : Impl {

        private fun textColor(@ColorInt color: Int): Int {
            val context = fragment.requireContext()
            val defaultFooterColor = themeFooterColor(fragment.requireContext())
            val nightMode = context.nightMode
            return if (color == defaultFooterColor) context.secondaryTextColor(nightMode)
            else if (nightMode) lightenColor(color) else darkenColor(color)
        }


        @SuppressLint("ObsoleteSdkInt")
        fun defaultColorChangeAnimatorSet(
            @ColorInt oldColor: Int,
            @ColorInt newColor: Int,
            vararg animators: Animator,
            onEnd: ((animator: Animator) -> Unit)? = null,
        ): AnimatorSet {
            val controllerFragment =
                (fragment.playbackControlsFragment as CardPlayerControllerFragment)
            val fab = controllerFragment.playerPlayPauseFab
            val progressSliderHeight = controllerFragment.progressSliderHeight
            require(progressSliderHeight >= 0) { "CardPlayer's progressSliderHeight is less than 0: $progressSliderHeight" }

            val backgroundAnimator: Animator =
                if (SDK_INT >= LOLLIPOP && fragment.viewBinding.root.isAttachedToWindow) {
                    val x =
                        fab.x + fab.width / 2 + fragment.playbackControlsFragment.requireView().x
                    val y =
                        fab.y + fab.height / 2 + fragment.playbackControlsFragment.requireView().y + progressSliderHeight
                    val startRadius = max(fab.width / 2, fab.height / 2)
                    val endRadius = max(
                        fragment.viewBinding.colorBackground.width,
                        fragment.viewBinding.colorBackground.height
                    )
                    fragment.viewBinding.colorBackground.setBackgroundColor(newColor)
                    createCircularReveal(
                        fragment.viewBinding.colorBackground,
                        x.toInt(), y.toInt(), startRadius.toFloat(), endRadius.toFloat()
                    )
                } else {
                    fragment.viewBinding.colorBackground.backgroundColorTransitionAnimator(
                        oldColor, newColor
                    )
                }
            val oldTextColor: Int = textColor(oldColor)
            val newTextColor: Int = textColor(newColor)
            val subHeaderAnimator =
                fragment.viewBinding.playerQueueSubHeader.textColorTransitionAnimator(oldTextColor, newTextColor)
            return AnimatorSet()
                .apply {
                    duration = PHONOGRAPH_ANIM_TIME / 2
                    play(backgroundAnimator).with(subHeaderAnimator).apply {
                        for (animator in animators) {
                            with(animator)
                        }
                    }
                    if (onEnd != null) {
                        doOnEnd(onEnd)
                    }
                }
        }

        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
            with(fragment.viewBinding) {
                playerQueueSubHeader.setTextColor(requireDarkenColor(newColor))
            }
        }

        override fun init() {}
    }

    private class PortraitImpl(fragment: CardPlayerFragment) : BaseImpl(fragment) {
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
                    if (fragment.viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
                        fragment.viewBinding.playerSlidingLayout.panelState = PanelState.EXPANDED
                    } else if (fragment.viewBinding.playerSlidingLayout.panelState == PanelState.EXPANDED) {
                        fragment.viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
                    }
                }
                menu.setOnClickListener {
                    ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = MusicPlayerRemote.position)
                        .prepareMenu(it, MusicPlayerRemote.currentSong)
                }
            }
        }

        override fun setUpPanelAndAlbumCoverHeight() {

            val albumCoverContainer: FragmentContainerView = fragment.viewBinding.playerAlbumCoverFragment

            val minPanelHeight = convertDpToPixel((72 + 24).toFloat(), fragment.resources).toInt()

            val slidingLayout = fragment.viewBinding.playerSlidingLayout
            val coverContainer = fragment.viewBinding.coverContainer

            val availablePanelHeight =
                slidingLayout.height - coverContainer.height - convertDpToPixel(8f, fragment.resources).toInt()

            if (availablePanelHeight < minPanelHeight) {
                // shrink AlbumCover
                val albumCoverHeight = albumCoverContainer.height - (minPanelHeight - availablePanelHeight)
                albumCoverContainer.layoutParams.height = albumCoverHeight
            }
            slidingLayout.panelHeight = max(minPanelHeight, availablePanelHeight)
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                slidingLayout.findViewById(R.id.player_panel)
            )
        }

        override fun updateCurrentSong(song: Song) {
            currentSongBinding.title.text = song.title
            currentSongBinding.text.text = song.infoString()
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            defaultColorChangeAnimatorSet(oldColor, newColor)

    }

    private class LandscapeImpl(fragment: CardPlayerFragment) : BaseImpl(fragment) {
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
            val panelHeight =
                fragment.viewBinding.playerSlidingLayout.height - fragment.playbackControlsFragment.requireView()
                    .height
            fragment.viewBinding.playerSlidingLayout.panelHeight = panelHeight
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel)
            )
        }

        override fun updateCurrentSong(song: Song) {
            fragment.viewBinding.playerToolbar.title = song.title
            fragment.viewBinding.playerToolbar.subtitle = song.infoString()
        }


        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            defaultColorChangeAnimatorSet(
                oldColor, newColor,
                fragment.viewBinding.playerToolbar.backgroundColorTransitionAnimator(oldColor, newColor),
                fragment.requireView().findViewById<View>(R.id.status_bar)
                    .backgroundColorTransitionAnimator(darkenColor(oldColor), darkenColor(newColor))
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
}
