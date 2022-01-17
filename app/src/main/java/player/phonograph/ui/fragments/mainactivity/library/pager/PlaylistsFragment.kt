package player.phonograph.ui.fragments.mainactivity.library.pager

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.adapter.PlaylistAdapter
import player.phonograph.interfaces.LoaderIds
import player.phonograph.misc.PlaylistsModifiedReceiver
import player.phonograph.misc.WrappedAsyncTaskLoader
import player.phonograph.model.Playlist
import player.phonograph.model.smartplaylist.HistoryPlaylist
import player.phonograph.model.smartplaylist.LastAddedPlaylist
import player.phonograph.model.smartplaylist.MyTopTracksPlaylist
import player.phonograph.util.PlaylistsUtil
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
// todo null safety
class PlaylistsFragment :
    AbsLibraryPagerRecyclerViewFragment<PlaylistAdapter, LinearLayoutManager>(),
    LoaderManager.LoaderCallbacks<List<Playlist>> {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(LOADER_ID, null, this)
    }

    private var playlistsModifiedReceiver: PlaylistsModifiedReceiver? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistsModifiedReceiver = PlaylistsModifiedReceiver { loaderManager.restartLoader(LOADER_ID, null, this) }
        LocalBroadcastManager.getInstance(App.instance).registerReceiver(
            playlistsModifiedReceiver!!,
            IntentFilter().also { it.addAction(BROADCAST_PLAYLISTS_CHANGED) }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(App.instance).unregisterReceiver(playlistsModifiedReceiver!!)
        playlistsModifiedReceiver = null
    }

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(requireActivity())
    }

    override fun createAdapter(): PlaylistAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return PlaylistAdapter(libraryFragment!!.mainActivity, dataSet, R.layout.item_list_single_row, libraryFragment)
    }

    override val emptyMessage: Int = R.string.no_playlists

    override fun onMediaStoreChanged() {
        loaderManager.restartLoader(LOADER_ID, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Playlist>> {
        return AsyncPlaylistLoader(libraryFragment!!.mainActivity)
    }

    override fun onLoadFinished(loader: Loader<List<Playlist>>, data: List<Playlist>) {
        adapter!!.swapDataSet(data)
    }

    override fun onLoaderReset(loader: Loader<List<Playlist>>) {
        adapter!!.swapDataSet(ArrayList())
    }

    private class AsyncPlaylistLoader(context: Context?) : WrappedAsyncTaskLoader<List<Playlist>>(context) {
        override fun loadInBackground(): List<Playlist> =
            mutableListOf<Playlist>(
                LastAddedPlaylist(context), HistoryPlaylist(context), MyTopTracksPlaylist(context)
            ).also { it.addAll(PlaylistsUtil.getAllPlaylists(context)) }
    }

    companion object {
        private const val LOADER_ID = LoaderIds.PLAYLISTS_FRAGMENT
    }
}
