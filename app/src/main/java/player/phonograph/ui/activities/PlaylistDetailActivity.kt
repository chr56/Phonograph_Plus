/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.core.Themer
import com.afollestad.materialcab.MaterialCab
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.*
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
import player.phonograph.model.smartplaylist.HistoryPlaylist
import player.phonograph.model.smartplaylist.LastAddedPlaylist
import player.phonograph.model.smartplaylist.MyTopTracksPlaylist
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.*

class PlaylistDetailActivity : AbsSlidingMusicPanelActivity() {
    private lateinit var binding: ActivityPlaylistDetailBinding // init in OnCreate()

    // init/bind in OnCreate() -> bindingView()
    private lateinit var recyclerView: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var empty: TextView
    private lateinit var cabStub: ViewStub

    private lateinit var playlist: Playlist // init in OnCreate()

    private var cab: MaterialCab? = null
    private lateinit var songAdapter: UniversalSongAdapter // init in OnCreate() -> setUpRecyclerView()
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    private lateinit var loader: Loader // init in OnCreate()

    private var savedMessageBundle: Bundle? = null

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

        // Init RecyclerView and Adapter
        setUpRecyclerView()
        loader = Loader(this, playlist, songAdapter)

        setUpToolbar()

        LoaderManager.getInstance(this)
            .initLoader(LOADER_ID, null, loader)

        setupHandler()
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
                0,
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
                0,
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

    private fun setupHandler() {
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    REQUEST_CODE_SAVE_PLAYLIST -> {
                        savedMessageBundle = msg.data
                        // just save message bundle, then wait for uri
                    }
                }
            }
        }
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
            if (it.isActive) it.finish()
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
                playlist = MediaStoreUtil.getPlaylist(this, playlist.id)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SAVE_PLAYLIST) {
            data?.let { intent ->
                val uri = intent.data!!

                val bundle = savedMessageBundle ?: throw Exception("No Playlist to save?")

                val playlistType = bundle.getString(TYPE)!!
                var result: Short

                when (playlistType) {
                    NormalPlaylist ->
                        bundle.getLong(PLAYLIST_ID).let { result = FileSaver.savePlaylist(this, uri, it) }
                    MyTopTracksPlaylist ->
                        result =
                            FileSaver.savePlaylist(this, uri, MyTopTracksPlaylist(this))
                    LastAddedPlaylist ->
                        result = FileSaver.savePlaylist(this, uri, LastAddedPlaylist(this))
                    HistoryPlaylist ->
                        result = FileSaver.savePlaylist(this, uri, HistoryPlaylist(this))
                    else -> {
                        result = -1
                    }
                }
                // report result
                Toast.makeText(
                    this,
                    if (result.toInt() != 0) getText(R.string.failed) else getText(R.string.success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == EDIT_PLAYLIST) {
            onMediaStoreChanged() // refresh after editing
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

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
        override fun openCab(menuRes: Int, callback: MaterialCab.Callback?): MaterialCab {
            // finish existed cab
            cab?.also { if (it.isActive) it.finish() }
            // create new
            return MaterialCab(activity, R.id.cab_stub)
                .setMenu(menuRes)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(
                    PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(activity))
                )
                .start(callback)
                .apply {
                    cab = this // also set activity's cab to this
                }
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
