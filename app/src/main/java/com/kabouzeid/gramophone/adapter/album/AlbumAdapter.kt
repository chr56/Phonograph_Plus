package com.kabouzeid.gramophone.adapter.album

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import com.bumptech.glide.Glide
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.adapter.base.AbsMultiSelectAdapter
import com.kabouzeid.gramophone.adapter.base.MediaEntryViewHolder
import com.kabouzeid.gramophone.glide.PhonographColoredTarget
import com.kabouzeid.gramophone.glide.SongGlideRequest
import com.kabouzeid.gramophone.helper.SortOrder
import com.kabouzeid.gramophone.helper.menu.SongsMenuHelper.handleMenuClick
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Album
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.NavigationUtil
import com.kabouzeid.gramophone.util.PreferenceUtil
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class AlbumAdapter(
    protected val activity: AppCompatActivity,
    dataSet: List<Album>,
    @LayoutRes protected var itemLayoutRes: Int,
    protected var usePalette: Boolean = false,
    cabHolder: CabHolder?
) : AbsMultiSelectAdapter<AlbumAdapter.ViewHolder?, Album?>(
    activity, cabHolder, R.menu.menu_media_selection
), SectionedAdapter {

    var dataSet:List<Album> = dataSet
        get() = field
        protected set(dataSet: List<Album>) {
        field = dataSet
    }



    init {
        setHasStableIds(true)
    }

    fun usePalette(usePalette: Boolean) {
        this.usePalette = usePalette
        notifyDataSetChanged()
    }

    fun swapDataSet(dataSet: List<Album>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder {
        return ViewHolder(view)
    }

    protected fun getAlbumTitle(album: Album): String {
        return album.title
    }

    protected open fun getAlbumText(album: Album): String? {
        return MusicUtil.buildInfoString(
            album.artistName,
            MusicUtil.getSongCountString(activity, album.songs.size)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataSet[position]
        val isChecked = isChecked(album)
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
            holder.title!!.text = getAlbumTitle(album)
        }
        if (holder.text != null) {
            holder.text!!.text = getAlbumText(album)
        }
        loadAlbumCover(album, holder)
    }

    protected open fun setColors(color: Int, holder: ViewHolder) {
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

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getIdentifier(position: Int): Album {
        return dataSet[position]
    }

    override fun getName(obj: Album?): String {
        return obj!!.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Album?>) {
        handleMenuClick(activity, getSongList(selection as List<Album>), menuItem.itemId)
    } //todo

    private fun getSongList(albums: List<Album>): List<Song?> {
        val songs: MutableList<Song?> = ArrayList()
        for (album in albums) {
            songs.addAll(album.songs)
        }
        return songs
    }

    override fun getSectionName(position: Int): String {
        var sectionName: String? = null
        when (PreferenceUtil.getInstance(activity).albumSortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z, SortOrder.AlbumSortOrder.ALBUM_Z_A -> sectionName =
                dataSet[position].title
            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> sectionName = dataSet[position].artistName
            SortOrder.AlbumSortOrder.ALBUM_YEAR -> return MusicUtil.getYearString(
                dataSet[position].year
            )
        }
        return MusicUtil.getSectionName(sectionName)
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                val albumPairs = arrayOf<Pair<*, *>>(
                    Pair.create(
                        image,
                        activity.resources.getString(R.string.transition_album_art)
                    )
                )
                NavigationUtil.goToAlbum(activity, dataSet[bindingAdapterPosition].id, *albumPairs)
            }
        }

        override fun onLongClick(view: View): Boolean {
            toggleChecked(bindingAdapterPosition)
            return true
        }

        init {
            setImageTransitionName(activity.getString(R.string.transition_album_art))
            if (menu != null) {
                menu!!.visibility = View.GONE
            }
        }
    }

}