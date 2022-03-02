/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer
import com.afollestad.materialcab.*
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.adapter.song.UniversalSongAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.menu.PlaylistMenuHelper.handleMenuClick
import player.phonograph.interfaces.CabHolder
import player.phonograph.interfaces.LoaderIds
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.misc.WrappedAsyncTaskLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.settings.PreferenceUtil
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.*

class PlaylistDetailActivity : AbsSlidingMusicPanelActivity(), SAFCallbackHandlerActivity {
    private lateinit var binding: ActivityPlaylistDetailBinding // init in OnCreate()

    // init/bind in OnCreate() -> bindingView()
    private lateinit var recyclerView: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var empty: TextView
    private lateinit var cabStub: ViewStub

    private lateinit var playlist: Playlist // init in OnCreate()

    private var cab: AttachedCab? = null
    private lateinit var songAdapter: UniversalSongAdapter // init in OnCreate() -> setUpRecyclerView()
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    private lateinit var loader: Loader // init in OnCreate()

    private var savedMessageBundle: Bundle? = null

    // for saf callback
    private lateinit var safLauncher: SafLauncher
    override fun getSafLauncher(): SafLauncher = safLauncher

    /* ********************
     *
     *  First Initialization
     *
     * ********************/

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        bindingViews()

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        Themer.setActivityToolbarColorAuto(this, mToolbar)

        playlist = intent.extras!!.getParcelable(EXTRA_PLAYLIST)!!

        safLauncher = SafLauncher(activityResultRegistry)
        lifecycle.addObserver(safLauncher)

        // Init RecyclerView and Adapter
        setUpRecyclerView()
        loader = Loader(this, playlist, songAdapter)

        setUpToolbar()

