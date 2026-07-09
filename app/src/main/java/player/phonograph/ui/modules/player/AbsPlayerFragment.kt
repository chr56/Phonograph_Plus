package player.phonograph.ui.modules.player

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.phonograph.R
import player.phonograph.foundation.compat.openEqualizer
import player.phonograph.foundation.compat.parcelable
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.model.ui.PlayerBaseStyle
import player.phonograph.model.ui.PlayerControllerStyle
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.repo.loader.FavoriteTracks
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.modules.panel.QueueViewModel
import player.phonograph.ui.modules.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.ui.modules.player.controller.PlayerControllerFragment
import player.phonograph.ui.modules.player.dialogs.LyricsDialog
import player.phonograph.ui.modules.player.dialogs.SleepTimerDialog
import player.phonograph.ui.modules.player.dialogs.SpeedControlDialog
import player.phonograph.ui.modules.setting.dialog.NowPlayingScreenStylePreferenceDialog
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.secondaryTextColorOn
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.util.PHONOGRAPH_ANIM_TIME
import player.phonograph.ui.util.SCREEN_CATEGORY_LANDSCAPE
import player.phonograph.ui.util.SCREEN_CATEGORY_PORTRAIT
import player.phonograph.ui.util.ScreenCategory
import player.phonograph.ui.util.backgroundColorTransitionAnimator
import player.phonograph.ui.util.detectScreenCategory
import player.phonograph.ui.util.observe
import player.phonograph.ui.util.setupValueAnimator
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
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils.createCircularReveal
import android.view.ViewGroup
import android.widget.Toast
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class AbsPlayerFragment :
        AbsMusicServiceFragment(),
        UnarySlidingUpPanelProvider,
        View.OnLayoutChangeListener,
        SlidingUpPanelLayout.PanelSlideListener {

    companion object {
        const val ARGUMENT_STYLE = "player_style"
    }

    protected abstract val frame: ViewElementsContainer
    protected lateinit var playbackControlsFragment: PlayerControllerFragment<*>
    protected lateinit var queueFragment: PlayerQueueFragment

    protected val viewModel: PlayerFragmentViewModel by viewModels()
    protected val lyricsViewModel: LyricsViewModel by viewModels(ownerProducer = { requireActivity() })
    protected val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    //region Lifecycle
    protected var argumentStyle: NowPlayingScreenStyle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        argumentStyle = arguments?.parcelable<NowPlayingScreenStyle>(ARGUMENT_STYLE)

        lastScreenCategory = detectScreenCategory(resources)
        favoritesEventReceiver.registerSelf(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val screenCategory = detectScreenCategory(resources)
        val inflated = inflatePlayerFrame(inflater, screenCategory)
        val controller =
            PlayerControllerFragment.newInstance(argumentStyle?.controllerStyle ?: PlayerControllerStyle.DEFAULT)
        val queue = PlayerQueueFragment.newInstance(
            withShadow = argumentStyle?.baseStyle == PlayerBaseStyle.FLAT, // todo
            withActionButtons = argumentStyle?.options?.showModeButtonsForQueue == true,
            displayCurrentSong = screenCategory != SCREEN_CATEGORY_LANDSCAPE, // todo
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

    protected abstract fun inflatePlayerFrame(inflater: LayoutInflater, @ScreenCategory screenCategory: Int): View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frame.slidingUpPanel?.addPanelSlideListener(this)
        initToolbar()
        observeState()
        view.addOnLayoutChangeListener(this)
    }

    override fun onDestroyView() {
        requireView().removeOnLayoutChangeListener(this)
        onLayoutChangedEffect.value = -1
        currentAnimatorSet?.cancel()
        favoriteMenuItem = null
        lyricsMenuItem = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        favoritesEventReceiver.unregisterSelf(requireContext())
    }
    //endregion

    //region Window Insets
    protected val onLayoutChangedEffect: MutableStateFlow<Int> = MutableStateFlow(-1)
    override fun onLayoutChange(
        v: View?,
        left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
    ) {
        onLayoutChangedEffect.update { it + 1 }
    }
    //endregion

    //region Configuration Change
    private var lastScreenCategory: Int = 0
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val current = detectScreenCategory(resources)
        if (current != lastScreenCategory) {
            lastScreenCategory = current
            parentFragmentManager.commit {
                detach(this@AbsPlayerFragment)
                attach(this@AbsPlayerFragment)
            }
        }
    }
    //endregion

    //region Toolbar
    private var lyricsMenuItem: MenuItem? = null
    private var favoriteMenuItem: MenuItem? = null

    private fun initToolbar() {
        buildPlayerToolbar(
            requireActivity(),
            frame.toolbar,
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

    //region SlideUpPanel

    fun onShow() {
        playbackControlsFragment.onShow()
    }

    fun onHide() {
        playbackControlsFragment.onHide()
        collapseToNormal()
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        val density = resources.displayMetrics.density

        // Update elevation
        updateElevation(slideOffset, density)
        playbackControlsFragment.updateElevation(slideOffset, density)
    }

    protected abstract fun updateElevation(slideOffset: Float, density: Float)

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

    protected abstract fun collapseToNormal()

    //endregion

    //region Color & Color Animation

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

    protected fun buildDefaultColorChangeAnimatorSet(
        @ColorInt oldColor: Int,
        @ColorInt newColor: Int,
    ): AnimatorSet {

        val rippleCenter = playbackControlsFragment.provideRippleCenter()
        val controllerPosition = Point(
            frame.playbackControlsContainer.left,
            frame.playbackControlsContainer.top,
        )
        val backgroundAnimator: Animator? =
            if (rippleCenter != null && frame.coloredBackground.isAttachedToWindow) {
                makeCircularRevealAnimation(
                    frame.coloredBackground,
                    frame.coloredBackgroundOverlay,
                    rippleCenter, controllerPosition,
                    newColor
                )
            } else {
                frame.coloredBackground.backgroundColorTransitionAnimator(oldColor, newColor)
            }

        val toolbarAnimator =
            if (frame.preferColoredToolbar) {
                frame.toolbar.backgroundColorTransitionAnimator(oldColor, newColor)
            } else {
                null
            }


        val toolbarTextAnimator =
            if (frame.preferColoredToolbar) {
                ValueAnimator.ofArgb(oldColor, newColor)
                    .setupValueAnimator {
                        setToolbarWidgetColor(it.animatedValue as Int)
                    }
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
        if (frame.preferColoredToolbar) {
            frame.toolbar.setBackgroundColor(newColor)
            setToolbarWidgetColor(newColor)
        }
    }

    protected fun setToolbarWidgetColor(backgroundColor: Int) {
        val context: Context = requireContext()
        val titleTextColor = textColorOn(context, backgroundColor)
        val subtitleTextColor = secondaryTextColorOn(context, backgroundColor)

        val playerToolbar = frame.toolbar
        playerToolbar.setToolbarTextColor(titleTextColor, titleTextColor, subtitleTextColor)
        tintToolbarMenuActionIcons(playerToolbar.menu, titleTextColor)
        tintOverflowButtonColor(context, titleTextColor)
    }
    //endregion

    //region State
    private fun observeState() {
        observe(queueViewModel.currentSong) { song ->
            if (song != null) {
                lyricsViewModel.loadLyricsFor(requireContext(), song)
                viewModel.updateFavoriteState(requireContext(), song)
                onCurrentSongChanged(song)
            }
        }
        observe(viewModel.favoriteState) { (song, isFavorite) ->
            if (song != null && song == queueViewModel.currentSong.value) {
                favoriteMenuItem?.apply {
                    icon = getTintedDrawable(
                        if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp,
                        textColorOn(requireContext(), Color.TRANSPARENT)
                    )
                    title =
                        if (isFavorite) getString(R.string.action_remove_from_favorites)
                        else getString(R.string.action_add_to_favorites)
                }
            }
        }
        observe(viewModel.showToolbar) {
            val container = frame.toolbarContainer
            if (container != null) {
                updateToolbarVisibility(container, it, animated = isResumed)
            }
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

    protected abstract fun onCurrentSongChanged(song: Song?)

    private val favoritesEventReceiver = EventHub.EventReceiver(EventHub.EVENT_FAVORITES_CHANGED) { _, _ ->
        lifecycleScope.launch {
            withStarted { viewModel.refreshFavoriteState(requireContext()) }
        }
    }
    //endregion

    interface ViewElementsContainer {
        val root: View

        val toolbar: Toolbar
        val toolbarContainer: View?

        val playerPanel: View?
        val playbackControlsContainer: View
        val slidingUpPanel: SlidingUpPanelLayout?

        val coloredBackground: View
        val coloredBackgroundOverlay: View

        val preferTransparentStatusbar: Boolean
        val preferColoredToolbar: Boolean
    }

    val useTransparentStatusbar: Boolean
        get() = if (isResumed) frame.preferTransparentStatusbar else false // fixme: lifecycle issue

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
        lyricsMenuItem = menuItem(activity.getString(R.string.label_lyrics)) {
            order = 0
            icon = context.getTintedDrawable(R.drawable.ic_comment_text_outline_white_24dp, Color.WHITE)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
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
            icon = context.getTintedDrawable(R.drawable.ic_favorite_border_white_24dp, Color.WHITE)
            // default state
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            itemId = R.id.action_toggle_favorite
            onClick {
                val song = queueViewModel.currentSong.value
                if (song != null) lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    FavoriteTracks.toggleState(context, song)
                    EventHub.sendEvent(context, EventHub.EVENT_FAVORITES_CHANGED)
                }
                true
            }
        }

        // collapsed
        menuItem {
            title = activity.getString(R.string.action_change_now_playing_screen)
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
                    warning(activity, "Lyrics", "Can not open file from $activity")
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
            title = activity.getString(R.string.label_equalizer)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                val audioSessionId = MusicPlayerRemote.audioSessionId
                if (audioSessionId <= 0) {
                    Toast.makeText(
                        activity,
                        activity.resources.getString(R.string.err_no_audio_ID),
                        Toast.LENGTH_LONG
                    ).show()
                }
                if (!openEqualizer(activity, audioSessionId)) {
                    Toast.makeText(
                        activity,
                        activity.resources.getString(R.string.err_no_equalizer),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.label_speed)
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