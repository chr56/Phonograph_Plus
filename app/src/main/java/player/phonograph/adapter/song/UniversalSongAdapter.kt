/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.adapter.song

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Pair
import chr_56.MDthemer.core.ThemeColor
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
import player.phonograph.model.smartplaylist.AbsSmartPlaylist
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.PreferenceUtil

open class UniversalSongAdapter(val activity: AppCompatActivity, songs: List<Song>, private val mode: Int = MODE_COMMON, cabHolder: CabHolder) :
    AbsMultiSelectAdapter<UniversalSongAdapter.CommonSongViewHolder, Song>(
        activity, cabHolder, R.menu.menu_media_selection
    ),
    SectionedAdapter,
    MaterialCab.Callback {

    var songs: List<Song> = songs
        get() = field
        set(dataSet: List<Song>) {
            field = dataSet
            notifyDataSetChanged()
            updateHeader()
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
    val headerLayoutRes: Int
        get() = when (mode) {
            MODE_PLAYLIST_LOCAL, MODE_PLAYLIST_SMART -> R.layout.item_header_playlist
            else -> R.layout.item_list_single_row
        }

    protected val hasHeader: Boolean
        get() {
            return when (mode) {
                MODE_PLAYLIST_SMART, MODE_PLAYLIST_LOCAL -> true
                else -> false
            }
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

    init {
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long =
        if (hasHeader && position == 0) -2 else songs[position - 1].id

    override fun getItemViewType(position: Int): Int = if (hasHeader && position == 0) ITEM_HEADER else ITEM_SONG

    override fun getIdentifier(position: Int): Song {
        return if (hasHeader && position == 0) Song.EMPTY_SONG else songs[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonSongViewHolder {
        return if (viewType == ITEM_SONG) CommonSongViewHolder(
            LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        ) else /* viewType == ITEM_HEADER */ CommonSongViewHolder(
            LayoutInflater.from(activity).inflate(headerLayoutRes, parent, false)
        )
    }

    var name: TextView? = null
    var songCountText: TextView? = null
    var durationText: TextView? = null
    var path: TextView? = null

    override fun onBindViewHolder(holder: CommonSongViewHolder, position: Int) {
        if (holder.itemViewType == ITEM_SONG) {
            val song = if (hasHeader) songs[position - 1] else songs[position]

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
        } else /* holder.itemViewType == ITEM_HEADER */ {
            holder.itemView.findViewById<ConstraintLayout>(R.id.header)?.background = ColorDrawable(
                ThemeColor.primaryColor(activity)
            )

            // todo MODE detect

            name = holder.itemView.findViewById<TextView>(R.id.name_text)
                .also { it.text = linkedPlaylist?.name ?: "-" }
            songCountText = holder.itemView.findViewById<TextView>(R.id.song_count_text)
                .also {
                    it.text = linkedPlaylist?.let { MusicUtil.getSongCountString(activity, songs.size) } ?: "-"
                }
            durationText = holder.itemView.findViewById<TextView>(R.id.duration_text)
                .also {
                    it.text = linkedPlaylist?.let { MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(activity, songs)) } ?: "-"
                }
            path = holder.itemView.findViewById<TextView>(R.id.path_text)
                .also { it ->
                    it.text = linkedPlaylist?.let { playlist ->
                        if (playlist is AbsSmartPlaylist) "-" else
                            MediaStoreUtil.getPlaylistPath(activity, playlist)
                    } ?: "-"
                }
        }
    }
    private fun updateHeader() {
        name?.text = linkedPlaylist?.name ?: "-"
        songCountText?.text = linkedPlaylist?.let { MusicUtil.getSongCountString(activity, songs.size) } ?: "-"
        durationText?.text = linkedPlaylist?.let { MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(activity, songs)) } ?: "-"
        path?.text = linkedPlaylist?.let { playlist ->
            if (playlist is AbsSmartPlaylist) "-" else
                MediaStoreUtil.getPlaylistPath(activity, playlist)
        } ?: "-"
    }

    override fun getItemCount(): Int {
        return songs.size +
            if (hasHeader) 1 else 0
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
            if (hasHeader && position == 0) ""
            else
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
        // real position if header exists
        protected open val songPosition: Int
            get() = if (!hasHeader) bindingAdapterPosition
            else bindingAdapterPosition - 1

        protected open val song: Song
            get() = if (itemViewType == ITEM_HEADER) Song.EMPTY_SONG else songs[songPosition]

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
            if (itemViewType == ITEM_HEADER) return
            if (isInQuickSelectMode) {
                toggleChecked(songPosition)
            } else {
                MusicPlayerRemote.openQueue(songs, songPosition, true)
            }
        }
        override fun onLongClick(view: View): Boolean {
            return if (itemViewType != ITEM_HEADER) toggleChecked(songPosition) else false
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

        // header & real songs
        private const val ITEM_HEADER = 0
        private const val ITEM_SONG = 1
    }
}
