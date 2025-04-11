package player.phonograph.ui.modules.player.card

import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.databinding.FragmentCardPlayerBinding
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.ui.modules.player.AbsPlayerFragment
import player.phonograph.util.text.infoString
import player.phonograph.util.theme.themeCardBackgroundColor
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.backgroundColorTransitionAnimator
import player.phonograph.util.ui.convertDpToPixel
import player.phonograph.util.ui.isLandscape
import util.theme.color.darkenColor
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
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.animation.Animator
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils.createCircularReveal
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import kotlin.math.max
import kotlinx.coroutines.launch

class CardPlayerFragment : AbsPlayerFragment() {

    private var _viewBinding: FragmentCardPlayerBinding? = null
    private val viewBinding: FragmentCardPlayerBinding get() = _viewBinding!!

    override fun requireToolBarContainer(): View? = viewBinding.toolbarContainer
    override fun requireToolbar(): Toolbar = viewBinding.playerToolbar

    private lateinit var impl: CardImpl

    private interface CardImpl {
        fun init()
        fun postInit()
        fun generateAnimators(@ColorInt oldColor: Int, @ColorInt newColor: Int): AnimatorSet
        fun forceChangeColor(@ColorInt newColor: Int)
    }

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

        viewBinding.playerSlidingLayout.addPanelSlideListener(this)

        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        // view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        impl.postInit()

                        // queueFragment.currentSongItemVisibility = !isLandscape(resources)
                        // queueFragment.shadowItemVisibility = false

                        viewBinding.playerSlidingLayout.setAntiDragView(queueFragment.antiDragArea)
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
        currentAnimatorSet?.end()
        currentAnimatorSet?.cancel()
        _viewBinding = null
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

    override fun forceChangeColor(newColor: Int) = impl.forceChangeColor(newColor)

    private var currentAnimatorSet: AnimatorSet? = null
    override fun changeColorWithAnimations(oldColor: Int, newColor: Int) {
        currentAnimatorSet?.end()
        currentAnimatorSet?.cancel()
        currentAnimatorSet = impl.generateAnimators(oldColor, newColor).also { it.start() }
    }

    override fun collapseToNormal() {
        viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
    }

    private class PortraitImpl(val fragment: CardPlayerFragment) : CardImpl {
        override fun init() {}

        override fun postInit() {

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
            val fragmentActivity = fragment.activity
            if (fragmentActivity is UnarySlidingUpPanelProvider) {
                fragmentActivity.requestToSetAntiDragView(fragment.viewBinding.playerPanel)
            }
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            fragment.defaultColorChangeAnimatorSet(oldColor, newColor)

        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
        }
    }

    private class LandscapeImpl(val fragment: CardPlayerFragment) : CardImpl {
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

        override fun postInit() {
            with(fragment) {
                val controllerHeight = playbackControlsFragment.requireView().height
                val playerSlidingLayout = viewBinding.playerSlidingLayout
                playerSlidingLayout.panelHeight = playerSlidingLayout.height - controllerHeight
            }
            val fragmentActivity = fragment.activity
            if (fragmentActivity is UnarySlidingUpPanelProvider) {
                fragmentActivity.requestToSetAntiDragView(fragment.viewBinding.playerPanel)
            }
        }

        override fun generateAnimators(oldColor: Int, newColor: Int): AnimatorSet =
            fragment.defaultColorChangeAnimatorSet(
                oldColor, newColor,
                fragment.viewBinding.playerToolbar.backgroundColorTransitionAnimator(oldColor, newColor),
                fragment.requireView().findViewById<View>(R.id.status_bar)
                    .backgroundColorTransitionAnimator(darkenColor(oldColor), darkenColor(newColor))
            ) {
                setToolbarWidgetColor(newColor)
            }

        override fun forceChangeColor(newColor: Int) {
            fragment.playbackControlsFragment.requireView().setBackgroundColor(newColor)
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


    @SuppressLint("ObsoleteSdkInt")
    fun defaultColorChangeAnimatorSet(
        @ColorInt oldColor: Int,
        @ColorInt newColor: Int,
        vararg animators: Animator,
        onEnd: ((animator: Animator) -> Unit)? = null,
    ): AnimatorSet {
        val controllerFragment = playbackControlsFragment as CardPlayerControllerFragment
        val fab = controllerFragment.playerPlayPauseFab
        val progressSliderHeight = controllerFragment.progressSliderHeight
        require(progressSliderHeight >= 0) { "CardPlayer's progressSliderHeight is less than 0: $progressSliderHeight" }

        val backgroundAnimator: Animator =
            if (SDK_INT >= LOLLIPOP && viewBinding.root.isAttachedToWindow) {
                val x =
                    fab.x + fab.width / 2 + playbackControlsFragment.requireView().x
                val y =
                    fab.y + fab.height / 2 + playbackControlsFragment.requireView().y + progressSliderHeight
                val startRadius = max(fab.width / 2, fab.height / 2)
                val endRadius = max(
                    viewBinding.colorBackground.width,
                    viewBinding.colorBackground.height
                )
                viewBinding.colorBackground.setBackgroundColor(newColor)
                createCircularReveal(
                    viewBinding.colorBackground,
                    x.toInt(), y.toInt(), startRadius.toFloat(), endRadius.toFloat()
                )
            } else {
                viewBinding.colorBackground.backgroundColorTransitionAnimator(oldColor, newColor)
            }
        return AnimatorSet()
            .apply {
                duration = PHONOGRAPH_ANIM_TIME / 2
                play(backgroundAnimator).apply {
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
