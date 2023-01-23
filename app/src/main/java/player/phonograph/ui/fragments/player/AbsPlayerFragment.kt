package player.phonograph.ui.fragments.player

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import mt.pref.primaryColor
import mt.tint.viewtint.setMenuColor
import mt.util.color.toolbarIconColor
import player.phonograph.R
import player.phonograph.adapter.display.PlayingQueueAdapter
import player.phonograph.dialogs.CreatePlaylistDialog
import player.phonograph.dialogs.LyricsDialog
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.misc.PaletteColorHolder
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.preferences.NowPlayingScreenPreferenceDialog
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.dialogs.QueueSnapshotsDialog
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.util.FavoriteUtil.toggleFavorite
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AbsPlayerFragment :
        AbsMusicServiceFragment(), PaletteColorHolder {

    protected lateinit var callbacks: Callbacks

    protected lateinit var playerAlbumCoverFragment: PlayerAlbumCoverFragment
    protected lateinit var playbackControlsFragment: AbsPlayerControllerFragment
    protected val viewModel: PlayerFragmentViewModel
            by viewModels {
                PlayerFragmentViewModel.from(requireContext().applicationContext as Application)
            }

    lateinit var handler: Handler

    // recycle view
    protected lateinit var layoutManager: LinearLayoutManager
    protected lateinit var playingQueueAdapter: PlayingQueueAdapter
    private var _wrappedAdapter: RecyclerView.Adapter<*>? = null
    protected val wrappedAdapter: RecyclerView.Adapter<*> get() = _wrappedAdapter!!
    private var _recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    protected val recyclerViewDragDropManager: RecyclerViewDragDropManager get() = _recyclerViewDragDropManager!!

    protected lateinit var playerToolbar: Toolbar

    internal lateinit var impl: Impl

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks =
            try {
                context as Callbacks
            } catch (e: ClassCastException) {
                throw RuntimeException(
                    "${context.javaClass.simpleName} must implement ${Callbacks::class.java.simpleName}"
                )
            }
        handler = Handler(Looper.getMainLooper(), handlerCallbacks)
    }

    private val handlerCallbacks = Handler.Callback { msg ->
        if (msg.what == UPDATE_LYRICS) {
            val lyrics = msg.data.get(LYRICS) as AbsLyrics
            viewModel.forceReplaceLyrics(lyrics)
            if (lyrics is LrcLyrics) {
                playerAlbumCoverFragment.setLyrics(lyrics)
                MusicPlayerRemote.musicService?.replaceLyrics(lyrics)
            } else {
                playerAlbumCoverFragment.clearLyrics()
                MusicPlayerRemote.musicService?.replaceLyrics(null)
            }
        }
        false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initToolbar()
        setUpControllerFragment()
        setUpCoverFragment()
        setupPaletteColorObserver()

        addFavoriteSateObserver()
        addLyricsObserver()
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

    abstract fun setUpCoverFragment()

    override fun onDestroyView() {
        favoriteMenuItem = null
        viewModel.lyricsMenuItem = null
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

    private fun addLyricsObserver() {
        lifecycleScope.launch(viewModel.exceptionHandler) {
            viewModel.lyricsList.collectLatest {
                val lyrics = viewModel.currentLyrics
                withContext(Dispatchers.Main) {
                    if (lyrics != null && lyrics is LrcLyrics) {
                        playerAlbumCoverFragment.setLyrics(lyrics)
                    } else {
                        playerAlbumCoverFragment.clearLyrics()
                    }
                    viewModel.lyricsMenuItem?.isVisible =
                        (viewModel.currentLyrics != null)
                }
            }
        }
    }

    //
    // Toolbar
    //
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
                    val lyricsPack = viewModel.lyricsList.value
                    if (!lyricsPack.isEmpty()) {
                        LyricsDialog.create(
                            lyricsPack,
                            viewModel.currentSong,
                            viewModel.currentLyrics ?: lyricsPack.getAvailableLyrics()!!
                        ).show(childFragmentManager, "LYRICS")
                    }
                    true
                }
            }.apply {
                viewModel.lyricsMenuItem = this
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
                    val result = toggleFavorite(requireContext(), viewModel.currentSong)
                    if (viewModel.currentSong.id == MusicPlayerRemote.currentSong.id && result) {
                        playerAlbumCoverFragment.showHeartAnimation()
                        viewModel.updateFavoriteState(viewModel.currentSong, context)
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

    private var isToolbarShown: Boolean = true

    protected fun toggleToolbar(toolbar: View?) {
        if (isToolbarShown) {
            hideToolbar(toolbar)
        } else {
            showToolbar(toolbar)
        }
    }

    private fun showToolbar(toolbar: View?) {
        if (toolbar == null) return
        isToolbarShown = true
        toolbar.visibility = View.VISIBLE
        toolbar.animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
    }

    private fun hideToolbar(toolbar: View?) {
        if (toolbar == null) return
        isToolbarShown = false
        toolbar.animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
            .withEndAction { toolbar.visibility = View.GONE }
    }

    abstract fun getToolBarContainer(): View?
    protected fun checkToggleToolbar() {
        val toolbar = getToolBarContainer() ?: return
        if (!isToolbarShown && toolbar.visibility != View.GONE) {
            hideToolbar(toolbar)
        } else if (isToolbarShown && toolbar.visibility != View.VISIBLE) {
            showToolbar(toolbar)
        }
    }

    var favoriteMenuItem: MenuItem? = null

    fun addFavoriteSateObserver() {
        lifecycleScope.launch(viewModel.exceptionHandler) {
            viewModel.favoriteState.collectLatest {
                if (it.first == viewModel.currentSong) {
                    updateFavoriteIcon(it.second)
                }
            }
        }
    }

    /**
     * delayed and run on main-thread
     */
    fun updateFavoriteIcon(isFavorite: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val activity =
                    activity ?: return@postDelayed
                val res =
                    if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
                val color = toolbarIconColor(activity, Color.TRANSPARENT)
                favoriteMenuItem?.apply {
                    icon = activity.getTintedDrawable(res, color)
                    title =
                        if (isFavorite) getString(R.string.action_remove_from_favorites)
                        else getString(R.string.action_add_to_favorites)
                }
            }, 200
        )
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

    override fun onServiceConnected() {
        updateQueue()
        updateCurrentSong()
        viewModel.updateFavoriteState(MusicPlayerRemote.currentSong, context)
        viewModel.loadLyrics(MusicPlayerRemote.currentSong)
    }

    override fun onPlayingMetaChanged() {
        updateCurrentSong()
        updateQueuePosition()
        viewModel.updateFavoriteState(MusicPlayerRemote.currentSong, context)
        viewModel.loadLyrics(MusicPlayerRemote.currentSong)
    }

    override fun onQueueChanged() {
        updateQueue()
    }

    override fun onMediaStoreChanged() {
        updateQueue()
        viewModel.updateFavoriteState(MusicPlayerRemote.currentSong, context)
    }

    override fun onShuffleModeChanged() {
        refreshAdapter()
    }

    open fun onShow() {
        playbackControlsFragment.show()
    }

    open fun onHide() {
        playbackControlsFragment.hide()
        onBackPressed()
    }

    abstract fun onBackPressed(): Boolean

    protected open fun updateCurrentSong() {
        viewModel.updateCurrentSong(MusicPlayerRemote.currentSong, context)
        impl.onCurrentSongChanged()
    }

    protected open fun updateQueue() {
        refreshAdapter()
    }

    protected open fun updateQueuePosition() {
        playingQueueAdapter.current = MusicPlayerRemote.position
    }

    private fun refreshAdapter() {
        playingQueueAdapter.dataset = MusicPlayerRemote.playingQueue
        playingQueueAdapter.current = MusicPlayerRemote.position
    }

    interface Callbacks {
        fun onPaletteColorChanged()
    }

    companion object {
        const val UPDATE_LYRICS = 1001
        const val LYRICS = "lyrics"
    }

    internal interface Impl {
        fun init()
        fun onCurrentSongChanged()
        fun animateColorChange(newColor: Int)
        fun setUpPanelAndAlbumCoverHeight()
    }


    override val paletteColor
        @ColorInt get() = viewModel.paletteColor.value

    val paletteColorState = viewModel.paletteColor

    open fun updateColor(color: Int) = viewModel.updatePaletteColor(color)

    private fun setupPaletteColorObserver() {
        lifecycleScope.launch {
            updateColor(requireContext().primaryColor()) // init
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paletteColor.collect { newColor ->
                    impl.animateColorChange(newColor)
                    playbackControlsFragment.modifyColor(newColor)
                    callbacks.onPaletteColorChanged()
                }
            }
        }
    }
}
