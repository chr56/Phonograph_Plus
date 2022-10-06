package player.phonograph.adapter.legacy

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import player.phonograph.R
import player.phonograph.actions.applyToToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.initMenu
import player.phonograph.coil.loadImage
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.NavigationUtil
import player.phonograph.util.menu.MenuClickListener

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
        loadImage(activity) {
            data(song)
            size(albumArt.maxWidth, albumArt.maxHeight)
            target(
                onStart = { albumArt.setImageResource(R.drawable.default_artist_image) },
                onSuccess = { albumArt.setImageDrawable(it) }
            )
        }

        albumArt.transitionName = activity.getString(R.string.transition_album_art)
        convertView.findViewById<ImageView>(R.id.menu).setOnClickListener {
            PopupMenu(activity, it).apply {
                song.initMenu(activity, this.menu, transitionView = albumArt)
            }.show()
        }
        convertView.isActivated = isChecked(song)
        convertView.setOnClickListener {
            if (isInQuickSelectMode) {
                toggleChecked(song)
            } else {
                MusicPlayerRemote
                    .playQueueCautiously(dataSet, position, true, null)
            }
        }
        convertView.setOnLongClickListener {
            toggleChecked(song)
            true
        }
        return convertView
    }

    protected fun toggleChecked(song: Song) {
        if (cabController != null) {
            if (!checked.remove(song)) checked.add(song)
            notifyDataSetChanged()
            updateCab()
        }
    }

    private fun updateCab() {
        // todo
        cabController?.onDismiss = ::unCheckAll
        cabController?.menuHandler = {
            applyToToolbar(it.menu, context, checked, Color.WHITE) { true }
        }

        cabController?.showContent(context, checked.size) // todo: valid
    }

    private fun unCheckAll() {
        checked.clear()
        notifyDataSetChanged()
    }

    protected fun isChecked(song: Song): Boolean = checked.contains(song)

    val isInQuickSelectMode: Boolean
        get() = cabController != null && cabController.isActive()

}