        LoaderManager.getInstance(this)
            .initLoader(LOADER_ID, null, loader)
    }
    private fun bindingViews() {
        mToolbar = binding.toolbar

        recyclerView = binding.recyclerView
        empty = binding.empty
        cabStub = binding.cabStub
    }
    override fun createContentView(): View {
        return wrapSlidingMusicPanel(binding.root)
    }

    private fun setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(
            this, recyclerView as FastScrollRecyclerView, ThemeColor.accentColor(this)
        )
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Init (song)adapter
        if (playlist is AbsCustomPlaylist) {
            songAdapter = UniversalSongAdapter(
                this,
                ArrayList(),
                UniversalSongAdapter.MODE_PLAYLIST_SMART,
                R.layout.item_list,
                CabCallBack(this)
            )
            recyclerView.adapter = songAdapter
        } else {
            recyclerViewDragDropManager = RecyclerViewDragDropManager()
            val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()

            songAdapter = UniversalSongAdapter(
                this,
                ArrayList(),
                UniversalSongAdapter.MODE_PLAYLIST_LOCAL,
                R.layout.item_list,
                CabCallBack(this)
            )
            wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(songAdapter)
            recyclerView.adapter = wrappedAdapter
            recyclerView.itemAnimator = animator
            recyclerViewDragDropManager!!.attachRecyclerView(recyclerView)
        }

        songAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }
    private fun checkIsEmpty() {
        empty.visibility =
            if (songAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun setUpToolbar() {
        mToolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setToolbarTitle(playlist.name)
    }
    private fun setToolbarTitle(title: String) {
        supportActionBar!!.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(
            if (playlist is AbsCustomPlaylist) R.menu.menu_smart_playlist_detail else R.menu.menu_playlist_detail,
            menu
        )
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shuffle_playlist -> {
                MusicPlayerRemote.openAndShuffleQueue(songAdapter.songs, true)
                return true
            }
            R.id.action_edit_playlist -> {
                startActivityForResult(
                    Intent(this, PlaylistEditorActivity::class.java).apply {
                        putExtra(EXTRA_PLAYLIST, playlist)
                    },
                    EDIT_PLAYLIST
                )
                return true
            }
            R.id.action_refresh -> {
                onMediaStoreChanged()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return handleMenuClick(this, playlist, item)
    }

    override fun onBackPressed() {
        cab?.let {
            if (it.isActive()) it.destroy()
            else {
                recyclerView.stopScroll()
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }

    /* *******************
     *
     *    States Changed
     *
     * *******************/

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        if (playlist !is AbsCustomPlaylist) {
            // Playlist deleted
            if (!PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
                finish()
                return
            }

            // Playlist renamed
            val playlistName = PlaylistsUtil.getNameForPlaylist(this, playlist.id)
            if (playlistName != playlist.name) {
                playlist = PlaylistsUtil.getPlaylist(this, playlist.id)
                setToolbarTitle(playlist.name)
            }
        }

        // don't forge this
        loader.updatePlaylist(playlist)

        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, loader)
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onDestroy() {
        try { backgroundCoroutineScope.coroutineContext[Job]?.cancel() } catch (e: java.lang.Exception) { Log.i("BackgroundCoroutineScope", e.message.orEmpty()) }
        recyclerViewDragDropManager?.let {
            it.release()
            recyclerViewDragDropManager = null
        }
        recyclerView.itemAnimator = null
        recyclerView.adapter = null
        wrappedAdapter?. let {
            WrapperAdapterUtils.releaseAll(it)
            wrappedAdapter = null
        }
        super.onDestroy()
    }

    private val backgroundCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /* *******************
     *
     *        Loader
     *
     * *******************/

    inner class Loader(private val context: AppCompatActivity, private var playlist: Playlist, private val adapter: UniversalSongAdapter) : LoaderManager.LoaderCallbacks<List<Song>> {
        override fun onCreateLoader(
            id: Int,
            args: Bundle?
        ): androidx.loader.content.Loader<List<Song>> {
            return AsyncPlaylistSongLoader(context, this.playlist)
        }

        override fun onLoadFinished(
            loader: androidx.loader.content.Loader<List<Song>>,
            data: List<Song>
        ) {
            this.adapter.linkedPlaylist = playlist
            this.adapter.songs = data
        }

        override fun onLoaderReset(loader: androidx.loader.content.Loader<List<Song>>) {
            this.adapter.songs = ArrayList()
        }
        fun updatePlaylist(playlist: Playlist) {
            this.playlist = playlist
        }
    }
    private class AsyncPlaylistSongLoader(context: Context, private val playlist: Playlist) :
        WrappedAsyncTaskLoader<List<Song>>(context) {
        override fun loadInBackground(): List<Song> {
            return if (playlist is AbsCustomPlaylist) {
                playlist.getSongs(context)
            } else {
                PlaylistSongLoader.getPlaylistSongList(context, playlist.id)
            }
        }
    }

    /* *******************
     *
     *     cabCallBack
     *
     * *******************/
    private inner class CabCallBack(private val activity: PlaylistDetailActivity) : CabHolder {
        override fun showCab(
            menuRes: Int,
            createCallback: CreateCallback,
            selectCallback: SelectCallback,
            destroyCallback: DestroyCallback
        ): AttachedCab {
            // finish existed cab
            cab?.let {
                if (cab.isActive()) cab.destroy()
            }

            // create new
            cab = createCab(R.id.cab_stub) {
                popupTheme(PreferenceUtil.getInstance(this@PlaylistDetailActivity).generalTheme)
                menu(menuRes)
                closeDrawable(R.drawable.ic_close_white_24dp)
                backgroundColor(literal = PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(activity)))
                onCreate(createCallback)
                onSelection(selectCallback)
                onDestroy(destroyCallback)
            }
            return cab as AttachedCab
        }
    }

    /* *******************
     *   companion object
     * *******************/

    companion object {
        private const val LOADER_ID = LoaderIds.PLAYLIST_DETAIL_ACTIVITY
        private const val TAG = "PlaylistDetail"
        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EDIT_PLAYLIST: Int = 100
    }
}
