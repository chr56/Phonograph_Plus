package player.phonograph.ui.modules.player

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.phonograph.R
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.model.ui.PlayerBaseStyle
import player.phonograph.model.ui.PlayerControllerStyle
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.repo.loader.FavoriteSongs
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.dialogs.LyricsDialog
import player.phonograph.ui.dialogs.SleepTimerDialog
import player.phonograph.ui.dialogs.SpeedControlDialog
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.modules.panel.QueueViewModel
import player.phonograph.ui.modules.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.ui.modules.player.controller.PlayerControllerFragment
import player.phonograph.ui.modules.setting.dialog.NowPlayingScreenStylePreferenceDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.parcelable
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.backgroundColorTransitionAnimator
import player.phonograph.util.ui.isLandscape
import player.phonograph.util.ui.setupValueAnimator
import player.phonograph.util.warning
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.color.toolbarIconColor
import util.theme.view.menu.setMenuColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarTextColor
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils.createCircularReveal
import android.view.ViewGroup
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class AbsPlayerFragment :
        AbsMusicServiceFragment(), UnarySlidingUpPanelProvider, SlidingUpPanelLayout.PanelSlideListener {

    companion object {
        const val ARGUMENT_STYLE = "player_style"
    }

    protected var argumentStyle: NowPlayingScreenStyle? = null

    protected val viewModel: PlayerFragmentViewModel by viewModels()
    protected val lyricsViewModel: LyricsViewModel by viewModels({ requireActivity() })
    protected val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    protected lateinit var playbackControlsFragment: PlayerControllerFragment<*>
    protected lateinit var queueFragment: PlayerQueueFragment

    protected abstract val slidingUpPanel: SlidingUpPanelLayout?

    protected abstract val controllerPosition: Point

    protected abstract fun requireToolBarContainer(): View?
    protected abstract fun requireToolbar(): Toolbar

    protected abstract fun inflateView(inflater: LayoutInflater): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(MediaStoreListener())

        argumentStyle = arguments?.parcelable<NowPlayingScreenStyle>(ARGUMENT_STYLE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val inflated = inflateView(inflater)
        val controller =
            PlayerControllerFragment.newInstance(argumentStyle?.controllerStyle ?: PlayerControllerStyle.DEFAULT)
        val queue = PlayerQueueFragment.newInstance(
            withShadow = argumentStyle?.baseStyle == PlayerBaseStyle.FLAT, // todo
            withActionButtons = argumentStyle?.options?.showModeButtonsForQueue == true,
            displayCurrentSong = !isLandscape(resources)
        )
        childFragmentManager.commit {
            replace(R.id.playback_controls_fragment, controller)
            replace(R.id.player_queue_fragment, queue)
        }
        childFragmentManager.executePendingTransactions()
        playbackControlsFragment = controller
        queueFragment = queue
        return inflated
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        slidingUpPanel?.addPanelSlideListener(this)
        initToolbar()
        observeState()
    }

    override fun onDestroyView() {
        favoriteMenuItem = null
        lyricsMenuItem = null
        currentAnimatorSet?.cancel()
        super.onDestroyView()
    }

    //region Toolbar
    private var lyricsMenuItem: MenuItem? = null
    private var favoriteMenuItem: MenuItem? = null

    private fun initToolbar() {
        buildPlayerToolbar(
            requireActivity(),
            requireToolbar(),
            lifecycle,
            childFragmentManager,
            lyricsViewModel,
            queueViewModel
        ).also {
            lyricsMenuItem = it.first
            favoriteMenuItem = it.second
        }
    }

    private fun updateToolbarVisibility(toolbar: View, visibility: Boolean, animated: Boolean) {
        if (animated) {
            if (visibility) {
                toolbar.animate().alpha(1f).setDuration(VISIBILITY_ANIM_DURATION)
                    .withStartAction { toolbar.visibility = View.VISIBLE }
                    .start()
            } else {
                toolbar.animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
                    .withEndAction { toolbar.visibility = View.GONE }
                    .start()
            }
        } else {
            toolbar.visibility = if (visibility) View.VISIBLE else View.GONE
        }
    }
    //endregion

    fun onShow() {
        playbackControlsFragment.onShow()
    }

    fun onHide() {
        playbackControlsFragment.onHide()
        collapseToNormal()
    }

    //region SlideUpPanel
    override fun onPanelSlide(panel: View, slideOffset: Float) {}

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.EXPANDED  -> {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, collapseBackPressedCallback)
                if (panel.id == R.id.player_sliding_layout) queueFragment.positionLockState = true
            }

            PanelState.COLLAPSED -> {
                collapseBackPressedCallback.remove()
                queueFragment.resetToCurrentPosition(true)
                if (panel.id == R.id.player_sliding_layout) queueFragment.positionLockState = false
            }

            PanelState.ANCHORED  -> {
                // this fixes a bug where the panel would get stuck for some reason
                collapseToNormal()
            }

            else                 -> Unit
        }
    }

    protected val collapseBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                collapseToNormal()
            }
        }
    //endregion

    protected abstract fun collapseToNormal()

    open val useTransparentStatusbar: Boolean = false

    private var currentAnimatorSet: AnimatorSet? = null

    private fun onColorChanged(oldColor: Int, newColor: Int) {
        val animated = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (animated) {
            currentAnimatorSet?.end()
            currentAnimatorSet?.cancel()
            currentAnimatorSet = buildDefaultColorChangeAnimatorSet(oldColor, newColor).also { it.start() }
        } else {
            currentAnimatorSet?.cancel()
            forceChangeColor(newColor)
        }
    }

    protected abstract val coloredToolbar: Boolean

    protected abstract val playerColoredBackground: View
    protected abstract val playerColoredBackgroundOverlay: View

    protected fun buildDefaultColorChangeAnimatorSet(
        @ColorInt oldColor: Int,
        @ColorInt newColor: Int,
    ): AnimatorSet {

        // todo: fix offset
        val rippleCenter = playbackControlsFragment.provideRippleCenter()
        val backgroundAnimator: Animator? =
            if (rippleCenter != null && playerColoredBackground.isAttachedToWindow) {
                makeCircularRevealAnimation(
                    playerColoredBackground,
                    playerColoredBackgroundOverlay,
                    rippleCenter, controllerPosition,
                    newColor
                )
            } else {
                playerColoredBackground.backgroundColorTransitionAnimator(oldColor, newColor)
            }

        val toolbarAnimator =
            if (coloredToolbar) requireToolbar().backgroundColorTransitionAnimator(oldColor, newColor) else null


        val toolbarTextAnimator =
            if (coloredToolbar) {
                ValueAnimator.ofArgb(oldColor, newColor)
                    .setupValueAnimator { setToolbarWidgetColor(it.animatedValue as Int) }
            } else {
                null
            }

        return AnimatorSet().apply {
            duration = PHONOGRAPH_ANIM_TIME
            play(backgroundAnimator).apply {
                if (toolbarAnimator != null) with(toolbarAnimator)
                if (toolbarTextAnimator != null) with(toolbarTextAnimator)
            }
        }
    }

    private fun makeCircularRevealAnimation(
        background: View,
        backgroundOverlay: View,
        rippleCenter: Point,
        offset: Point,
        @ColorInt newColor: Int,
    ): Animator {
        val radius = max(backgroundOverlay.width, backgroundOverlay.height)
        return createCircularReveal(
            backgroundOverlay,
            rippleCenter.x + offset.x, rippleCenter.y + offset.y,
            0f, radius.toFloat(),
        ).apply {
            doOnStart {
                backgroundOverlay.setBackgroundColor(newColor)
                backgroundOverlay.visibility = View.VISIBLE
            }
            doOnEnd {
                background.setBackgroundColor(newColor)
                backgroundOverlay.visibility = View.GONE
            }
        }
    }

    protected open fun forceChangeColor(@ColorInt newColor: Int) {
        playbackControlsFragment.requireView().setBackgroundColor(newColor)
        if (coloredToolbar) {
            requireToolbar().setBackgroundColor(newColor)
            setToolbarWidgetColor(newColor)
        }
    }

    protected fun setToolbarWidgetColor(backgroundColor: Int) {
        val context: Context = requireContext()
        val titleTextColor = context.primaryTextColor(backgroundColor)
        val subtitleTextColor = context.secondaryTextColor(backgroundColor)

        val playerToolbar = requireToolbar()
        playerToolbar.setToolbarTextColor(titleTextColor, titleTextColor, subtitleTextColor)
        tintToolbarMenuActionIcons(playerToolbar.menu, titleTextColor)
        tintOverflowButtonColor(context, titleTextColor)
    }

    private fun observeState() {
        observe(queueViewModel.currentSong) { song ->
            if (song != null) {
                lyricsViewModel.loadLyricsFor(requireContext(), song)
                viewModel.updateFavoriteState(requireContext(), song)
            }
        }
        observe(viewModel.favoriteState) { (song, isFavorite) ->
            if (song != null && song == queueViewModel.currentSong.value) {
                favoriteMenuItem?.apply {
                    icon = requireContext().getTintedDrawable(
                        if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp,
                        toolbarIconColor(requireContext(), Color.TRANSPARENT)
                    )
                    title =
                        if (isFavorite) getString(R.string.action_remove_from_favorites)
                        else getString(R.string.action_add_to_favorites)
                }
            }
        }
        observe(viewModel.showToolbar) {
            val container = requireToolBarContainer() ?: return@observe
            updateToolbarVisibility(container, it, animated = isResumed)
        }
        observe(lyricsViewModel.lyricsInfo) { lyricsInfo ->
            MusicPlayerRemote.replaceLyrics(lyricsInfo?.activatedLyrics as? LrcLyrics)
            lyricsMenuItem?.isVisible = !lyricsInfo.isNullOrEmpty()
        }
        observe(panelViewModel.colorChange) { (oldColor, newColor) ->
            withResumed { // fixme: fix lifecycle issues
                onColorChanged(oldColor, newColor)
            }
        }
    }

    protected inline fun <reified T> observe(
        flow: StateFlow<T>,
        lifecycle: Lifecycle = this.lifecycle,
        state: Lifecycle.State = Lifecycle.State.CREATED,
        scope: CoroutineScope = lifecycle.coroutineScope,
        flowCollector: FlowCollector<T>,
    ) {
        scope.launch {
            lifecycle.repeatOnLifecycle(state) {
                flow.collect(flowCollector)
            }
        }
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            lifecycleScope.launch {
                withStarted { viewModel.refreshFavoriteState(requireContext()) }
            }
        }
    }

}

