/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter

import coil.size.ViewSizeResolver
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.actions.menu.multiItemsToolbar
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.base.UniversalMediaEntryViewHolder
import player.phonograph.adapter.display.initMenu
import player.phonograph.coil.loadImage
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.NavigationUtil.goToAlbum
import player.phonograph.util.NavigationUtil.goToArtist
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ComponentActivity
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu


class SearchResultAdapter(
    activity: ComponentActivity, cabController: MultiSelectionCabController?,
) : MultiSelectAdapter<RecyclerView.ViewHolder, Any>(activity, cabController) {

    var dataSet: List<Any> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItem(datasetPosition: Int): Any = dataSet[datasetPosition]

    @SuppressLint("NotifyDataSetChanged")
    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            HEADER -> HeaderViewHolder(
                LayoutInflater.from(context).inflate(R.layout.sub_header, parent, false)
            )

            else   -> ItemViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_list, parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataSet[position]
        when (val itemViewType = getItemViewType(position)) {
            HEADER -> {
                holder as HeaderViewHolder
                holder.bind(item as String)
            }

            else   -> {
                holder as ItemViewHolder
                holder.bind(item, itemViewType)
                holder.itemView.setOnClickListener {
                    if (isInQuickSelectMode) {
                        toggleChecked(position)
                    } else {
                        holder.onClick()
                    }
                }
                holder.itemView.setOnLongClickListener {
                    if (!isInQuickSelectMode) toggleChecked(position)
                    true
                }
                holder.itemView.isActivated = isChecked(item)
            }
        }
    }

    override val multiSelectMenuHandler: (Toolbar) -> Boolean
        get() = {
            multiItemsToolbar(it.menu, context, checkedList, cabTextColorColor) {
                checkAll()
                true
            }
        }

    override fun getItemCount(): Int = dataSet.size

    override fun getItemViewType(position: Int): Int =
        when (dataSet[position]) {
            is Album  -> ALBUM
            is Artist -> ARTIST
            is Song   -> SONG
            else      -> HEADER
        }

    class ItemViewHolder(itemView: View) :
            UniversalMediaEntryViewHolder(itemView) {
        init {
            val context = itemView.context
            itemView.setBackgroundColor(resolveColor(context, androidx.cardview.R.attr.cardBackgroundColor))
            itemView.elevation = context.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
            shortSeparator?.visibility = View.GONE
            menu?.visibility = View.GONE
            when (itemViewType) {
                ALBUM  -> setImageTransitionName(context.getString(R.string.transition_album_art))
                ARTIST -> setImageTransitionName(context.getString(R.string.transition_artist_image))
                // else -> itemView.findViewById<View>(R.id.image_container)?.visibility = View.GONE
            }
        }

        private lateinit var item: Any

        fun bind(item: Any, itemViewType: Int) {
            val context = itemView.context
            this.item = item
            when (itemViewType) {
                SONG   -> {
                    menu?.visibility = View.VISIBLE
                    menu?.setOnClickListener {
                        PopupMenu(context, menu).apply {
                            (item as Song).initMenu(context, this.menu, showPlay = true)
                        }.show()
                    }
                    val song = item as Song
                    title?.text = song.title
                    text?.text = song.infoString()
                    loadImage(context) {
                        size(ViewSizeResolver(image!!))
                        data(song)
                        target(
                            onStart = { image!!.setImageResource(R.drawable.default_album_art) },
                            onSuccess = { image!!.setImageDrawable(it) }
                        )
                    }
                }

                ARTIST -> {
                    val artist = item as Artist
                    title?.text = artist.name
                    text?.text = artist.infoString(context)
                    loadImage(context) {
                        data(artist)
                        target(
                            onStart = { image!!.setImageResource(R.drawable.default_artist_image) },
                            onSuccess = { image!!.setImageDrawable(it) }
                        )
                    }
                }

                ALBUM  -> {
                    val album = item as Album
                    title?.text = album.title
                    text?.text = album.infoString(context)
                    loadImage(context) {
                        data(album.safeGetFirstSong())
                        target(
                            onStart = { image!!.setImageResource(R.drawable.default_album_art) },
                            onSuccess = { image!!.setImageDrawable(it) }
                        )
                    }
                }

                else   -> {}
            }
        }

        fun onClick() {
            when (itemViewType) {

                SONG   -> {
                    MusicPlayerRemote.playNow(item as Song)
                }

                ARTIST -> {
                    goToArtist(
                        itemView.context, (item as Artist).id,
                        Pair.create(
                            image, itemView.context.resources.getString(R.string.transition_artist_image)
                        )
                    )
                }

                ALBUM  -> {
                    goToAlbum(
                        itemView.context, (item as Album).id,
                        Pair.create(
                            image, itemView.context.resources.getString(R.string.transition_album_art)
                        )
                    )
                }
            }
        }

        // fun onLongClick(): Boolean {
        //     return when (itemViewType) {
        //         // SONG   -> true
        //         // ARTIST -> true
        //         // ALBUM  -> true
        //         else -> false
        //     }
        // }
    }

    class HeaderViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView) {
        fun bind(text: String) {
            title?.text = text
        }
    }

    companion object {
        private const val HEADER = 0
        private const val ALBUM = 1
        private const val ARTIST = 2
        private const val SONG = 3
    }
}
