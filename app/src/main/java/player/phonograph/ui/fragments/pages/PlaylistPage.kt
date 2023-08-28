/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import mt.pref.accentColor
import mt.pref.primaryColor
import mt.util.color.lightenColor
import org.koin.core.context.GlobalContext
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.misc.PlaylistsModifiedReceiver
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.model.playlist.HistoryPlaylist
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.dialogs.CreatePlaylistDialog
import player.phonograph.ui.fragments.pages.adapter.PlaylistDisplayAdapter
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.CoroutineScope

class PlaylistPage : AbsDisplayPage<Playlist, DisplayAdapter<Playlist>>() {

    override val viewModel: AbsDisplayPageViewModel<Playlist> get() = _viewModel

    private val _viewModel: PlaylistPageViewModel by viewModels()

    class PlaylistPageViewModel : AbsDisplayPageViewModel<Playlist>() {
        private val favoritesStore by GlobalContext.get().inject<FavoritesStore>()
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Playlist> {
            return mutableListOf<Playlist>(
                LastAddedPlaylist(context),
                HistoryPlaylist(context),
                MyTopTracksPlaylist(context),
            ).also {
                if (!Setting.instance.useLegacyFavoritePlaylistImpl) it.add(FavoriteSongsPlaylist(context))
            }.also {
                val allPlaylist = PlaylistLoader.all(context)
                val (pined, normal) = allPlaylist.partition {
                    favoritesStore.containsPlaylist(it.id, it.associatedFilePath)
                }
                it.addAll(pined)
                it.addAll(normal)
            }
        }

        override val headerTextRes: Int get() = R.plurals.item_playlists
    }

    // private _viewModel:

    //region MediaStore & FloatingActionButton

    private lateinit var playlistsModifiedReceiver: PlaylistsModifiedReceiver
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // PlaylistsModifiedReceiver
        playlistsModifiedReceiver = PlaylistsModifiedReceiver(adapter::notifyDataSetChanged)
        LocalBroadcastManager.getInstance(App.instance).registerReceiver(
            playlistsModifiedReceiver,
            IntentFilter().also { it.addAction(BROADCAST_PLAYLISTS_CHANGED) }
        )
        // AddNewItemButton
        setUpFloatingActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(App.instance).unregisterReceiver(playlistsModifiedReceiver)
    }
    //endregion

    override val displayConfigTarget: DisplayConfigTarget get() = DisplayConfigTarget.PlaylistPage

    override fun initAdapter(): DisplayAdapter<Playlist> {
        return PlaylistDisplayAdapter(
            hostFragment.mainActivity,
        ).apply {
            showSectionName = true
        }
    }


    override val availableSortRefs: Array<SortRef>
        get() = arrayOf(
            SortRef.DISPLAY_NAME,
            SortRef.PATH,
            SortRef.ADDED_DATE,
            SortRef.MODIFIED_DATE,
        )

    private fun setUpFloatingActionButton() {
        val primaryColor = addNewItemButton.context.primaryColor()
        val accentColor = addNewItemButton.context.accentColor()
        addNewItemButton.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(),
            ),
            intArrayOf(
                lightenColor(primaryColor), accentColor, primaryColor
            )
        )
        addNewItemButton.visibility = View.VISIBLE
        addNewItemButton.setOnClickListener {
            CreatePlaylistDialog.create(null).show(childFragmentManager, "CREATE_NEW_PLAYLIST")
        }
    }

    override fun allowColoredFooter(): Boolean = false

    companion object {
        const val TAG = "PlaylistPage"
    }
}