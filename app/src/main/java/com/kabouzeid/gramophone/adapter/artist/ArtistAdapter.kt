package com.kabouzeid.gramophone.adapter.artist

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
import com.kabouzeid.gramophone.glide.ArtistGlideRequest
import com.kabouzeid.gramophone.glide.PhonographColoredTarget
import com.kabouzeid.gramophone.helper.SortOrder
import com.kabouzeid.gramophone.helper.menu.SongsMenuHelper.handleMenuClick
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Artist
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.NavigationUtil
import com.kabouzeid.gramophone.util.PreferenceUtil
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class ArtistAdapter(
    private val activity: AppCompatActivity,
    private var dataSet: List<Artist>,
    @LayoutRes private var itemLayoutRes: Int,
    private var usePalette: Boolean = false,
    cabHolder: CabHolder?
) : AbsMultiSelectAdapter<ArtistAdapter.ViewHolder?, Artist?>(
    activity, cabHolder, R.menu.menu_media_selection
),
    SectionedAdapter {

    init {
        setHasStableIds(true)
    }

    fun swapDataSet(dataSet: List<Artist>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    fun usePalette(usePalette: Boolean) {
        this.usePalette = usePalette
        notifyDataSetChanged()
    }

    fun getDataSet(): List<Artist>{
        return dataSet
    }


    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view)
    }

    private inline fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = dataSet[position]
        val isChecked = isChecked(artist)
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
            holder.title!!.text = artist.name
        }
        if (holder.text != null) {
            holder.text!!.text = MusicUtil.getArtistInfoString(activity, artist)
        }
        holder.itemView.isActivated = isChecked(artist)
        loadArtistImage(artist, holder)
    }

    fun setColors(color: Int, holder: ViewHolder) {
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

    fun loadArtistImage(artist: Artist?, holder: ViewHolder) {
        if (holder.image == null) return
        ArtistGlideRequest.Builder.from(Glide.with(activity), artist)
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

    override fun getIdentifier(position: Int): Artist {
        return dataSet[position]
    }

    override fun getName(obj: Artist?): String {
        return obj!!.name
    }
    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Artist?>) {
        handleMenuClick(activity, getSongList(selection as List<Artist>), menuItem.itemId)
    }

    private fun getSongList(artists: List<Artist>): List<Song?> {
        val songs: MutableList<Song?> = ArrayList()
        for (artist in artists) {
            songs.addAll(artist.songs)
            // maybe async in future?  Todo
        }
        return songs
    }

    override fun getSectionName(position: Int): String {
        var sectionName: String? = null
        when (PreferenceUtil.getInstance(activity).artistSortOrder) {
            SortOrder.ArtistSortOrder.ARTIST_A_Z, SortOrder.ArtistSortOrder.ARTIST_Z_A ->
                sectionName =
                    dataSet[position].name
        }
        return MusicUtil.getSectionName(sectionName)
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                val artistPairs = arrayOf<Pair<*, *>>(
                    Pair.create(
                        image,
                        activity.resources.getString(R.string.transition_artist_image)
                    )
                )
                NavigationUtil.goToArtist(activity, dataSet[bindingAdapterPosition].id, *artistPairs)
            }
        }

        override fun onLongClick(view: View): Boolean {
            toggleChecked(bindingAdapterPosition)
            return true
        }

        init {
            setImageTransitionName(activity.getString(R.string.transition_artist_image))
            if (menu != null) {
                menu!!.visibility = View.GONE
            }
        }
    }
}
