package player.phonograph.adapter.legacy

import coil.size.ViewSizeResolver
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.menu.multiItemsToolbar
import player.phonograph.adapter.base.MultiSelectionAdapterContract
import player.phonograph.adapter.base.MultiSelectionCabController
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
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
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
    cabController: MultiSelectionCabController? = null,
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>(),
    SectionedAdapter,
    MultiSelectionAdapterContract<Album> {


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
            cabController,
            { activity },
            multiSelectMenuHandler
        )

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder = ViewHolder(view)

    private fun getAlbumTitle(album: Album): String = album.title

    protected open fun getAlbumText(album: Album): String =
        buildInfoString(
            album.artistName, songCountString(activity, album.songs.size)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataSet[position]
        holder.bind(album, controller)
        loadAlbumCover(album, holder)
    }

    protected open fun setColors(color: Int, holder: ViewHolder) {
        holder.paletteColorContainer?.let { container ->
            container.setBackgroundColor(color)
            holder.title?.setTextColor(
                activity.primaryTextColor(color)
            )

            holder.text?.setTextColor(
                activity.secondaryTextColor(color)
            )
        }
    }

    protected open fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) return
        val context = holder.itemView.context
        loadImage(context) {
            data(album.safeGetFirstSong())
            size(ViewSizeResolver(holder.image!!))
            target(
                PaletteTargetBuilder(context)
                    .onStart {
                        holder.image!!.setImageResource(R.drawable.default_album_art)
                        setColors(context.getColor(R.color.defaultFooterColor), holder)
                    }
                    .onResourceReady { result, palette ->
                        holder.image!!.setImageDrawable(result)
                        if (usePalette) setColors(palette, holder)
                    }
                    .build()
            )
        }
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItem(datasetPosition: Int): Album = dataSet[datasetPosition]

    override fun getItems(): Iterable<Album> = dataSet


    private val multiSelectMenuHandler: ((Toolbar) -> Boolean)?
        get() = {
            multiItemsToolbar(it.menu, activity, controller)
        }

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

    inner class ViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        init {
            setImageTransitionName(activity.getString(R.string.transition_album_art))
            menu?.visibility = GONE
        }

        fun bind(item: Album, controller: MultiSelectionController<Album>) {
            itemView.isActivated = controller.isSelected(item)
            shortSeparator?.visibility = if (bindingAdapterPosition == itemCount - 1) GONE else VISIBLE
            title?.text = getAlbumTitle(item)
            text?.text = getAlbumText(item)


            itemView.setOnClickListener {
                if (controller.isInQuickSelectMode) {
                    controller.toggle(bindingAdapterPosition)
                } else {
                    NavigationUtil.goToAlbum(
                        activity,
                        dataSet[bindingAdapterPosition].id,
                        Pair.create(image, activity.resources.getString(R.string.transition_album_art))
                    )
                }
            }
            itemView.setOnLongClickListener {
                controller.toggle(bindingAdapterPosition)
            }
            itemView.isActivated = controller.isSelected(item)
        }
    }
}
