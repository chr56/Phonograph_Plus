package player.phonograph.adapter.legacy

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.bumptech.glide.Glide
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.glide.SongGlideRequest
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.NavigationUtil
import player.phonograph.util.menu.MenuClickListener
import player.phonograph.util.menu.onMultiSongMenuItemClick

// Todo: use AbsMultiSelectAdapter
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class ArtistSongAdapter(
    private val activity: AppCompatActivity,
    private val cabController: MultiSelectionCabController?,
    dataSet: List<Song>,
) : ArrayAdapter<Song>(
    activity, R.layout.item_list, dataSet
) {

    private val checked: MutableList<Song> = ArrayList()

    var dataSet: List<Song> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getView(position: Int, convertView_: View?, parent: ViewGroup): View {
        var convertView = convertView_
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
        albumArt.transitionName = activity.getString(R.string.transition_album_art)
        val overflowButton = convertView.findViewById<ImageView>(R.id.menu)
        overflowButton.setOnClickListener(object : MenuClickListener(activity, R.menu.menu_item_song_short) {
            override val song: Song
                get() = song

            override fun onMenuItemClick(item: MenuItem): Boolean {
                if (item.itemId == R.id.action_go_to_album) {
                    val albumPairs = arrayOf<Pair<View, String>>(
                        Pair.create(albumArt, activity.resources.getString(R.string.transition_album_art))
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
                MusicPlayerRemote
                    .playQueueCautiously(dataSet,position,true,null)
            }
        }
        convertView.setOnLongClickListener {
            toggleChecked(song)
            true
        }
        return convertView
    }

    private fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        onMultiSongMenuItemClick(activity, selection, menuItem.itemId)
    }

    protected fun toggleChecked(song: Song) {
        if (cabController != null) {
            if (!checked.remove(song)) checked.add(song)
            notifyDataSetChanged()
            updateCab()
        }
    }

    private fun updateCab() {
        cabController?.showContent(context, checked.size, R.menu.menu_media_selection) // todo: valid
        // todo
        cabController?.onDismiss = ::unCheckAll
        cabController?.onMenuItemClick = ::onCabItemClicked
    }

    private fun unCheckAll() {
        checked.clear()
        notifyDataSetChanged()
    }

    protected fun isChecked(song: Song): Boolean = checked.contains(song)

    val isInQuickSelectMode: Boolean
        get() = cabController != null && cabController.isActive()

    fun onCabItemClicked(menuItem: MenuItem): Boolean {
        onMultipleItemAction(menuItem, ArrayList(checked))
        unCheckAll()
        return true
    }
}
