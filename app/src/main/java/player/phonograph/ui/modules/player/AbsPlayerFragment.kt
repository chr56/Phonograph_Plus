package player.phonograph.ui.modules.player

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.repo.loader.FavoriteSongs
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.dialogs.LyricsDialog
import player.phonograph.ui.dialogs.QueueSnapshotsDialog
import player.phonograph.ui.dialogs.SleepTimerDialog
import player.phonograph.ui.dialogs.SpeedControlDialog
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.modules.panel.QueueViewModel
import player.phonograph.ui.modules.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.ui.modules.player.controller.PlayerControllerFragment
import player.phonograph.ui.modules.playlist.dialogs.CreatePlaylistDialogActivity
import player.phonograph.ui.modules.setting.dialog.NowPlayingScreenPreferenceDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.warning
import util.theme.color.toolbarIconColor
import util.theme.view.menu.setMenuColor
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withCreated
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AbsPlayerFragment :
        AbsMusicServiceFragment(), UnarySlidingUpPanelProvider, SlidingUpPanelLayout.PanelSlideListener {

    protected val viewModel: PlayerFragmentViewModel by viewModels()
    protected val lyricsViewModel: LyricsViewModel by viewModels({ requireActivity() })
    protected val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    protected lateinit var playbackControlsFragment: PlayerControllerFragment<*>
    protected lateinit var queueFragment: PlayerQueueFragment

    protected abstract fun requireToolBarContainer(): View?
    protected abstract fun requireToolbar(): Toolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        playbackControlsFragment =
            childFragmentManager.findFragmentById(R.id.playback_controls_fragment) as PlayerControllerFragment<*>

        queueFragment = childFragmentManager.findFragmentById(R.id.player_queue_fragment) as PlayerQueueFragment

        observeState()
    }

    override fun onDestroyView() {
        favoriteMenuItem = null
        lyricsMenuItem = null
        super.onDestroyView()
    }

    //
    // Toolbar
    //

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

    private fun showToolbar(toolbar: View) {
        toolbar.visibility = View.VISIBLE
        toolbar.animate().alpha(1f).setDuration(VISIBILITY_ANIM_DURATION)
    }

    private fun hideToolbar(toolbar: View) {
        toolbar.animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
            .withEndAction { toolbar.visibility = View.GONE }
    }

    fun onShow() {
        playbackControlsFragment.onShow()
    }

    fun onHide() {
        playbackControlsFragment.onHide()
        collapseToNormal()
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.EXPANDED  -> {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, collapseBackPressedCallback)
                if (panel.id == R.id.player_sliding_layout) queueFragment.positionLockState = true
            }

            PanelState.COLLAPSED -> {
                collapseBackPressedCallback.remove()
                lifecycleScope.launch(Dispatchers.Main) {
                    withCreated {
                        queueFragment.resetToCurrentPosition(true)
                    }
                }
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

    private lateinit var listener: MediaStoreListener
    override fun onCreate(savedInstanceState: Bundle?) {
        listener = MediaStoreListener()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(listener)
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            viewModel.updateFavoriteState(requireContext(), MusicPlayerRemote.currentSong)
        }
    }

    protected abstract fun forceChangeColor(@ColorInt newColor: Int)
    protected abstract fun changeColorWithAnimations(@ColorInt oldColor: Int, @ColorInt newColor: Int)

    private fun observeState() {
        observe(queueViewModel.currentSong) { song ->
            if (song != null) {
                lyricsViewModel.loadLyricsFor(requireContext(), song)
                viewModel.updateFavoriteState(requireContext(), song)
            }
        }
        observe(viewModel.favoriteState) {
            if (it.first != null && it.first == queueViewModel.currentSong.value) {
                val isFavorite = it.second
                lifecycle.withStarted {
                    val res =
                        if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
                    val color = toolbarIconColor(requireContext(), Color.TRANSPARENT)
                    favoriteMenuItem?.apply {
                        icon = requireContext().getTintedDrawable(res, color)
                        title =
                            if (isFavorite) getString(R.string.action_remove_from_favorites)
                            else getString(R.string.action_add_to_favorites)
                    }
                }
            }
        }
        observe(viewModel.showToolbar) {
            val container = requireToolBarContainer() ?: return@observe
            withStarted {
                if (it) {
                    showToolbar(container)
                } else {
                    hideToolbar(container)
                }
            }
        }
        observe(lyricsViewModel.lyricsInfo) { lyricsInfo ->
            withContext(Dispatchers.Main) {
                lyricsMenuItem?.isVisible = !lyricsInfo.isNullOrEmpty()
                val activated = lyricsInfo?.activatedLyrics
                MusicPlayerRemote.replaceLyrics(activated as? LrcLyrics)
            }
        }
        observe(panelViewModel.colorChange) { (oldColor, newColor) ->
            withResumed {
                val animated = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (animated) {
                    changeColorWithAnimations(oldColor, newColor)
                } else {
                    forceChangeColor(newColor)
                }
            }

        }
    }

    protected inline fun <reified T> observe(
        flow: StateFlow<T>,
        state: Lifecycle.State = Lifecycle.State.CREATED,
        lifecycle: Lifecycle = this.lifecycle,
        scope: CoroutineScope = lifecycle.coroutineScope,
        flowCollector: FlowCollector<T>,
    ) {
        scope.launch {
            lifecycle.repeatOnLifecycle(state) {
                flow.collect(flowCollector)
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
            icon =
                activity.getTintedDrawable(
                    R.drawable.ic_favorite_border_white_24dp, Color.WHITE
                )
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
            title = activity.getString(R.string.action_clear_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                MusicPlayerRemote.pauseSong()
                MusicPlayerRemote.queueManager.clearQueue()
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.action_save_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                activity.startActivity(
                    CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForCreating(
                        activity, MusicPlayerRemote.playingQueue
                    )
                )
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
        menuItem {
            title = activity.getString(R.string.change_now_playing_screen)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                NowPlayingScreenPreferenceDialog()
                    .show(childFragmentManager, "NOW_PLAYING_SCREEN")
                true
            }
        }
        menuItem {
            title = context.getString(R.string.playing_queue_history)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                QueueSnapshotsDialog()
                    .show(childFragmentManager, "QUEUE_SNAPSHOTS")
                true
            }
        }
        menuItem {
            title = activity.getString(R.string.action_clean_missing_items)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.action_clean)
                    .setMessage(R.string.action_clean_missing_items)
                    .setPositiveButton(activity.getString(android.R.string.ok)) { dialog, _ ->
                        val queueManager: QueueManager = GlobalContext.get().get()
                        queueManager.clean()
                        dialog.dismiss()
                    }
                    .setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .tintButtons()
                    .show()
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