package player.phonograph.adapter.song

import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.bumptech.glide.Glide
import player.phonograph.R
import player.phonograph.glide.SongGlideRequest
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.helper.menu.SongMenuHelper.ClickMenuListener
import player.phonograph.helper.menu.SongsMenuHelper.handleMenuClick
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.Song
import player.phonograph.util.NavigationUtil

// Todo: use AbsMultiSelectAdapter
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class ArtistSongAdapter(
    private val activity: AppCompatActivity,
    @JvmField var dataSet: List<Song>,
    private val cabHolder: CabHolder?
) : ArrayAdapter<Song>(
    activity, R.layout.item_list, dataSet
) {

    private var cab: AttachedCab? = null
    private val checked: MutableList<Song> = ArrayList<Song>()
//    init {
//        checked = ArrayList()
//    }
    fun getDataSet(): List<Song> = dataSet

    fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet
        clear()
        addAll(dataSet)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val song = getItem(position)!!
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_list, parent, false)
        }
        val songTitle = convertView!!.findViewById<TextView>(R.id.title)
        val songInfo = convertView.findViewById<TextView>(R.id.text)
        val albumArt = convertView.findViewById<ImageView>(R.id.image)
        val shortSeparator = convertView.findViewById<View>(R.id.short_separator)
        if (position == count - 1) {
            if (shortSeparator != null) {
                shortSeparator.visibility = View.GONE
            }
        } else {
            if (shortSeparator != null) {
                shortSeparator.visibility = View.VISIBLE
            }
        }
        songTitle.text = song.title
        songInfo.text = song.albumName
        SongGlideRequest.Builder.from(Glide.with(activity), song)
            .checkIgnoreMediaStore(activity).build()
            .into(albumArt)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        albumArt.transitionName = activity.getString(R.string.transition_album_art)
//        }
        val overflowButton = convertView.findViewById<ImageView>(R.id.menu)
        overflowButton.setOnClickListener(object : ClickMenuListener(activity, R.menu.menu_item_song_short) {
            override val song: Song
                get() = song

            override fun onMenuItemClick(item: MenuItem): Boolean {
                if (item.itemId == R.id.action_go_to_album) {
                    val albumPairs = arrayOf<Pair<*, *>>(
                        Pair.create(
                            albumArt,
                            activity.resources.getString(R.string.transition_album_art)
                        )
                    )
                    NavigationUtil.goToAlbum(activity, song.albumId, *albumPairs)
                    return true
                }
                return super.onMenuItemClick(item)
            }
        })
        convertView.isActivated = isChecked(song)
        convertView.setOnClickListener {
            if (isInQuickSelectMode) {
                toggleChecked(song)
            } else {
                MusicPlayerRemote.openQueue(dataSet, position, true)
            }
        }
        convertView.setOnLongClickListener {
            toggleChecked(song)
            true
        }
        return convertView
    }

    private fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        handleMenuClick(activity, selection, menuItem.itemId)
    }

    protected fun toggleChecked(song: Song) {
        if (cabHolder != null) {
            openCabIfNecessary()
            if (!checked.remove(song)) checked.add(song)
            notifyDataSetChanged()
            val size = checked.size
            when {
                size <= 0 -> cab!!.destroy()
                size > 0 -> cab!!.title(
                    literal = size.toString()
                )
            }
        }
    }

    private fun openCabIfNecessary() {
        if (cabHolder != null) {
            if (cab == null || !cab!!.isActive()) {
                cab = cabHolder.showCab(R.menu.menu_media_selection, this::onCabCreated, this::onCabItemClicked, this::onCabFinished)
            }
        }
    }

    private fun unCheckAll() {
        checked.clear()
        notifyDataSetChanged()
    }

    protected fun isChecked(song: Song): Boolean {
        return checked.contains(song)
    }

    protected val isInQuickSelectMode: Boolean
        get() = cab != null && cab!!.isActive()

    fun onCabCreated(materialCab: AttachedCab, menu: Menu): Boolean {
        return true
    }

    fun onCabItemClicked(menuItem: MenuItem): Boolean {
        onMultipleItemAction(menuItem, ArrayList(checked))
        cab!!.destroy()
        unCheckAll()
        return true
    }

    fun onCabFinished(materialCab: AttachedCab): Boolean {
        unCheckAll()
        return true
    }
}
