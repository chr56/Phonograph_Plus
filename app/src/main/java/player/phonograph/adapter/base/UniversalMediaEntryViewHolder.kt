package player.phonograph.adapter.base

import android.os.Build
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.R

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class UniversalMediaEntryViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    var image: ImageView? = null
    var imageText: TextView? = null
    var title: TextView? = null
    var text: TextView? = null
    var menu: View? = null
    var separator: View? = null
    var shortSeparator: View? = null
    var dragView: View? = null
    var paletteColorContainer: View? = null

    init {
        bindingViews()
    }

    protected fun setClickListener(clickListener: MediaEntryViewClickListener) {
        itemView.setOnClickListener(clickListener)
        itemView.setOnLongClickListener(clickListener)
    }

    private fun bindingViews() {
        // todo: use viewBinding
        image = itemView.findViewById(R.id.image)
        imageText = itemView.findViewById(R.id.image_text)
        title = itemView.findViewById(R.id.title)
        text = itemView.findViewById(R.id.text)
        menu = itemView.findViewById(R.id.menu)
        separator = itemView.findViewById(R.id.separator)
        dragView = itemView.findViewById(R.id.drag_view)
        paletteColorContainer = itemView.findViewById(R.id.palette_color_container)
    }

    protected fun setImageTransitionName(transitionName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            image?.transitionName = transitionName
        }
    }
}

interface MediaEntryViewClickListener :
    View.OnClickListener,
    OnLongClickListener {
    override fun onLongClick(v: View): Boolean {
        return false
    }

    override fun onClick(v: View) {}
}
