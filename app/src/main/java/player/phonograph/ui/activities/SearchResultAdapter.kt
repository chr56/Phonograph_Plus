/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import coil.size.ViewSizeResolver
import mt.util.color.resolveColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.adapter.IMultiSelectableAdapter
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.adapter.UniversalMediaEntryViewHolder
import player.phonograph.ui.adapter.initMenu
import player.phonograph.util.NavigationUtil.goToAlbum
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.NavigationUtil.goToPlaylist
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView


class SearchResultAdapter(
    val activity: ComponentActivity,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    IMultiSelectableAdapter<Any> {

    var dataSet: List<Any> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val controller: MultiSelectionController<Any> =
        MultiSelectionController(
            this,
            activity,
            true
        )

    override fun getItem(datasetPosition: Int): Any = dataSet[datasetPosition]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            SONG     -> SongViewHolder(parent.context, parent)
            ALBUM    -> AlbumViewHolder(parent.context, parent)
            ARTIST   -> ArtistViewHolder(parent.context, parent)
            PLAYLIST -> PlaylistViewHolder(parent.context, parent)
            QUEUE    -> PlayingQueueSongViewHolder(parent.context, parent)
            HEADER   -> HeaderViewHolder(parent.context, parent)
            else     -> throw IllegalStateException("Unknown view type: $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataSet[position]
        when (getItemViewType(position)) {
            HEADER   -> {
                holder as HeaderViewHolder
                holder.bind(item as String)
            }

            SONG     -> {
                holder as SongViewHolder
                holder.bind(item as Song)
                controller.registerClicking(holder.itemView, position, holder::onClick)
            }

            ALBUM    -> {
                holder as AlbumViewHolder
                holder.bind(item as Album)
                controller.registerClicking(holder.itemView, position, holder::onClick)
            }

            ARTIST   -> {
                holder as ArtistViewHolder
                holder.bind(item as Artist)
                controller.registerClicking(holder.itemView, position, holder::onClick)
            }

            PLAYLIST -> {
                holder as PlaylistViewHolder
                holder.bind(item as Playlist)
                controller.registerClicking(holder.itemView, position, holder::onClick)
            }

            QUEUE    -> {
                holder as PlayingQueueSongViewHolder
                holder.bind(item as PlayingQueueSong)
                controller.registerClicking(holder.itemView, position, holder::onClick)
            }
        }
        holder.itemView.isActivated = controller.isSelected(item)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItemViewType(position: Int): Int =
        when (dataSet[position]) {
            is Album            -> ALBUM
            is Artist           -> ARTIST
            is Song             -> SONG
            is Playlist         -> PLAYLIST
            is PlayingQueueSong -> QUEUE
            else                -> HEADER
        }

    abstract class AbsItemViewHolder<T : Any> protected constructor(itemView: View) :
            UniversalMediaEntryViewHolder(itemView) {

        init {
            val context = itemView.context
            itemView.setBackgroundColor(resolveColor(context, androidx.cardview.R.attr.cardBackgroundColor))
            itemView.elevation = context.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
        }

        protected lateinit var item: T

        abstract fun onClick(): Boolean
        abstract fun bind(item: T)

    }

    class SongViewHolder private constructor(itemView: View) : AbsItemViewHolder<Song>(itemView) {

        constructor(context: Context, parent: ViewGroup) :
                this(LayoutInflater.from(context).inflate(R.layout.item_list, parent, false))

        override fun bind(item: Song) {
            val context = itemView.context
            this.item = item

            menu?.visibility = View.VISIBLE
            menu?.setOnClickListener {
                PopupMenu(context, menu).apply {
                    item.initMenu(context, this.menu, showPlay = true)
                }.show()
            }

            title?.text = item.title
            text?.text = item.infoString()

            loadImage(context, item)

            shortSeparator?.visibility = View.GONE
        }

        fun loadImage(context: Context, item: Song) {
            loadImage(context) {
                size(ViewSizeResolver(image!!))
                data(item)
                target(
                    onStart = { image!!.setImageResource(R.drawable.default_album_art) },
                    onSuccess = { image!!.setImageDrawable(it) }
                )
            }
        }

        override fun onClick(): Boolean {
            return MusicPlayerRemote.playNow(item)
        }
    }

    class PlayingQueueSongViewHolder private constructor(itemView: View) : AbsItemViewHolder<PlayingQueueSong>(itemView) {

        constructor(context: Context, parent: ViewGroup) :
                this(LayoutInflater.from(context).inflate(R.layout.item_list, parent, false))

        override fun bind(item: PlayingQueueSong) {
            val context = itemView.context
            this.item = item

            menu?.visibility = View.VISIBLE
            menu?.setOnClickListener {
                PopupMenu(context, menu).apply {
                    item.song.initMenu(context, this.menu, showPlay = true, index = item.position)
                }.show()
            }

            title?.text = item.song.title
            text?.text = item.song.infoString()

            image?.visibility = View.INVISIBLE
            imageText?.visibility = View.VISIBLE
            if (item.position > -1)
                imageText?.text = item.position.toString()


            shortSeparator?.visibility = View.GONE
        }

        override fun onClick(): Boolean {
            return if (item.position > -1) {
                val queueManager = (itemView.context.applicationContext as App).queueManager
                queueManager.modifyPosition(item.position)
                true
            } else {
                false
            }
        }
    }

    class AlbumViewHolder private constructor(itemView: View) : AbsItemViewHolder<Album>(itemView) {

        constructor(context: Context, parent: ViewGroup) :
                this(LayoutInflater.from(context).inflate(R.layout.item_list, parent, false))

        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_album_art))
        }

        override fun bind(item: Album) {
            val context = itemView.context
            this.item = item
            title?.text = item.title
            text?.text = item.infoString(context)
            loadImage(context) {
                data(item.safeGetFirstSong())
                target(
                    onStart = { image!!.setImageResource(R.drawable.default_album_art) },
                    onSuccess = { image!!.setImageDrawable(it) }
                )
            }

            shortSeparator?.visibility = View.GONE
            menu?.visibility = View.GONE
        }

        override fun onClick(): Boolean {
            goToAlbum(
                itemView.context, item.id,
                Pair.create(
                    image, itemView.context.resources.getString(R.string.transition_album_art)
                )
            )
            return true
        }
    }

    class ArtistViewHolder private constructor(itemView: View) : AbsItemViewHolder<Artist>(itemView) {

        constructor(context: Context, parent: ViewGroup) :
                this(LayoutInflater.from(context).inflate(R.layout.item_list, parent, false))

        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_artist_image))
        }

        override fun bind(item: Artist) {
            val context = itemView.context
            this.item = item
            title?.text = item.name
            text?.text = item.infoString(context)
            loadImage(context) {
                data(item)
                target(
                    onStart = { image!!.setImageResource(R.drawable.default_artist_image) },
                    onSuccess = { image!!.setImageDrawable(it) }
                )
            }

            shortSeparator?.visibility = View.GONE
            menu?.visibility = View.GONE
        }

        override fun onClick(): Boolean {
            goToArtist(
                itemView.context, item.id,
                Pair.create(
                    image, itemView.context.resources.getString(R.string.transition_artist_image)
                )
            )
            return true
        }
    }

    class PlaylistViewHolder private constructor(itemView: View) : AbsItemViewHolder<Playlist>(itemView) {

        constructor(context: Context, parent: ViewGroup) :
                this(LayoutInflater.from(context).inflate(R.layout.item_list_single_row, parent, false))


        override fun bind(item: Playlist) {
            val context = itemView.context
            this.item = item
            title?.text = item.name
            image?.setImageDrawable(context.getTintedDrawable(item.iconRes, context.getColor(R.color.grey_highlight)))

            shortSeparator?.visibility = View.GONE
            menu?.visibility = View.GONE
        }

        override fun onClick(): Boolean {
            goToPlaylist(itemView.context, item)
            return true
        }
    }

    class HeaderViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        constructor(context: Context, parent: ViewGroup) :
                this(LayoutInflater.from(context).inflate(R.layout.sub_header, parent, false))

        fun bind(text: String) {
            itemView.findViewById<TextView>(R.id.title)?.text = text
        }
    }

    data class PlayingQueueSong(val song: Song, val position: Int)

    companion object {
        private const val HEADER = 0
        private const val ALBUM = 1
        private const val ARTIST = 2
        private const val SONG = 3
        private const val PLAYLIST = 4
        private const val QUEUE = 5
    }
}
