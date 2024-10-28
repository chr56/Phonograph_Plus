/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import org.koin.core.context.GlobalContext
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.misc.PlaylistsModifiedReceiver
import player.phonograph.model.Song
import player.phonograph.model.playlist.DynamicPlaylists
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.loader.Playlists
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.PlaylistDisplayAdapter
import player.phonograph.ui.modules.playlist.dialogs.CreatePlaylistDialogActivity
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.primaryColor
import util.theme.color.lightenColor
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context
import android.content.Intent
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
            val resources = context.resources
            return mutableListOf<Playlist>(
                DynamicPlaylists.lastAdded(resources),
                DynamicPlaylists.history(resources),
                DynamicPlaylists.myTopTrack(resources),
            ).also {
                if (!Setting(context)[Keys.useLegacyFavoritePlaylistImpl].data) {
                    it.add(DynamicPlaylists.favorites(resources))
                }
            }.also { playlists ->
                val (pined, normal) =
                    Playlists.all(context).partition { playlist ->
                        favoritesStore.containsPlaylist(playlist.mediaStoreId(), playlist.path())
                    }
                playlists.addAll(pined)
                playlists.addAll(normal)
            }
        }

        override suspend fun collectAllSongs(context: Context): List<Song> =
            Playlists.all(context).flatMap { PlaylistProcessors.reader(it).allSongs(context) }

        override val headerTextRes: Int get() = R.plurals.item_playlists
    }

    // private _viewModel:

    //region MediaStore & FloatingActionButton

    private lateinit var playlistsModifiedReceiver: PlaylistsModifiedReceiver
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // PlaylistsModifiedReceiver
        playlistsModifiedReceiver = object : PlaylistsModifiedReceiver() {
            override fun onPlaylistChanged(context: Context, intent: Intent) {
                viewModel.loadDataset(requireContext())
            }
        }
        LocalBroadcastManager.getInstance(App.instance).registerReceiver(
            playlistsModifiedReceiver, PlaylistsModifiedReceiver.filter
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
        return PlaylistDisplayAdapter(mainActivity)
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
            startActivity(
                CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForCreating(
                    requireContext(), emptyList()
                )
            )
        }
    }

    companion object {
        const val TAG = "PlaylistPage"
    }
}