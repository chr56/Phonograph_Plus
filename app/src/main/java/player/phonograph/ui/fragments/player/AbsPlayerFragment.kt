package player.phonograph.ui.fragments.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.actions.injectPlayerToolbar
import player.phonograph.adapter.display.PlayingQueueAdapter
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.dialogs.SongShareDialog
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.util.NavigationUtil.goToAlbum
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.menu.onSongMenuItemClick
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity

abstract class AbsPlayerFragment :
    AbsMusicServiceFragment(),
    Toolbar.OnMenuItemClickListener,
    PaletteColorHolder {

    protected var callbacks: Callbacks? = null
        private set

    protected lateinit var playerAlbumCoverFragment: PlayerAlbumCoverFragment
    protected lateinit var playbackControlsFragment: AbsPlayerControllerFragment
    protected val viewModel: PlayerFragmentViewModel by viewModels()

    lateinit var handler: Handler

    // recycle view
    protected lateinit var layoutManager: LinearLayoutManager
    protected lateinit var playingQueueAdapter: PlayingQueueAdapter
    private var _wrappedAdapter: RecyclerView.Adapter<*>? = null
    protected val wrappedAdapter: RecyclerView.Adapter<*> get() = _wrappedAdapter!!
    private var _recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    protected val recyclerViewDragDropManager: RecyclerViewDragDropManager get() = _recyclerViewDragDropManager!!

    // toolbar
    protected lateinit var playerToolbar: Toolbar

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks =
            try { context as Callbacks } catch (e: ClassCastException) {
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

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initToolbar()
        setUpControllerFragment()
        setUpCoverFragment()

        viewModel.favoriteAnimateCallback = {
            if (viewModel.currentSong.id == MusicPlayerRemote.currentSong.id && it) playerAlbumCoverFragment.showHeartAnimation()
        }
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
        viewModel.favoriteAnimateCallback = null
        viewModel.favoriteMenuItem = null
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        // current song
        val song = MusicPlayerRemote.currentSong
        when (item.itemId) {
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(List(1) { song })
                    .show(childFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_details -> {
                SongDetailDialog.create(song)
                    .show(childFragmentManager, "SONG_DETAIL")
                return true
            }
            R.id.action_go_to_album -> {
                goToAlbum(requireActivity(), song.albumId)
                return true
            }
            R.id.action_go_to_artist -> {
                goToArtist(requireActivity(), song.artistId)
                return true
            }
            R.id.action_tag_editor -> {
                startActivity(
                    Intent(activity, SongTagEditorActivity::class.java)
                        .apply { putExtra(AbsTagEditorActivity.EXTRA_ID, song.id) }
                )
                return true
            }
            R.id.action_share -> {
                SongShareDialog.create(song)
                    .show(childFragmentManager, "SHARE_SONG")
                return true
            }
            else -> onSongMenuItemClick(requireActivity(), song, item.itemId)
        }
        return false
    }

    protected val backgroundCoroutine: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO + exceptionHandler)
    }

    protected val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            Log.w("AbsPlayerFragment", "Exception:\n${throwable.message}")
            ErrorNotification.postErrorNotification(throwable, null)
        }
    }

    private fun showLyrics(lyrics: LrcLyrics) =
        runBlocking(Dispatchers.Main + exceptionHandler) {
            playerAlbumCoverFragment.setLyrics(lyrics)
            viewModel.lyricsMenuItem?.isVisible = true
        }
    private fun hideLyrics() =
        backgroundCoroutine.launch(Dispatchers.Main + exceptionHandler) {
            playerAlbumCoverFragment.clearLyrics()
            viewModel.lyricsMenuItem?.isVisible = false
        }

    protected fun monitorLyricsState() {
        backgroundCoroutine.launch {
            hideLyrics()
            // wait
            withTimeout(8000) {
                while (viewModel.lyricsList == null) delay(150)
            }
            delay(100)
            // refresh anyway
            viewModel.currentLyrics?.let {
                if (it is LrcLyrics) showLyrics(it)
            }
        }
    }

    //
    // Toolbar
    //
    private fun initToolbar() {
        playerToolbar = getImplToolbar()
        playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        injectPlayerToolbar(playerToolbar.menu, this, viewModel)
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

    protected fun checkToggleToolbar(toolbar: View?) {
        if (toolbar == null) return
        if (!isToolbarShown && toolbar.visibility != View.GONE) {
            hideToolbar(toolbar)
        } else if (isToolbarShown && toolbar.visibility != View.VISIBLE) {
            showToolbar(toolbar)
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

    abstract fun onShow()
    abstract fun onHide()
    abstract fun onBackPressed(): Boolean

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
}
