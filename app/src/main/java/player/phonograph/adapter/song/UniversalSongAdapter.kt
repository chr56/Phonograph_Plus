/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.adapter.song

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.SortOrder
import player.phonograph.helper.menu.SongMenuHelper
import player.phonograph.helper.menu.SongsMenuHelper
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.Album
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.PreferenceUtil

open class UniversalSongAdapter(val activity: AppCompatActivity, songs: List<Song>, private val mode: Int = MODE_COMMON, cabHolder: CabHolder) :
    AbsMultiSelectAdapter<UniversalSongAdapter.CommonSongViewHolder, Song>(
        activity, cabHolder, R.menu.menu_media_selection
    ),
    SectionedAdapter,
    MaterialCab.Callback {

    init {
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long = songs[position].id

    var songs: List<Song> = songs
        get() = field
        set(dataSet: List<Song>) {
            field = dataSet
            notifyDataSetChanged()
        }

    val itemLayoutRes: Int
        get() = when (mode) {
            MODE_COMMON, MODE_ALL_SONGS -> R.layout.item_list
            MODE_NO_COVER, MODE_SEARCH -> R.layout.item_list_no_image
            MODE_PLAYING_QUEUE, MODE_ALBUM -> R.layout.item_list // todo
            MODE_ARTIST, MODE_PLAYLIST_LOCAL, MODE_PLAYLIST_SMART -> R.layout.item_list
            MODE_GRID -> R.layout.item_grid
            else -> R.layout.item_list_no_image
        }

    var usePalette: Boolean = false
        set(value) { field = value }

    var showSectionName: Boolean = true
        get() = field
        set(value) { field = value }

    var linkedPlaylist: Playlist? = null
        get() = field
        set(value) { field = value }

    var linkedAlbum: Album? = null
        get() = field
        set(value) { field = value }

    override fun getIdentifier(position: Int): Song {
        return songs[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonSongViewHolder {
        return CommonSongViewHolder(
            LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CommonSongViewHolder, position: Int) {
        val song = songs[position]

        val isChecked = isChecked(song)
        holder.itemView.isActivated = isChecked

        holder.title?.text = song.title
        holder.text?.text = MusicUtil.getSongInfoString(song)
        holder.shortSeparator?.visibility = View.VISIBLE
        holder.image?.also {
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
                            defaultFooterColor, holder
                        )
                    }
                })
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    private fun setColors(color: Int, holder: CommonSongViewHolder) {
        holder.paletteColorContainer?.also {
            it.setBackgroundColor(color)
            holder.title?.also { textView ->
                textView.setTextColor(
                    MaterialColorHelper.getPrimaryTextColor(
                        activity, ColorUtil.isColorLight(color)
                    )
                )
            }
            holder.text?.also { textView ->
                textView.setTextColor(
                    MaterialColorHelper.getSecondaryTextColor(
                        activity,
                        ColorUtil.isColorLight(color)
                    )
                )
            }
        }
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId) // todo
    }

    override fun getSectionName(position: Int): String =
        MusicUtil.getSectionName(
            when (PreferenceUtil.getInstance(activity).songSortOrder) {
                SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A ->
                    songs[position].title
                SortOrder.SongSortOrder.SONG_ALBUM ->
                    songs[position].albumName
                SortOrder.SongSortOrder.SONG_ARTIST ->
                    songs[position].artistName
                SortOrder.SongSortOrder.SONG_YEAR ->
                    songs[position].year.let {
                        if (it> 0) it.toString() else "-"
                    }
//                    MusicUtil.getYearString(songs[position].year)
                else -> "-"
            }
        )

    open inner class CommonSongViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        protected open val song: Song
            get() = songs[bindingAdapterPosition]

        protected open val menuRes: Int?
            get() = when (mode) {
                MODE_COMMON, MODE_ALL_SONGS, MODE_NO_COVER, MODE_SEARCH -> MENU_LONG
                MODE_ARTIST, MODE_ALBUM -> MENU_SHORT
                MODE_PLAYLIST_LOCAL, MODE_PLAYLIST_SMART -> MENU_SHORT_PLAYLIST
                MODE_PLAYING_QUEUE -> MENU_QUEUE
                else -> R.menu.menu_item_song
            }

        init {
            setupMenuListener()
        }

        private fun setupMenuListener() {
            menu?.setOnClickListener(object : SongMenuHelper.ClickMenuListener(activity, menuRes) {
                override val song: Song
                    get() = this@CommonSongViewHolder.song

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return onSongMenuItemClick(item) || super.onMenuItemClick(item)
                }
            })
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            return image?.let {
                if (it.visibility == View.VISIBLE) false
                else if (item.itemId == R.id.action_go_to_album) {
                    NavigationUtil.goToAlbum(
                        activity, song.albumId,
                        Pair.create(
                            image, activity.resources.getString(R.string.transition_album_art)
                        )
                    )
                    return true
                } else false
            } ?: false
        }

        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                MusicPlayerRemote.openQueue(songs, bindingAdapterPosition, true)
            }
        }
        override fun onLongClick(view: View): Boolean {
            return toggleChecked(bindingAdapterPosition)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val FEATURE_GRID = 1 shl 8

        const val FEATURE_PLAIN = 0
        const val FEATURE_IMAGE = 1
        const val FEATURE_NUMBER = 1 shl 1
        const val FEATURE_WITH_HANDLE = 1 shl 2

        const val FEATURE_ORDERABLE = 1 shl 3
        const val FEATURE_DELETABLE = 1 shl 4

        const val FEATURE_HEADER_SHUFFLE = 1 shl 6
        const val FEATURE_HEADER_SUMMARY = 1 shl 7

        const val MENU_LONG = R.menu.menu_item_song
        const val MENU_SHORT = R.menu.menu_item_song_short
        const val MENU_LONG_PLAYLIST = R.menu.menu_item_playlist_song
        const val MENU_SHORT_PLAYLIST = R.menu.menu_item_playlist_song_short
        const val MENU_QUEUE = R.menu.menu_item_playing_queue_song

        const val MODE_GRID = FEATURE_GRID // no menu button ,so no ORDERABLE or DELETABLE
        const val MODE_NO_COVER = FEATURE_PLAIN
        const val MODE_COMMON = FEATURE_PLAIN + FEATURE_IMAGE
        const val MODE_ALL_SONGS = FEATURE_PLAIN + FEATURE_IMAGE + FEATURE_HEADER_SHUFFLE
        const val MODE_PLAYLIST_LOCAL = FEATURE_PLAIN + FEATURE_IMAGE + FEATURE_HEADER_SUMMARY + FEATURE_WITH_HANDLE + FEATURE_ORDERABLE + FEATURE_DELETABLE
        const val MODE_PLAYLIST_SMART = FEATURE_PLAIN + FEATURE_IMAGE + FEATURE_HEADER_SUMMARY
        const val MODE_PLAYING_QUEUE = FEATURE_PLAIN + FEATURE_NUMBER + FEATURE_ORDERABLE + FEATURE_DELETABLE
        const val MODE_ALBUM = FEATURE_PLAIN + FEATURE_NUMBER
        const val MODE_ARTIST = FEATURE_PLAIN + FEATURE_IMAGE

        const val MODE_SEARCH = MODE_NO_COVER
    }
}
