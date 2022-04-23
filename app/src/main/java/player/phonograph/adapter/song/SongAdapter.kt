package player.phonograph.adapter.song

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.bumptech.glide.Glide
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import java.text.SimpleDateFormat
import java.util.*
import player.phonograph.R
import player.phonograph.adapter.base.AbsMultiSelectAdapter
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.menu.SongMenuHelper
import player.phonograph.helper.menu.SongsMenuHelper.handleMenuClick
import player.phonograph.interfaces.CabHolder
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class SongAdapter constructor(
    protected val activity: AppCompatActivity,
    dataSet: List<Song>,
    protected var itemLayoutRes: Int,
    protected var usePalette: Boolean = false,
    cabHolder: CabHolder?,
    protected var showSectionName: Boolean = true
) :
    AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(
        activity, cabHolder, R.menu.menu_media_selection
    ),
    SectionedAdapter {

    init {
        setHasStableIds(true)
    }

    var dataSet: List<Song> = dataSet
        set(dataSet) {
            field = dataSet
            notifyDataSetChanged()
        }

    fun usePalette(usePalette: Boolean) {
        this.usePalette = usePalette
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long = dataSet[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }
    protected open fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = dataSet[position]

        holder.itemView.isActivated = isChecked(song)
        holder.title?.text = song.title
        holder.text?.text = getSongText(song)

        loadAlbumCover(song, holder)

        holder.shortSeparator?.visibility = if (holder.bindingAdapterPosition == itemCount - 1) View.GONE else View.VISIBLE
    }

    private fun setColors(color: Int, holder: ViewHolder) {
        holder.paletteColorContainer?.let {
            it.setBackgroundColor(color)
            holder.title?.setTextColor(
                MaterialColorHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color))
            )
            holder.text?.setTextColor(
                MaterialColorHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color))
            )
        }
    }

    protected open fun loadAlbumCover(song: Song, holder: ViewHolder) {
        if (holder.image == null) return
        SongGlideRequest.Builder.from(Glide.with(activity), song)
            .checkIgnoreMediaStore(activity)
            .generatePalette(activity).build()
            .into(object : PhonographColoredTarget(holder.image) {

                override fun onLoadCleared(placeholder: Drawable?) {
                    super.onLoadCleared(placeholder)
                    setColors(defaultFooterColor, holder)
                }

                override fun onColorReady(color: Int) {
                    setColors(
                        if (usePalette) color else defaultFooterColor,
                        holder
                    )
                }
            })
    }

    protected open fun getSongText(song: Song): String = MusicUtil.getSongInfoString(song)

    override fun getItemCount(): Int = dataSet.size

    override fun getIdentifier(position: Int): Song = dataSet[position]

    override fun getName(obj: Song): String = obj.title

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun getSectionName(position: Int): String {
        if (!showSectionName) return ""
        val song = dataSet[position]
        val sectionName: String =
            when (Setting.instance.songSortMode.sortRef) {
                SortRef.SONG_NAME -> MusicUtil.getSectionName(song.title)
                SortRef.ARTIST_NAME -> MusicUtil.getSectionName(song.artistName)
                SortRef.ALBUM_NAME -> MusicUtil.getSectionName(song.albumName)
                SortRef.YEAR -> MusicUtil.getYearString(song.year)
                SortRef.SONG_DURATION -> MusicUtil.getReadableDurationString(song.duration)
                SortRef.MODIFIED_DATE -> SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(song.dateModified * 1000)
                SortRef.ADDED_DATE -> SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(song.dateAdded * 1000)
                else -> ""
            }
        return sectionName
    }

    open inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        protected open val song: Song
            get() = dataSet[bindingAdapterPosition]

        protected open val menuRes: Int?
            get() = R.menu.menu_item_song

        init {
            setImageTransitionName(activity.getString(R.string.transition_album_art))
            setupMenuListener()
        }

        private fun setupMenuListener() {
            menu?.setOnClickListener(object : SongMenuHelper.ClickMenuListener(activity, menuRes) {
                override val song: Song
                    get() = this@ViewHolder.song

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return onSongMenuItemClick(item) || super.onMenuItemClick(item)
                }
            })
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (image != null && image!!.visibility == View.VISIBLE) {
                when (item.itemId) {
                    R.id.action_go_to_album -> {
                        val albumPairs = arrayOf<Pair<*, *>>(
                            Pair.create(image, activity.resources.getString(R.string.transition_album_art))
                        )
                        NavigationUtil.goToAlbum(activity, song.albumId, *albumPairs)
                        return true
                    }
                }
            }
            return false
        }

        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                MusicPlayerRemote.openQueue(dataSet, bindingAdapterPosition, true)
            }
        }

        override fun onLongClick(v: View): Boolean {
            return toggleChecked(bindingAdapterPosition)
        }
    }
}
