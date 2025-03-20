/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.views

import player.phonograph.R
import player.phonograph.databinding.ItemTextBinding
import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.file.Location
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import util.theme.color.primaryTextColor
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

@Suppress("JoinDeclarationAndAssignment")
class BreadCrumbView : FrameLayout {

    constructor (context: Context) : super(context)
    constructor (context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor (context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context, attrs, defStyleAttr, defStyleRes
    )

    var callBack: (Location) -> Unit
        get() = adapter.callBack
        set(value) {
            adapter.callBack = value
        }

    var location: Location? = null
        set(value) {
            field = value
            adapter.location = value
        }

    val recyclerView: RecyclerView
    val adapter: BreadCrumbAdapter
    val layoutManager: LinearLayoutManager

    init {
        recyclerView = RecyclerView(context)
        adapter = BreadCrumbAdapter(context)
        adapter.location = location
        layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager

        val drawable = context.getTintedDrawable(
            R.drawable.ic_keyboard_arrow_right_white_24dp,
            context.primaryTextColor(context.nightMode)
        )!!
        recyclerView.addItemDecoration(ItemDecorator(drawable))

        addView(
            recyclerView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    class BreadCrumbAdapter(val context: Context) :
            RecyclerView.Adapter<BreadCrumbAdapter.ViewHolder>() {

        private var segmentations: List<String> = emptyList()

        var callBack: (Location) -> Unit = {}

        var location: Location? = null
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                segmentations = (if (value != null) generateSegmentation(value.basePath) else emptyList())
                notifyDataSetChanged()
            }

        private fun generateSegmentation(path: String): List<String> = path.split("/").filter { it.isNotEmpty() }
        private fun locationAt(position: Int): Location? {
            val all = location ?: return null
            val root = all.volumeRootPath
            val base = segmentations.subList(0, position).fold("") { acc, s -> "$acc/$s" }
            val path = "$root$base"
            return Locations.from(path, context)
        }

        override fun getItemCount(): Int = segmentations.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemTextBinding.inflate(LayoutInflater.from(context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val text = if (position == 0) location?.volumeName ?: "" else segmentations[position - 1]
            val location = locationAt(position)
            holder.bind(text) { if (location != null) callBack(location) }
        }


        class ViewHolder(val viewBinding: ItemTextBinding) : RecyclerView.ViewHolder(viewBinding.root) {
            fun bind(segment: String, onClick: (View) -> Unit) {
                val context = viewBinding.root.context
                viewBinding.text.text = segment
                viewBinding.text.setTextColor(context.primaryTextColor(context.nightMode))
                itemView.setOnClickListener(onClick)
            }
        }
    }

    class ItemDecorator(val drawable: Drawable) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            if (parent.getChildAdapterPosition(view) == 0) return
            outRect.left = drawable.intrinsicWidth
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val centerHorizontal = parent.height / 2
            val top = centerHorizontal - drawable.intrinsicHeight / 2
            val bottom = centerHorizontal + drawable.intrinsicHeight / 2
            for (i in 1 until parent.childCount) {
                val item = parent.getChildAt(i)
                val left = item.left - drawable.intrinsicWidth
                val right = item.left
                drawable.setBounds(left, top, right, bottom)
                drawable.draw(c)
            }
        }
    }
}
