package player.phonograph.adapter.legacy

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import mt.util.color.getPrimaryTextColor
import mt.util.color.getSecondaryTextColor
import mt.util.color.isColorLight
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.model.buildInfoString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.SortOrderSettings
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.menu.onMultiSongMenuItemClick

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
        buildInfoString(
            album.artistName, songCountString(activity, album.songs.size)
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
                getPrimaryTextColor(activity, isColorLight(color))
            )

            holder.text?.setTextColor(
                getSecondaryTextColor(activity, isColorLight(color))
            )
        }
    }

    protected open fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) return
        loadImage(context) {
            data(album.safeGetFirstSong())
            size(holder.image!!.maxWidth, holder.image!!.maxHeight)
            target(
                PaletteTargetBuilder(context)
                    .onStart {
                        holder.image!!.setImageResource(R.drawable.default_album_art)
                        setColors(context.getColor(R.color.defaultFooterColor), holder)
                    }
                    .onResourceReady { result, palette ->
                        holder.image!!.setImageDrawable(result)
                        if (usePalette) setColors(palette, holder)
                    }
                    .build()
            )
        }
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
            when (SortOrderSettings.instance.albumSortMode.sortRef) {
                SortRef.ALBUM_NAME -> MusicUtil.getSectionName(album.title)
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(album.artistName)
                SortRef.YEAR -> getYearString(album.year)
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
