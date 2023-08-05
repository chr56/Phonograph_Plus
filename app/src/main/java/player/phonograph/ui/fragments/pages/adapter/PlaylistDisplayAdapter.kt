/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.mechanism.Favorite
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.util.text.makeSectionName
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

class PlaylistDisplayAdapter(
    activity: AppCompatActivity,
) : DisplayAdapter<Playlist>(activity, ArrayList(), R.layout.item_list_single_row) {

    override fun getSectionNameImp(position: Int): String {
        return when (Setting.instance.genreSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(dataset[position].name)
            else                 -> ""
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (dataset[position] is SmartPlaylist) SMART_PLAYLIST else DEFAULT_PLAYLIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Playlist> {
        val view = inflatedView(layoutRes, parent)
        return if (viewType == SMART_PLAYLIST) SmartPlaylistViewHolder(view) else CommonPlaylistViewHolder(view)
    }

    open class CommonPlaylistViewHolder(itemView: View) : DisplayViewHolder<Playlist>(itemView) {
        init {
            image?.also { image ->
                val iconPadding =
                    itemView.context.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
                image.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                image.setColorFilter(
                    resolveColor(itemView.context, R.attr.iconColor), PorterDuff.Mode.SRC_IN
                )
            }
        }

        override val defaultIcon: Drawable?
            get() = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_queue_music_white_24dp)

        override fun setImage(position: Int, dataset: List<Playlist>, usePalette: Boolean) {
            super.setImage(position, dataset, usePalette)
            image?.also {
                val playlist = dataset[position]
                it.setImageResource(getIconRes(playlist))
            }
        }

        @Suppress("DEPRECATION")
        private fun getIconRes(playlist: Playlist): Int = when {
            playlist is SmartPlaylist                               -> playlist.iconRes
            FavoritesStore.instance.containsPlaylist(playlist)      -> R.drawable.ic_pin_white_24dp
            Favorite.isFavoritePlaylist(itemView.context, playlist) -> R.drawable.ic_favorite_white_24dp
            else                                                    -> R.drawable.ic_queue_music_white_24dp
        }

    }

    inner class SmartPlaylistViewHolder(itemView: View) : CommonPlaylistViewHolder(itemView) {
        init {
            if (shortSeparator != null) {
                shortSeparator!!.visibility = View.GONE
            }
            itemView.setBackgroundColor(resolveColor(activity, androidx.cardview.R.attr.cardBackgroundColor))
            itemView.elevation =
                activity.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
        }
    }

    companion object {
        private const val SMART_PLAYLIST = 0
        private const val DEFAULT_PLAYLIST = 1
    }

}