private fun buildPlayerToolbar(
    activity: FragmentActivity,
    playerToolbar: Toolbar,
    lifecycle: Lifecycle,
    childFragmentManager: FragmentManager,
    lyricsViewModel: LyricsViewModel,
    queueViewModel: QueueViewModel,
): Pair<MenuItem?, MenuItem?> {
    var lyricsMenuItem: MenuItem? = null
    var favoriteMenuItem: MenuItem? = null
    attach(activity, playerToolbar.menu) {
        // visible
        lyricsMenuItem = menuItem(activity.getString(R.string.lyrics)) {
            order = 0
            icon = activity.getTintedDrawable(R.drawable.ic_comment_text_outline_white_24dp, Color.WHITE)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            visible = false
            itemId = R.id.action_show_lyrics
            onClick {
                if (lyricsViewModel.hasLyrics) {
                    LyricsDialog().show(childFragmentManager, "LYRICS")
                }
                true
            }
        }

        favoriteMenuItem = menuItem(activity.getString(R.string.action_add_to_favorites)) {
            order = 1
            icon = activity.getTintedDrawable(R.drawable.ic_favorite_border_white_24dp, Color.WHITE)
            // default state
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            itemId = R.id.action_toggle_favorite
            onClick {
                val song = queueViewModel.currentSong.value
                if (song != null) lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    FavoriteSongs.toggleFavorite(context, song)
                }
                true
            }
        }

        // collapsed
        menuItem {
            title = activity.getString(R.string.change_now_playing_screen)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                NowPlayingScreenStylePreferenceDialog().show(childFragmentManager, "NOW_PLAYING_SCREEN")
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.action_choose_lyrics)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                val activity = activity
                val accessor = activity as? IOpenFileStorageAccessible
                if (accessor != null) {
                    accessor.openFileStorageAccessDelegate.launch(OpenDocumentContract.Config(arrayOf("*/*"))) { uri ->
                        if (uri == null) return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            val lyricsViewModel = ViewModelProvider(activity)[LyricsViewModel::class.java]
                            lyricsViewModel.appendLyricsFrom(activity, uri)
                        }
                    }
                } else {
                    warning("Lyrics", "Can not open file from $activity")
                }
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.action_sleep_timer)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                SleepTimerDialog()
                    .show(childFragmentManager, "SET_SLEEP_TIMER")
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.equalizer)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                NavigationUtil.openEqualizer(activity)
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.action_speed)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                SpeedControlDialog().show(childFragmentManager, "SPEED_CONTROL_DIALOG")
                true
            }
        }
    }

    playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    playerToolbar.setNavigationOnClickListener {
        activity.onBackPressedDispatcher.onBackPressed()
    }
    setMenuColor(activity, playerToolbar, playerToolbar.menu, Color.WHITE)
    return lyricsMenuItem to favoriteMenuItem
}