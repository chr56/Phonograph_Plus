/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import org.koin.core.context.GlobalContext
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.mechanism.playlist2.PlaylistProcessors
import player.phonograph.misc.PlaylistsModifiedReceiver
import player.phonograph.model.Song
import player.phonograph.model.playlist2.DynamicPlaylists
import player.phonograph.model.playlist2.FilePlaylistLocation
import player.phonograph.model.playlist2.Playlist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.mediastore.loaders.PlaylistLoader2
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.Playlist2DisplayAdapter
import player.phonograph.ui.modules.playlist2.dialogs.CreatePlaylistDialog
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.primaryColor
import util.theme.color.lightenColor
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.CoroutineScope

class Playlist2Page : AbsDisplayPage<Playlist, DisplayAdapter<Playlist>>() {

    override val viewModel: AbsDisplayPageViewModel<Playlist> get() = _viewModel

    private val _viewModel: PlaylistPageViewModel by viewModels()

    class PlaylistPageViewModel : AbsDisplayPageViewModel<Playlist>() {
        private val favoritesStore by GlobalContext.get().inject<FavoritesStore>()
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Playlist> {
            return mutableListOf<Playlist>(
                DynamicPlaylists.lastAdded(),
                DynamicPlaylists.history(),
                DynamicPlaylists.myTopTrack(),
            ).also {
                if (!Setting(context)[Keys.useLegacyFavoritePlaylistImpl].data) {
                    it.add(DynamicPlaylists.favorite())
                }
            }.also { playlists ->
                val (pined, normal) =
                    PlaylistLoader2.all(context).partition { playlist ->
                        val path = (playlist.location as? FilePlaylistLocation)?.path!!
                        favoritesStore.containsPlaylist(playlist.id, path)
                    }
                playlists.addAll(pined)
                playlists.addAll(normal)
            }
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            PlaylistLoader2.all(context).flatMap { PlaylistProcessors.of(it).allSongs(context) }

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


    override fun displayConfig(): PageDisplayConfig = PlaylistPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Playlist> {
        return Playlist2DisplayAdapter(mainActivity)
    }

    // override fun configAppBarActionButton(menuContext: MenuContext) {}

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

    companion object {
        const val TAG = "PlaylistPage"
    }
}