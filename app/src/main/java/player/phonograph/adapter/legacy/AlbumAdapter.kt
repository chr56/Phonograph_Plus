package player.phonograph.adapter.legacy

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.bumptech.glide.Glide
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.util.menu.onMultiSongMenuItemClick
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class AlbumAdapter(
    protected val activity: AppCompatActivity,
    dataSet: List<Album>,
    @LayoutRes protected val itemLayoutRes: Int,
    usePalette: Boolean = false,
    cabController: MultiSelectionCabController? = null,
) :
    MultiSelectAdapter<AlbumAdapter.ViewHolder, Album>(activity, cabController), SectionedAdapter {

    override var multiSelectMenuRes: Int = R.menu.menu_media_selection

    var dataSet: List<Album> = dataSet
        set(dataSet) {
            field = dataSet
            notifyDataSetChanged()
        }

    var usePalette: Boolean = usePalette
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder = ViewHolder(view)

    protected fun getAlbumTitle(album: Album): String =
        album.title

    protected open fun getAlbumText(album: Album): String =
        MusicUtil.buildInfoString(
            album.artistName, MusicUtil.getSongCountString(activity, album.songs.size)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataSet[position]
        holder.itemView.isActivated = isChecked(album)
        holder.shortSeparator?.visibility =
            if (holder.bindingAdapterPosition == itemCount - 1) GONE else VISIBLE
        holder.title?.text = getAlbumTitle(album)
        holder.text?.text = getAlbumText(album)
        loadAlbumCover(album, holder)
    }

    protected open fun setColors(color: Int, holder: ViewHolder) {
        holder.paletteColorContainer?.let { container ->
            container.setBackgroundColor(color)
            holder.title?.setTextColor(
                MaterialColorHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color))
            )

            holder.text?.setTextColor(
                MaterialColorHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color))
            )
        }
    }

    protected open fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) return
        SongGlideRequest.Builder.from(Glide.with(activity), album.safeGetFirstSong())
            .checkIgnoreMediaStore(activity)
            .generatePalette(activity).build()
            .into(object : PhonographColoredTarget(holder.image) {
                override fun onLoadCleared(placeholder: Drawable?) {
                    super.onLoadCleared(placeholder)
                    setColors(defaultFooterColor, holder)
                }

                override fun onColorReady(color: Int) {
                    if (usePalette) setColors(color, holder) else setColors(
                        defaultFooterColor,
                        holder
                    )
                }
            })
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItem(datasetPosition: Int): Album = dataSet[datasetPosition]

    override fun getName(obj: Album): String = obj.title

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Album>) {
        onMultiSongMenuItemClick(activity, getSongList(selection), menuItem.itemId)
    } // todo

    private fun getSongList(albums: List<Album>): List<Song> {
        return albums.flatMap { album -> album.songs }
    }

    override fun getSectionName(position: Int): String {
        val album = dataSet[position]
        val sectionName: String =
            when (Setting.instance.albumSortMode.sortRef) {
                SortRef.ALBUM_NAME -> MusicUtil.getSectionName(album.title)
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(album.artistName)
                SortRef.YEAR -> MusicUtil.getYearString(album.year)
                SortRef.SONG_COUNT -> album.songCount.toString()
                else -> ""
            }
        return sectionName
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                NavigationUtil.goToAlbum(
                    activity,
                    dataSet[bindingAdapterPosition].id,
                    Pair.create(image, activity.resources.getString(R.string.transition_album_art))
                )
            }
        }

        override fun onLongClick(v: View): Boolean {
            toggleChecked(bindingAdapterPosition)
            return true
        }

        init {
            setImageTransitionName(activity.getString(R.string.transition_album_art))
            menu?.visibility = GONE
        }
    }

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)
}
