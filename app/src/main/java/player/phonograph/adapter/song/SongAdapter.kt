package player.phonograph.adapter.song

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import com.afollestad.materialcab.MaterialCab
import com.bumptech.glide.Glide
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import player.phonograph.R
import player.phonograph.adapter.base.AbsMultiSelectAdapter
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.database.mediastore.SongColumns
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.SortOrder
import player.phonograph.helper.menu.SongMenuHelper
import player.phonograph.helper.menu.SongsMenuHelper.handleMenuClick
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.PreferenceUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class SongAdapter @JvmOverloads constructor(
    @JvmField protected val activity: AppCompatActivity,
    dataSet: List<Song>,
    @LayoutRes protected var itemLayoutRes: Int,
    protected var usePalette: Boolean = false,
    cabHolder: CabHolder?,
    protected var showSectionName: Boolean = true
) : AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(
    activity, cabHolder, R.menu.menu_media_selection
),
    MaterialCab.Callback,
    SectionedAdapter {

    init {
        setHasStableIds(true)
    }

    var dataSet: List<Song> = dataSet
        get() = field
        protected set(dataSet: List<Song>) {
            field = dataSet
        }

    fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    fun usePalette(usePalette: Boolean) {
        this.usePalette = usePalette
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view)
    }
    protected open fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = dataSet[position]
        val isChecked = isChecked(song)
        holder.itemView.isActivated = isChecked
        if (holder.bindingAdapterPosition == itemCount - 1) {
            if (holder.shortSeparator != null) {
                holder.shortSeparator!!.visibility = View.GONE
            }
        } else {
            if (holder.shortSeparator != null) {
                holder.shortSeparator!!.visibility = View.VISIBLE
            }
        }
        if (holder.title != null) {
            holder.title!!.text = song.title
        }
        if (holder.text != null) {
            holder.text!!.text = getSongText(song)
        }
        loadAlbumCover(song, holder)
    }

    private fun setColors(color: Int, holder: ViewHolder) {
        if (holder.paletteColorContainer != null) {
            holder.paletteColorContainer!!.setBackgroundColor(color)
            if (holder.title != null) {
                holder.title!!.setTextColor(
                    MaterialColorHelper.getPrimaryTextColor(
                        activity,
                        ColorUtil.isColorLight(color)
                    )
                )
            }
            if (holder.text != null) {
                holder.text!!.setTextColor(
                    MaterialColorHelper.getSecondaryTextColor(
                        activity,
                        ColorUtil.isColorLight(color)
                    )
                )
            }
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
                    if (usePalette) setColors(color, holder) else setColors(
                        defaultFooterColor,
                        holder
                    )
                }
            })
    }

    protected open fun getSongText(song: Song): String {
        return MusicUtil.getSongInfoString(song)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): Song {
        return dataSet[position]
    }

    override fun getName(obj: Song): String {
        return obj.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun getSectionName(position: Int): String {
        if (!showSectionName) {
            return ""
        }
        var sectionName: String? = null
        when (PreferenceUtil.getInstance(activity).sortOrderSongColumn) {
            SongColumns.TITLE ->
                sectionName = dataSet[position].title
            SongColumns.ALBUM_NAME ->
                sectionName = dataSet[position].albumName
            SongColumns.ARTIST_NAME ->
                sectionName = dataSet[position].artistName
            SongColumns.YEAR ->
                return MusicUtil.getYearString(dataSet[position].year)
        }
        return MusicUtil.getSectionName(sectionName)
    }

    open inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        protected open val song: Song
            get() = dataSet[bindingAdapterPosition]

        protected open val menuRes: Int?
            get() = R.menu.menu_item_song

        init {
            setImageTransitionName(activity.getString(R.string.transition_album_art))
            setupMenuListener()
//            setupMenu()
        }
//        protected open fun setupMenu(@Nullable @MenuRes menuRes: Int?) {
//            val realMenuRes: Int = menuRes ?: R.menu.menu_item_song // default
//
//            menu?.setOnClickListener(object : SongMenuHelper.ClickMenuListener(activity, realMenuRes) {
//                override val song: Song
//                    get() = this@ViewHolder.song
//
//                override fun onMenuItemClick(item: MenuItem): Boolean {
//                    return onSongMenuItemClick(item) || super.onMenuItemClick(item)
//                }
//            })
//        }

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
                            Pair.create(
                                image,
                                activity.resources.getString(R.string.transition_album_art)
                            )
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

        override fun onLongClick(view: View): Boolean {
            return toggleChecked(bindingAdapterPosition)
        }
    }
}
