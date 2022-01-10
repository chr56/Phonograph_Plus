/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.adapter.song

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Pair
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import chr_56.MDthemer.util.TintHelper
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

@Suppress("unused")
open class UniversalSongAdapter :
    AbsMultiSelectAdapter<UniversalSongAdapter.CommonSongViewHolder, Song>,
    SectionedAdapter,
    MaterialCab.Callback {

    @Suppress("JoinDeclarationAndAssignment")
    private val activity: AppCompatActivity
    private val mode: Int
    var itemLayoutRes: Int
        private set

    constructor(
        activity: AppCompatActivity,
        songs: List<Song>,
        mode: Int = MODE_COMMON,
        @LayoutRes
        layoutRes: Int,
        cabHolder: CabHolder?
    ) : super(
        activity, cabHolder, R.menu.menu_media_selection
    ) {
        this.activity = activity
        this.mode = mode
        this.itemLayoutRes = layoutRes
        this.songs = songs
        setHasStableIds(true)
    }

    var songs: List<Song>
        set(dataSet) {
            field = dataSet
            notifyDataSetChanged()
            updateHeader()
        }

    private val headerLayoutRes: Int
        get() = when (mode) {
            MODE_PLAYLIST_LOCAL, MODE_PLAYLIST_SMART -> R.layout.item_header_playlist
            else -> R.layout.item_list_single_row
        }

    protected val hasHeader: Boolean
        get() {
            return when (mode) {
                MODE_PLAYLIST_SMART, MODE_PLAYLIST_LOCAL -> true
                MODE_ALL_SONGS -> true
                else -> false
            }
        }

    var usePalette: Boolean = false

    var showSectionName: Boolean = true

    var linkedPlaylist: Playlist? = null

    var linkedAlbum: Album? = null

    override fun getItemId(position: Int): Long =
        when (hasHeader) {
            false -> songs[position].id
            true -> if (position > 0) songs[position - 1].id else -2 // position == 0
        }

    override fun getItemViewType(position: Int): Int =
        if (hasHeader && position == 0) ITEM_HEADER else ITEM_SONG

    override fun getIdentifier(position: Int): Song =
        when (hasHeader) {
            false -> songs[position]
            true -> if (position > 0) songs[position - 1] else Song.EMPTY_SONG
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonSongViewHolder {
        return if (viewType == ITEM_SONG) CommonSongViewHolder(
            LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        ) else /* viewType == ITEM_HEADER */ CommonSongViewHolder(
            LayoutInflater.from(activity).inflate(headerLayoutRes, parent, false)
        )
    }

    private var name: TextView? = null
    private var songCountText: TextView? = null
    private var durationText: TextView? = null
    private var path: TextView? = null

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
            val primaryColor = ThemeColor.primaryColor(activity)
            holder.itemView.findViewById<ConstraintLayout>(R.id.header)?.background = ColorDrawable(primaryColor)

            val textColor = MaterialColorHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(primaryColor))
            val iconColor = MaterialColorHelper.getSecondaryDisabledTextColor(activity, ColorUtil.isColorLight(primaryColor))

            holder.itemView.findViewById<ImageView>(R.id.icon)
                .also {
                    it.setImageDrawable(
                        TintHelper.createTintedDrawable(
                            AppCompatResources.getDrawable(activity, R.drawable.ic_queue_music_white_24dp), textColor
                        )
                    )
                }
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.name_icon), iconColor)
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.song_count_icon), iconColor)
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.duration_icon), iconColor)
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.path_icon), iconColor)

            // todo MODE detect

            name = holder.itemView.findViewById<TextView>(R.id.name_text)
                .also {
                    it.text = linkedPlaylist?.name ?: "-"
                    it.setTextColor(textColor)
                }
            songCountText = holder.itemView.findViewById<TextView>(R.id.song_count_text)
                .also {
                    it.text = linkedPlaylist?.let { MusicUtil.getSongCountString(activity, songs.size) } ?: "-"
                    it.setTextColor(textColor)
                }
            durationText = holder.itemView.findViewById<TextView>(R.id.duration_text)
                .also {
                    it.text = linkedPlaylist?.let { MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(activity, songs)) } ?: "-"
                    it.setTextColor(textColor)
                }
            path = holder.itemView.findViewById<TextView>(R.id.path_text)
                .also { it ->
                    it.text = linkedPlaylist?.let { playlist ->
                        if (playlist is AbsSmartPlaylist) "-" else
                            MediaStoreUtil.getPlaylistPath(activity, playlist)
                    } ?: "-"
                    it.setTextColor(textColor)
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
                        activity, ColorUtil.isColorLight(color)
                    )
                )
            }
        }
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId) // todo
    }

    override fun getSectionName(position: Int): String = // todo fix offset
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
            get() =
                if (hasHeader) bindingAdapterPosition - 1
                else bindingAdapterPosition

        protected open val song: Song
            get() = if (itemViewType == ITEM_HEADER) Song.EMPTY_SONG else songs[songPosition]

        protected open val menuRes: Int?
            get() = when (mode) {
                MODE_COMMON, MODE_ALL_SONGS, /*MODE_NO_COVER,*/ MODE_SEARCH -> MENU_LONG
                /*MODE_ARTIST,*/ MODE_ALBUM -> MENU_SHORT
                MODE_PLAYLIST_LOCAL, MODE_PLAYLIST_SMART -> MENU_SHORT
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
            return when {
                image == null && image?.visibility == View.VISIBLE -> false
                item.itemId == R.id.action_go_to_album -> {
                    NavigationUtil.goToAlbum(
                        activity, song.albumId,
                        Pair.create(image, activity.resources.getString(R.string.transition_album_art))
                    )
                    true
                }
                else -> false
            }
        }

        override fun onClick(v: View) {
            if (itemViewType == ITEM_HEADER) return

            when (isInQuickSelectMode) {
                true -> toggleChecked(bindingAdapterPosition)
                false -> MusicPlayerRemote.openQueue(songs, songPosition, true)
            }
        }
        override fun onLongClick(view: View): Boolean {
            return if (itemViewType != ITEM_HEADER) toggleChecked(bindingAdapterPosition) else false
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
//        const val MODE_NO_COVER = FEATURE_PLAIN
        const val MODE_COMMON = FEATURE_PLAIN + FEATURE_IMAGE
        const val MODE_ALL_SONGS = FEATURE_PLAIN + FEATURE_IMAGE + FEATURE_HEADER_SHUFFLE
        const val MODE_PLAYLIST_LOCAL = FEATURE_PLAIN + FEATURE_IMAGE + FEATURE_HEADER_SUMMARY + FEATURE_WITH_HANDLE + FEATURE_ORDERABLE + FEATURE_DELETABLE
        const val MODE_PLAYLIST_SMART = FEATURE_PLAIN + FEATURE_IMAGE + FEATURE_HEADER_SUMMARY
        const val MODE_PLAYING_QUEUE = FEATURE_PLAIN + FEATURE_NUMBER + FEATURE_ORDERABLE + FEATURE_DELETABLE
        const val MODE_ALBUM = FEATURE_PLAIN + FEATURE_NUMBER
//        const val MODE_ARTIST = FEATURE_PLAIN + FEATURE_IMAGE

        const val MODE_SEARCH = FEATURE_PLAIN

        // header & real songs
        private const val ITEM_HEADER = 0
        private const val ITEM_SONG = 1
    }
}
