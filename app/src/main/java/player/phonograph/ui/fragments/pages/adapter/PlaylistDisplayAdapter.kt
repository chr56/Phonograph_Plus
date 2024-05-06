/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.actions.menu.ActionMenuProviders
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.util.text.makeSectionName
import util.theme.internal.resolveColor
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PlaylistDisplayAdapter(
    activity: AppCompatActivity,
) : DisplayAdapter<Playlist>(activity, ConstDisplayConfig(ItemLayoutStyle.LIST_SINGLE_ROW)) {

    override fun getSectionNameImp(position: Int): String {
        val sortMode = Setting(activity).Composites[Keys.playlistSortMode].data
        return when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(dataset[position].name)
            else                 -> ""
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (dataset[position] is SmartPlaylist) SMART_PLAYLIST else DEFAULT_PLAYLIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Playlist> {
        val view =
            LayoutInflater.from(activity)
                .inflate(ItemLayoutStyle.LIST_SINGLE_ROW.layout(), parent, false)
        return if (viewType == SMART_PLAYLIST) SmartPlaylistViewHolder(view) else CommonPlaylistViewHolder(view)
    }

    open class CommonPlaylistViewHolder(itemView: View) : DisplayViewHolder<Playlist>(itemView) {
        init {
            image?.also { image ->
                val iconPadding =
                    itemView.context.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
                image.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                image.setColorFilter(
                    itemView.context.resolveColor(
                        R.attr.iconColor,
                        itemView.context.getColor(R.color.icon_lightdark)
                    ), PorterDuff.Mode.SRC_IN
                )
            }
        }

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<Playlist>
            get() = ClickActionProviders.PlaylistClickActionProvider()

        override val menuProvider: ActionMenuProviders.ActionMenuProvider<Playlist>
            get() = ActionMenuProviders.PlaylistActionMenuProvider


        override val defaultIcon: Drawable?
            get() = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_queue_music_white_24dp)

        override fun getIcon(item: Playlist): Drawable? =
            AppCompatResources.getDrawable(itemView.context, getIconRes(item))

        private fun getIconRes(playlist: Playlist): Int = when {
            playlist is SmartPlaylist                      -> playlist.iconRes
            favoritesStore.containsPlaylist(playlist)      -> R.drawable.ic_pin_white_24dp
            isFavoritePlaylist(itemView.context, playlist) -> R.drawable.ic_favorite_white_24dp
            else                                           -> R.drawable.ic_queue_music_white_24dp
        }

        companion object {
            private val favoritesStore by GlobalContext.get().inject<FavoritesStore>()

            private fun isFavoritePlaylist(context: Context, playlist: Playlist): Boolean {
                return playlist.name == context.getString(R.string.favorites)
            }

        }

    }

    inner class SmartPlaylistViewHolder(itemView: View) : CommonPlaylistViewHolder(itemView) {
        init {
            if (shortSeparator != null) {
                shortSeparator!!.visibility = View.GONE
            }
            itemView.setBackgroundColor(
                activity.resolveColor(
                    androidx.cardview.R.attr.cardBackgroundColor,
                    activity.getColor(R.color.card_background_lightdark)
                )
            )
            itemView.elevation =
                activity.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
        }
    }

    companion object {
        private const val SMART_PLAYLIST = 0
        private const val DEFAULT_PLAYLIST = 1
    }

}
