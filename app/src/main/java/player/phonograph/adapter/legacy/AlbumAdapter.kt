package player.phonograph.adapter.legacy

import coil.size.ViewSizeResolver
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.adapter.base.IMultiSelectableAdapter
import player.phonograph.adapter.base.MultiSelectionController
import player.phonograph.adapter.base.UniversalMediaEntryViewHolder
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.buildInfoString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.util.NavigationUtil
import player.phonograph.util.text.makeSectionName
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class AlbumAdapter(
    protected val activity: AppCompatActivity,
    dataSet: List<Album>,
    @LayoutRes protected val itemLayoutRes: Int,
    usePalette: Boolean = false,
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>(),
    SectionedAdapter,
    IMultiSelectableAdapter<Album> {


    var dataSet: List<Album> = dataSet
        @SuppressLint("NotifyDataSetChanged")
        set(dataSet) {
            field = dataSet
            notifyDataSetChanged()
        }

    var usePalette: Boolean = usePalette
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val controller: MultiSelectionController<Album> =
        MultiSelectionController(
            this,
            activity,
            true
        )

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = dataSet[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder = ViewHolder(view)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataSet[position]
        holder.bind(album, position, usePalette, dataSet, controller)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItem(datasetPosition: Int): Album = dataSet[datasetPosition]

    override fun getSectionName(position: Int): String {
        val album = dataSet[position]
        val sectionName: String =
            when (Setting.instance.albumSortMode.sortRef) {
                SortRef.ALBUM_NAME -> makeSectionName(album.title)
                SortRef.ARTIST_NAME -> makeSectionName(album.artistName)
                SortRef.YEAR -> getYearString(album.year)
                SortRef.SONG_COUNT -> album.songCount.toString()
                else -> ""
            }
        return sectionName
    }

    open class ViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        init {
            setImageTransitionName(itemView.context.getString(R.string.transition_album_art))
            menu?.visibility = GONE
        }

        fun bind(
            item: Album,
            position: Int,
            usePalette: Boolean,
            all: Collection<Album>,
            controller: MultiSelectionController<Album>,
        ) {
            val context = itemView.context

            itemView.isActivated = controller.isSelected(item)
            shortSeparator?.visibility = if (position == all.size - 1) GONE else VISIBLE
            title?.text = item.title
            text?.text = buildInfoString(item.artistName, songCountString(context, item.songs.size))

            loadImage(context) {
                data(item.safeGetFirstSong())
                size(ViewSizeResolver(image!!))
                target(
                    PaletteTargetBuilder(context)
                        .onStart {
                            image!!.setImageResource(R.drawable.default_album_art)
                            setColors(context, context.getColor(R.color.defaultFooterColor))
                        }
                        .onResourceReady { result, palette ->
                            image!!.setImageDrawable(result)
                            if (usePalette) setColors(context, palette)
                        }
                        .build()
                )
            }

            itemView.setOnClickListener {
                if (controller.isInQuickSelectMode) {
                    controller.toggle(position)
                } else {
                    NavigationUtil.goToAlbum(
                        context,
                        item.id,
                        Pair.create(image, context.resources.getString(R.string.transition_album_art))
                    )
                }
            }
            itemView.setOnLongClickListener {
                controller.toggle(position)
            }
            itemView.isActivated = controller.isSelected(item)
        }

        protected open fun setColors(context: Context, color: Int) {
            paletteColorContainer?.let { container ->
                container.setBackgroundColor(color)
                title?.setTextColor(context.primaryTextColor(color))
                text?.setTextColor(context.secondaryTextColor(color))
            }
        }

    }
}
