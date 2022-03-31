package player.phonograph.ui.fragments.player

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.dialogs.*
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.AbsLyrics
import player.phonograph.model.lyrics2.LyricsLoader
import player.phonograph.model.lyrics2.LyricsPack
import player.phonograph.model.lyrics2.getLyrics
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.util.FavoriteUtil.toggleFavorite
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil.goToAlbum
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.NavigationUtil.openEqualizer
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity
import java.io.File

abstract class AbsPlayerFragment :
    AbsMusicServiceFragment(),
    Toolbar.OnMenuItemClickListener,
    PaletteColorHolder {

    protected var callbacks: Callbacks? = null
        private set
    protected lateinit var playerAlbumCoverFragment: PlayerAlbumCoverFragment // setUpSubFragments() in derived class //todo make sure field gets inited

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = try { context as Callbacks } catch (e: ClassCastException) { throw RuntimeException("${context.javaClass.simpleName} must implement ${Callbacks::class.java.simpleName}") }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val song = MusicPlayerRemote.getCurrentSong()

        when (item.itemId) {
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(requireActivity().supportFragmentManager, "SET_SLEEP_TIMER")
                return true
            }
            R.id.action_toggle_favorite -> {
                toggleFavorite(song)
                return true
            }
            R.id.action_share -> {
                SongShareDialog.create(song)
                    .show(requireActivity().supportFragmentManager, "SHARE_SONG")
                return true
            }
            R.id.action_equalizer -> {
                openEqualizer(requireActivity())
                return true
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(MutableList(1) { song })
                    .show(requireActivity().supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_clear_playing_queue -> {
                MusicPlayerRemote.clearQueue()
                return true
            }
            R.id.action_save_playing_queue -> {
                CreatePlaylistDialog.create(MusicPlayerRemote.getPlayingQueue())
                    .show(requireActivity().supportFragmentManager, "ADD_TO_PLAYLIST")
                return true
            }
            R.id.action_tag_editor -> {
                startActivity(
                    Intent(activity, SongTagEditorActivity::class.java)
                        .apply { putExtra(AbsTagEditorActivity.EXTRA_ID, song.id) }
                )
                return true
            }
            R.id.action_details -> {
                SongDetailDialog.create(song)
                    .show(requireActivity().supportFragmentManager, "SONG_DETAIL")
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
        }
        return false
    }

    protected val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    protected var lyricsPack: LyricsPack? = null
    protected var currentLyrics: AbsLyrics? = null

    private var loadLyricsJob: Job? = null
    private fun loadLyrics(song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        currentLyrics = null
        lyricsPack = null
        // load new lyrics
        loadLyricsJob = backgroundCoroutine.launch {
            lyricsPack = LyricsLoader.loadLyrics(File(song.data), song.title)
            currentLyrics = getLyrics(lyricsPack!!)
        }
    }

    private fun updateLyrics(lyrics: AbsLyrics) = runBlocking(Dispatchers.Main) {
        playerAlbumCoverFragment.setLyrics(lyrics)
        showLyricsMenuItem()
    }
    private fun clearLyrics() = backgroundCoroutine.launch(Dispatchers.Main) {
        playerAlbumCoverFragment.setLyrics(null)
        currentLyrics = null
        hideLyricsMenuItem()
    }

    protected fun loadAndRefreshLyrics(song: Song) {
        loadLyrics(song)
        clearLyrics()
        backgroundCoroutine.launch {
            // wait
            var timeout = 10
            while (lyricsPack == null || timeout <= 0) {
                delay(320)
                timeout -= 1
            }
            // refresh anyway
            currentLyrics?.let { updateLyrics(it) }
        }
    }

    protected abstract fun hideLyricsMenuItem()
    protected abstract fun showLyricsMenuItem()

    protected open fun toggleFavorite(song: Song) = toggleFavorite(requireActivity(), song)

    protected var isToolbarShown: Boolean
        get() = Companion.isToolbarShown
        set(toolbarShown) {
            Companion.isToolbarShown = toolbarShown
        }

    protected fun showToolbar(toolbar: View?) {
        if (toolbar == null) return
        isToolbarShown = true
        toolbar.visibility = View.VISIBLE
        toolbar.animate().alpha(1f).duration = PlayerAlbumCoverFragment.VISIBILITY_ANIM_DURATION.toLong()
    }

    protected fun hideToolbar(toolbar: View?) {
        if (toolbar == null) return
        isToolbarShown = false
        toolbar.animate().alpha(0f).setDuration(PlayerAlbumCoverFragment.VISIBILITY_ANIM_DURATION.toLong())
            .withEndAction { toolbar.visibility = View.GONE }
    }

    protected fun toggleToolbar(toolbar: View?) {
        if (isToolbarShown) {
            hideToolbar(toolbar)
        } else {
            showToolbar(toolbar)
        }
    }

    protected fun checkToggleToolbar(toolbar: View?) {
        if (toolbar != null && !isToolbarShown && toolbar.visibility != View.GONE) {
            hideToolbar(toolbar)
        } else if (toolbar != null && isToolbarShown && toolbar.visibility != View.VISIBLE) {
            showToolbar(toolbar)
        }
    }

    protected val upNextAndQueueTime: String
        get() {
            val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.getPosition())
            return MusicUtil.buildInfoString(
                resources.getString(R.string.up_next), MusicUtil.getReadableDurationString(duration)
            )
        }

    abstract fun onShow()
    abstract fun onHide()
    abstract fun onBackPressed(): Boolean

    interface Callbacks {
        fun onPaletteColorChanged()
    }

    companion object {
        private var isToolbarShown = true
    }

    internal interface Impl {
        fun init()
        fun updateCurrentSong(song: Song)
        fun animateColorChange(newColor: Int)
        fun setUpPanelAndAlbumCoverHeight()
    }
}
