/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import mt.util.color.primaryTextColor
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.mechanism.Favorite
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.util.text.makeSectionName
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PlaylistDisplayAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    cfg: (DisplayAdapter<Playlist>.() -> Unit)?,
) : DisplayAdapter<Playlist>(
    activity, cabController,
    ArrayList(),
    R.layout.item_list_single_row, cfg
) {

    override fun getSectionNameImp(position: Int): String {
        return when (Setting.instance.genreSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> makeSectionName(dataset[position].name)
            else                 -> ""
        }
    }

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.also {
            val playlist = dataset[position]
            it.setImageDrawable(
                activity.getTintedDrawable(
                    getIconRes(playlist), activity.primaryTextColor(activity.nightMode)
                )
            )
        }
    }

    private fun getIconRes(playlist: Playlist): Int = when {
        playlist is SmartPlaylist                       -> playlist.iconRes
        Favorite.isFavoritePlaylist(activity, playlist) -> R.drawable.ic_favorite_white_24dp
        else                                            -> R.drawable.ic_queue_music_white_24dp
    }


    override val defaultIcon: Drawable?
        get() = AppCompatResources.getDrawable(activity, R.drawable.ic_queue_music_white_24dp)


    override fun getItemViewType(position: Int): Int =
        if (dataset[position] is SmartPlaylist) SMART_PLAYLIST else DEFAULT_PLAYLIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        val view = LayoutInflater.from(activity).inflate(layoutRes, parent, false)
        return if (viewType == SMART_PLAYLIST) SmartPlaylistViewHolder(view) else CommonPlaylistViewHolder(view)
    }

    inner class CommonPlaylistViewHolder(itemView: View) : DisplayViewHolder(itemView)
    inner class SmartPlaylistViewHolder(itemView: View) : DisplayViewHolder(itemView) {
        init {
            if (shortSeparator != null) {
                shortSeparator!!.visibility = View.GONE
            }
            itemView.setBackgroundColor(resolveColor(activity, androidx.cardview.R.attr.cardBackgroundColor))
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            itemView.elevation =
                activity.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
            // }
        }
    }

    companion object {
        private const val SMART_PLAYLIST = 0
        private const val DEFAULT_PLAYLIST = 1
    }

}
