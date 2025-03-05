/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import org.koin.core.context.GlobalContext
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.broadcast.PlaylistsModifiedReceiver
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.playlist.DynamicPlaylists
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.loader.Playlists
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.GenericDisplayAdapter
import player.phonograph.ui.adapter.PlaylistBasicDisplayPresenter
import player.phonograph.ui.modules.playlist.dialogs.CreatePlaylistDialogActivity
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.themeCardBackgroundColor
import util.theme.color.lightenColor
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope

class NeoPlaylistPage : BasicDisplayPage<Playlist, GenericDisplayAdapter<Playlist>>() {

    private val _viewModel: PlaylistPageViewModel by viewModels()
    override val viewModel: AbsDisplayPageViewModel<Playlist> get() = _viewModel


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


    override val displayConfig: PageDisplayConfig get() = PlaylistPageDisplayConfig(requireContext())

    override fun createAdapter(): GenericDisplayAdapter<Playlist> {
        return PlaylistAdapter(requireActivity(), PlaylistDisplayPresenter.from(displayConfig))
    }

    override fun updateDisplayedItems(items: List<Playlist>) {
        adapter.dataset = items
    }

    override fun updatePresenterSettings(
        sortMode: SortMode,
        usePalette: Boolean,
        layoutStyle: ItemLayoutStyle,
    ) {
        adapter.presenter = PlaylistDisplayPresenter.from(sortMode, layoutStyle)
    }


    class PlaylistDisplayPresenter(
        sortMode: SortMode,
        override val layoutStyle: ItemLayoutStyle,
    ) : PlaylistBasicDisplayPresenter(sortMode) {

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_FIXED_ICON
        override val usePalette: Boolean = false

        override fun getIconRes(playlist: Playlist): Int = when {
            favoritesStore.containsPlaylist(playlist)  -> R.drawable.ic_pin_white_24dp
            isFavoritePlaylist(App.instance, playlist) -> R.drawable.ic_favorite_white_24dp
            else                                       -> playlist.iconRes
        }



        private val favoritesStore by GlobalContext.get().inject<FavoritesStore>()

        private fun isFavoritePlaylist(context: Context, playlist: Playlist): Boolean {
            return playlist.name == context.getString(R.string.favorites)
        }

        companion object {

            fun from(displayConfig: PageDisplayConfig): PlaylistDisplayPresenter =
                PlaylistDisplayPresenter(displayConfig.sortMode, displayConfig.layout)

            fun from(sortMode: SortMode, layoutStyle: ItemLayoutStyle): PlaylistDisplayPresenter =
                PlaylistDisplayPresenter(sortMode, layoutStyle)
        }
    }

    private class PlaylistAdapter(activity: FragmentActivity, presenter: DisplayPresenter<Playlist>) :
            GenericDisplayAdapter<Playlist>(activity, presenter) {

        override fun getItemViewType(position: Int): Int =
            if (dataset[position].isVirtual()) DYNAMIC_PLAYLIST else DEFAULT_PLAYLIST

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Playlist> {
            val view = LayoutInflater.from(activity).inflate(ItemLayoutStyle.LIST_SINGLE_ROW.layout(), parent, false)
            return if (viewType == DYNAMIC_PLAYLIST) SmartPlaylistViewHolder(view) else CommonPlaylistViewHolder(view)
        }

        class CommonPlaylistViewHolder(itemView: View) : DisplayViewHolder<Playlist>(itemView) {
            init {
                val context = itemView.context
                image?.also { image ->
                    val iconPadding = context.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
                    image.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                }
            }
        }

        class SmartPlaylistViewHolder(itemView: View) : DisplayViewHolder<Playlist>(itemView) {
            init {
                val context = itemView.context
                image?.also { image ->
                    val iconPadding = context.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
                    image.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                }
                shortSeparator?.visibility = View.GONE
                itemView.setBackgroundColor(themeCardBackgroundColor(context))
                itemView.elevation = context.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
            }
        }

        companion object {
            private const val DYNAMIC_PLAYLIST = 0
            private const val DEFAULT_PLAYLIST = 1
        }
    }

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

    private fun setUpFloatingActionButton() {
        val addNewItemButton = binding.addNewItem
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
        val initialMarginBottom = addNewItemButton.marginBottom
        ViewCompat.setOnApplyWindowInsetsListener(addNewItemButton) { view, windowInsets ->
            if (mainActivity.isBottomBarHidden) {
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = initialMarginBottom + insets.bottom
                }
                windowInsets
            } else {
                view.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = initialMarginBottom
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    //endregion
}