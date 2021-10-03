package com.kabouzeid.gramophone.adapter.song

import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.afollestad.materialcab.MaterialCab
import com.bumptech.glide.Glide
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.glide.SongGlideRequest
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.helper.menu.SongMenuHelper.ClickMenuListener
import com.kabouzeid.gramophone.helper.menu.SongsMenuHelper.handleMenuClick
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.NavigationUtil

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
),
    MaterialCab.Callback {

    private var cab: MaterialCab? = null
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
                size <= 0 -> cab!!.finish()
                size > 0 -> cab!!.setTitle(
                    size.toString()
                )
            }
        }
    }

    private fun openCabIfNecessary() {
        if (cabHolder != null) {
            if (cab == null || !cab!!.isActive) {
                cab = cabHolder.openCab(R.menu.menu_media_selection, this)
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
        get() = cab != null && cab!!.isActive

    override fun onCabCreated(materialCab: MaterialCab, menu: Menu): Boolean {
        return true
    }

    override fun onCabItemClicked(menuItem: MenuItem): Boolean {
        onMultipleItemAction(menuItem, ArrayList(checked))
        cab!!.finish()
        unCheckAll()
        return true
    }

    override fun onCabFinished(materialCab: MaterialCab): Boolean {
        unCheckAll()
        return true
    }
}
