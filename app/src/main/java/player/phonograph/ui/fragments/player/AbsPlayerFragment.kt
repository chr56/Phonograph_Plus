package player.phonograph.ui.fragments.player

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import mt.tint.viewtint.setMenuColor
import mt.util.color.toolbarIconColor
import player.phonograph.R
import player.phonograph.adapter.display.PlayingQueueAdapter
import player.phonograph.mechanism.Favorite.toggleFavorite
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.PaletteColorHolder
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.ui.dialogs.CreatePlaylistDialog
import player.phonograph.ui.dialogs.LyricsDialog
import player.phonograph.ui.dialogs.NowPlayingScreenPreferenceDialog
import player.phonograph.ui.dialogs.QueueSnapshotsDialog
import player.phonograph.ui.dialogs.SleepTimerDialog
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.util.NavigationUtil
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.warning
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        AbsMusicServiceFragment(), PaletteColorHolder {

    protected lateinit var playbackControlsFragment: AbsPlayerControllerFragment
    protected val viewModel: PlayerFragmentViewModel by viewModels()
    protected val lyricsViewModel: LyricsViewModel by viewModels()

    // recycle view
    protected lateinit var layoutManager: LinearLayoutManager
    protected lateinit var playingQueueAdapter: PlayingQueueAdapter
    private var _wrappedAdapter: RecyclerView.Adapter<*>? = null
    protected val wrappedAdapter: RecyclerView.Adapter<*> get() = _wrappedAdapter!!
    private var _recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    protected val recyclerViewDragDropManager: RecyclerViewDragDropManager get() = _recyclerViewDragDropManager!!

    protected lateinit var playerToolbar: Toolbar

    internal lateinit var impl: Impl

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initToolbar()
        setUpControllerFragment()

        observeState()
    }

    private fun initRecyclerView() {
        layoutManager = LinearLayoutManager(requireActivity())
        playingQueueAdapter = PlayingQueueAdapter(
            requireActivity() as AppCompatActivity,
            MusicPlayerRemote.playingQueue,
            MusicPlayerRemote.position
        ) {}
        _recyclerViewDragDropManager = RecyclerViewDragDropManager()
        _wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(playingQueueAdapter)
        implementRecyclerView()
    }

    protected abstract fun implementRecyclerView()

    abstract fun setUpControllerFragment()

    override fun onDestroyView() {
        favoriteMenuItem = null
        lyricsMenuItem = null
        super.onDestroyView()
        _recyclerViewDragDropManager?.let {
            recyclerViewDragDropManager.release()
            _recyclerViewDragDropManager = null
        }
        _wrappedAdapter?.let {
            WrapperAdapterUtils.releaseAll(wrappedAdapter)
            _wrappedAdapter = null
        }
    }

    //
    // Toolbar
    //

    private var lyricsMenuItem: MenuItem? = null

    private fun initToolbar() {
        playerToolbar = getImplToolbar()
        playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        requireContext().attach(playerToolbar.menu) {
            // visible
            menuItem(getString(R.string.lyrics)) {
                order = 0
                icon = requireContext()
                    .getTintedDrawable(R.drawable.ic_comment_text_outline_white_24dp, Color.WHITE)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                visible = false
                itemId = R.id.action_show_lyrics
                onClick {
                    val lyricsList = lyricsViewModel.lyricsInfo.value
                    if (lyricsList.isNotEmpty()) {
                        LyricsDialog().show(childFragmentManager, "LYRICS")
                    }
                    true
                }
            }.also {
                lyricsMenuItem = it
            }

            menuItem(getString(R.string.action_add_to_favorites)) {
                order = 1
                icon =
                    requireContext().getTintedDrawable(
                        R.drawable.ic_favorite_border_white_24dp, Color.WHITE
                    )
                // default state
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                itemId = R.id.action_toggle_favorite
                onClick {
                    val result = toggleFavorite(requireContext(), viewModel.currentSong.value)
                    if (viewModel.currentSong.value.id == MusicPlayerRemote.currentSong.id && result) {
                        viewModel.updateFavoriteState(viewModel.currentSong.value, context)
                    }
                    true
                }
            }.apply {
                favoriteMenuItem = this
            }

            // collapsed
            menuItem {
                title = getString(R.string.action_clear_playing_queue)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    MusicPlayerRemote.clearQueue()
                }
            }
            menuItem {
                title = getString(R.string.action_save_playing_queue)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    CreatePlaylistDialog.create(MusicPlayerRemote.playingQueue)
                        .show(childFragmentManager, "ADD_TO_PLAYLIST")
                    true
                }
            }
            menuItem {
                title = getString(R.string.action_choose_lyrics)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    val accessor = requireActivity() as? IOpenFileStorageAccess
                    if (accessor != null) {
                        accessor.openFileStorageAccessTool.launch(
                            OpenDocumentContract.Config(arrayOf("*/*"))
                        ) { uri -> lyricsViewModel.insert(requireContext(), uri) }
                    } else {
                        warning("Lyrics", "Can not open file from $activity")
                    }
                    true
                }
            }
            menuItem {
                title = getString(R.string.action_sleep_timer)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    SleepTimerDialog()
                        .show(childFragmentManager, "SET_SLEEP_TIMER")
                    true
                }
            }
            menuItem {
                title = getString(R.string.equalizer)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    NavigationUtil.openEqualizer(requireActivity())
                    true
                }
            }
            menuItem {
                title = getString(R.string.change_now_playing_screen)
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
        }
        setMenuColor(requireContext(), playerToolbar, playerToolbar.menu, Color.WHITE)

    }

    abstract fun getImplToolbar(): Toolbar

    protected fun toggleToolbar(toolbar: View?) {
        if (viewModel.showToolbar.value) {
            showToolbar(toolbar)
        } else {
            hideToolbar(toolbar)
        }
    }

    private fun showToolbar(toolbar: View?) {
        if (toolbar == null) return
        toolbar.visibility = View.VISIBLE
        toolbar.animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
    }

    private fun hideToolbar(toolbar: View?) {
        if (toolbar == null) return
        toolbar.animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
            .withEndAction { toolbar.visibility = View.GONE }
    }

    abstract fun getToolBarContainer(): View?
    protected fun checkToggleToolbar() {
        val toolbar = getToolBarContainer() ?: return
        if (!viewModel.showToolbar.value && toolbar.visibility != View.GONE) {
            hideToolbar(toolbar)
        } else if (viewModel.showToolbar.value && toolbar.visibility != View.VISIBLE) {
            showToolbar(toolbar)
        }
    }

    var favoriteMenuItem: MenuItem? = null

    /**
     * delayed and run on main-thread
     */
    private suspend fun updateFavoriteIcon(isFavorite: Boolean) {
        val activity = activity ?: return
        withContext(Dispatchers.Main) {
            val res = if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
            val color = toolbarIconColor(activity, Color.TRANSPARENT)
            favoriteMenuItem?.apply {
                icon = activity.getTintedDrawable(res, color)
                title =
                    if (isFavorite) getString(R.string.action_remove_from_favorites)
                    else getString(R.string.action_add_to_favorites)
            }
        }
    }

    protected val upNextAndQueueTime: String
        get() {
            val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.position)
            return buildInfoString(
                resources.getString(R.string.up_next),
                getReadableDurationString(duration)
            )
        }

    override fun onPause() {
        recyclerViewDragDropManager.cancelDrag()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        checkToggleToolbar()
    }

    open fun onShow() {
        playbackControlsFragment.show()
    }

    open fun onHide() {
        playbackControlsFragment.hide()
        onBackPressed()
    }

    private lateinit var listener: MediaStoreListener
    override fun onCreate(savedInstanceState: Bundle?) {
        listener = MediaStoreListener()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(listener)
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            lifecycleScope.launch { updateQueue() }
            viewModel.updateFavoriteState(MusicPlayerRemote.currentSong, context)
        }
    }

    abstract fun onBackPressed(): Boolean

    protected open suspend fun updateQueue() {
        lifecycle.whenStarted {
            withContext(Dispatchers.Main) {
                playingQueueAdapter.dataset = MusicPlayerRemote.playingQueue
                playingQueueAdapter.current = MusicPlayerRemote.position
            }
        }
    }

    protected open fun updateQueuePosition() {
        playingQueueAdapter.current = MusicPlayerRemote.position
    }

    internal interface Impl {
        fun init()
        fun setUpPanelAndAlbumCoverHeight()
    }

    private fun observeState() {
        observe(CurrentQueueState.queue) { queue ->
            playingQueueAdapter.dataset = queue.get() ?: MusicPlayerRemote.playingQueue
            updateQueuePosition()
        }
        observe(CurrentQueueState.position) { position ->
            playingQueueAdapter.current = position
        }
        observe(CurrentQueueState.currentSong) {
            viewModel.updateCurrentSong(MusicPlayerRemote.currentSong, context)
            lyricsViewModel.loadLyrics(MusicPlayerRemote.currentSong)
        }
        observe(CurrentQueueState.shuffleMode) {
            updateQueue()
        }
        observe(viewModel.favoriteState) {
            if (it.first == viewModel.currentSong) {
                updateFavoriteIcon(it.second)
            }
        }
        observe(lyricsViewModel.lyricsInfo) { lyricsList ->
            withContext(Dispatchers.Main) {
                val activated = lyricsList.activatedLyrics
                if (lyricsList.isNotEmpty() && activated is LrcLyrics) {
                    MusicPlayerRemote.musicService?.replaceLyrics(activated)
                    viewModel.updateLrcLyrics(activated)
                } else {
                    MusicPlayerRemote.musicService?.replaceLyrics(null)
                    viewModel.updateLrcLyrics(null)
                }
                lyricsMenuItem?.isVisible = lyricsList.isNotEmpty()
            }
        }
        observe(viewModel.paletteColor) { newColor ->
            playbackControlsFragment.modifyColor(newColor)
        }
    }

    override val paletteColor @ColorInt get() = viewModel.paletteColor.value


    private inline fun <reified T> observe(
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

    fun observePaletteColor(owner: LifecycleOwner? = null, callback: (Int) -> Unit) {
        observe(
            viewModel.paletteColor,
            state = Lifecycle.State.STARTED,
            lifecycle = owner?.lifecycle ?: this.lifecycle
        ) {
            callback(it)
        }
    }
}
