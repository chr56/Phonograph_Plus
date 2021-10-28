/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialcab.MaterialCab
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.adapter.song.OrderablePlaylistSongAdapter
import player.phonograph.adapter.song.OrderablePlaylistSongAdapter.OnMoveItemListener
import player.phonograph.adapter.song.PlaylistSongAdapter
import player.phonograph.adapter.song.SongAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.menu.PlaylistMenuHelper
import player.phonograph.helper.menu.PlaylistMenuHelper.handleMenuClick
import player.phonograph.interfaces.CabHolder
import player.phonograph.interfaces.LoaderIds
import player.phonograph.loader.PlaylistLoader
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.misc.WrappedAsyncTaskLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.ViewUtil
import java.util.*

class NeoPlaylistDetailActivity : AbsSlidingMusicPanelActivity() {
    private lateinit var binding: ActivityPlaylistDetailBinding

    private lateinit var recyclerView: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var empty: TextView
    private lateinit var cabStub: ViewStub

    private lateinit var playlist: Playlist
//    private lateinit var songs: MutableList<PlaylistSong>

    private lateinit var cab: MaterialCab
    private lateinit var adapter: SongAdapter
    private lateinit var wrappedAdapter: RecyclerView.Adapter<*>
    private lateinit var recyclerViewDragDropManager: RecyclerViewDragDropManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)

        bindingViews()

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        playlist = intent.extras!!.getParcelable(EXTRA_PLAYLIST)!!
//        songs = PlaylistSongLoader.getPlaylistSongList(this, playlist.id) as MutableList<PlaylistSong>

        setUpRecyclerView()
        setUpToolbar()

        LoaderManager.getInstance(this)
            .initLoader<List<Song>>(LOADER_ID, null, Loader())

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
            this,
            recyclerView as FastScrollRecyclerView, ThemeColor.accentColor(this)
        )
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (playlist is AbsCustomPlaylist) {
            adapter = PlaylistSongAdapter(this, ArrayList(), R.layout.item_list, false, CabCallBack(this))
            recyclerView.adapter = adapter
        } else {
            recyclerViewDragDropManager = RecyclerViewDragDropManager()
            val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()

            adapter =
                OrderablePlaylistSongAdapter( // todo cab holder
                    this, ArrayList(), R.layout.item_list, false, CabCallBack(this),
                    object : OnMoveItemListener {
                        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
                            if (PlaylistsUtil.moveItem(
                                    this@NeoPlaylistDetailActivity,
                                    playlist.id, fromPosition, toPosition
                                )
                            ) {
//                                val song: Song = adapter.dataSet.removeAt(fromPosition)
//                                adapter.dataSet.add(toPosition, song)
//                                adapter.notifyItemMoved(fromPosition, toPosition)
                                val songs = adapter.dataSet as MutableList<Song>
                                val song = songs.removeAt(fromPosition)
                                songs.add(toPosition, song)
                                adapter.swapDataSet(songs)
                                // todo
                            }
                        }
                    }
                )

            wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(adapter)
            recyclerView.adapter = wrappedAdapter
            recyclerView.itemAnimator = animator
            recyclerViewDragDropManager.attachRecyclerView(recyclerView)
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }
    private fun checkIsEmpty() { empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE }

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
                MusicPlayerRemote.openAndShuffleQueue(adapter.dataSet, true)
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
        if (cab != null && cab.isActive) cab.finish() else {
            recyclerView.stopScroll()
            super.onBackPressed()
        }
    }

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
                playlist = PlaylistLoader.getPlaylist(this, playlist.id)
                setToolbarTitle(playlist.name)
            }
        }
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, Loader()) // todo
    }

    override fun onPause() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.cancelDrag()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.release()
//            recyclerViewDragDropManager = null
        }
        if (recyclerView != null) {
            recyclerView.itemAnimator = null
            recyclerView.adapter = null
//            recyclerView = null
        }
        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter)
//            wrappedAdapter = null
        }
//        adapter = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == PlaylistMenuHelper.TASK_ID_SAVE_PLAYLIST) {
            if (data != null) {
                data.data?.also {
                    PlaylistMenuHelper.handleSavePlaylist(this, it)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private inner class Loader : LoaderManager.LoaderCallbacks<List<Song>> {
        override fun onCreateLoader(
            id: Int,
            args: Bundle?
        ): androidx.loader.content.Loader<List<Song>> {
            return AsyncPlaylistSongLoader(this@NeoPlaylistDetailActivity, playlist)
        }

        override fun onLoadFinished(
            loader: androidx.loader.content.Loader<List<Song>>,
            data: List<Song>
        ) { adapter?.swapDataSet(data) }

        override fun onLoaderReset(loader: androidx.loader.content.Loader<List<Song>>) {
            adapter?.swapDataSet(ArrayList())
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
    private inner class CabCallBack(private val activity: NeoPlaylistDetailActivity) : CabHolder {
        override fun openCab(menuRes: Int, callback: MaterialCab.Callback?): MaterialCab {
            if (cab != null && cab.isActive) cab.finish()
            cab = MaterialCab(activity, R.id.cab_stub)
                .setMenu(menuRes)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(
                    PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(activity))
                )
                .start(callback)
            return cab
        }
    }

    companion object {
        private const val LOADER_ID = LoaderIds.PLAYLIST_DETAIL_ACTIVITY
        private const val TAG = "PlaylistDetail"
        const val EXTRA_PLAYLIST = "extra_playlist"
    }
}
