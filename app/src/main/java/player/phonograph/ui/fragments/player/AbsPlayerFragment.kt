package player.phonograph.ui.fragments.player

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.chr56.android.menu_dsl.add
import com.github.chr56.android.menu_dsl.attach
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mt.util.color.toolbarContentColor
import player.phonograph.R
import player.phonograph.actions.injectPlayerToolbar
import player.phonograph.adapter.display.PlayingQueueAdapter
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.LyricsDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.dialogs.SongShareDialog
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment.Companion.VISIBILITY_ANIM_DURATION
import player.phonograph.util.FavoriteUtil.toggleFavorite
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil.goToAlbum
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.menu.onSongMenuItemClick
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity

abstract class AbsPlayerFragment :
    AbsMusicServiceFragment(),
    Toolbar.OnMenuItemClickListener,
    PaletteColorHolder {

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

    // toolbar
    protected lateinit var playerToolbar: Toolbar

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

    override fun onDetach() {
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initToolbar()
        setUpControllerFragment()
        setUpCoverFragment()

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

    private fun addLyricsObserver() {
        viewModel.backgroundCoroutine.launch {
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
        playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        attachSpecialMenuItem(playerToolbar.menu)
        injectPlayerToolbar(playerToolbar.menu, this)
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

    private fun attachSpecialMenuItem(menu: Menu) {
        requireActivity().attach(menu) {
            // todo
            rootMenu.add(this) {
                order = 0
                title = getString(R.string.lyrics)
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

            rootMenu.add(this) {
                order = 1
                title = getString(R.string.action_add_to_favorites)
                icon = requireContext()
                    .getTintedDrawable(R.drawable.ic_favorite_border_white_24dp, Color.WHITE) // default state
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                itemId = R.id.action_toggle_favorite
                onClick {
                    val result = toggleFavorite(requireContext(), viewModel.currentSong)
                    if (viewModel.currentSong.id == MusicPlayerRemote.currentSong.id && result) {
                        playerAlbumCoverFragment.showHeartAnimation()
                        viewModel.updateFavoriteState(viewModel.currentSong)
                    }
                    true
                }
            }.apply {
                favoriteMenuItem = this
            }
        }
    }

    var favoriteMenuItem: MenuItem? = null

    fun addFavoriteSateObserver() {
        viewModel.backgroundCoroutine.launch {
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
        Handler(Looper.getMainLooper()).postDelayed({
            val activity = activity ?: return@postDelayed
            val res = if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
            val color = toolbarContentColor(activity, Color.TRANSPARENT)
            favoriteMenuItem?.apply {
                icon = activity.getTintedDrawable(res, color)
                title =
                    if (isFavorite) getString(R.string.action_remove_from_favorites)
                    else getString(R.string.action_add_to_favorites)
            }
        }, 200)

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
