package player.phonograph.adapter.base

import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.R

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class MediaEntryViewHolder : RecyclerView.ViewHolder, View.OnClickListener, View.OnLongClickListener {

    @JvmField
    var image: ImageView? = null

    @JvmField
    var imageText: TextView? = null

    @JvmField
    var title: TextView? = null

    @JvmField
    var text: TextView? = null

    @JvmField
    var menu: View? = null

    @JvmField
    var separator: View? = null

    @JvmField
    var shortSeparator: View? = null

    @JvmField
    var dragView: View? = null

    @JvmField
    var paletteColorContainer: View? = null

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(itemView: View) : super(itemView) {
        // todo viewBinding
        image = itemView.findViewById(R.id.image)
        imageText = itemView.findViewById(R.id.image_text)
        title = itemView.findViewById(R.id.title)
        text = itemView.findViewById(R.id.text)
        menu = itemView.findViewById(R.id.menu)
        separator = itemView.findViewById(R.id.separator)
        shortSeparator = itemView.findViewById(R.id.short_separator)
        dragView = itemView.findViewById(R.id.drag_view)
        paletteColorContainer = itemView.findViewById(R.id.palette_color_container)

        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    protected fun setImageTransitionName(transitionName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && image != null) {
            image!!.transitionName = transitionName
        }
    }

    override fun onLongClick(v: View): Boolean = false

    override fun onClick(v: View) {}
}
