/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.click.listClick
import player.phonograph.model.Displayable
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu

abstract class DisplayAdapter<I : Displayable>(
    protected val activity: AppCompatActivity,
    dataSet: List<I>,
    @LayoutRes var layoutRes: Int,
    cfg: (DisplayAdapter<I>.() -> Unit)?,
) : RecyclerView.Adapter<DisplayAdapter.DisplayViewHolder>(),
    FastScrollRecyclerView.SectionedAdapter,
    IMultiSelectableAdapter<I> {

    var dataset: List<I> = dataSet
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        cfg?.invoke(this)
    }

    var usePalette: Boolean = false

    var useImageText: Boolean = false

    var showSectionName: Boolean = true


    protected val controller: MultiSelectionController<I> =
        MultiSelectionController(this, activity, allowMultiSelection)

    protected open val allowMultiSelection: Boolean get() = true

    override fun getItemId(position: Int): Long = dataset[position].getItemID()
    override fun getItem(datasetPosition: Int): I = dataset[datasetPosition]

    protected fun inflatedView(layoutRes: Int, parent: ViewGroup): View =
        LayoutInflater.from(activity).inflate(layoutRes, parent, false)

    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        val item: I = dataset[position]
        holder.bind(item, position, dataset, controller, useImageText, usePalette)
    }

    override fun getItemCount(): Int = dataset.size

    override fun getSectionName(position: Int): String =
        if (showSectionName) getSectionNameImp(position) else ""

    // for inheriting
    open fun getSectionNameImp(position: Int): String =
        dataset[position].defaultSortOrderReference()?.substring(0..1) ?: ""

    open class DisplayViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        fun <I : Displayable> bind(
            item: I,
            position: Int,
            dataset: List<I>,
            controller: MultiSelectionController<I>,
            useImageText: Boolean,
            usePalette: Boolean,
        ) {
            shortSeparator?.visibility = View.VISIBLE
            itemView.isActivated = controller.isSelected(item)
            title?.text = item.getDisplayTitle(context = itemView.context)
            text?.text = getDescription(item)
            if (useImageText) {
                setImageText(this, getRelativeOrdinalText(item))
            } else {
                setImage(position, dataset, usePalette)
            }
            controller.registerClicking(itemView, position) {
                onClick(position, dataset, image)
            }
            menu?.visibility = if (item.hasMenu()) View.VISIBLE else View.GONE
            menu?.setOnClickListener {
                onMenuClick(dataset, position, it)
            }
        }

        protected open fun <I : Displayable> onClick(position: Int, dataset: List<I>, imageView: ImageView?): Boolean {
            return listClick(dataset, position, itemView.context, imageView)
        }

        protected open fun <I : Displayable> onMenuClick(
            dataset: List<I>,
            bindingAdapterPosition: Int,
            menuButtonView: View,
        ) {
            if (dataset.isNotEmpty()) {
                PopupMenu(itemView.context, menuButtonView).apply {
                    dataset[bindingAdapterPosition].initMenu(itemView.context, this.menu)
                }.show()
            }
        }

        protected open fun <I : Displayable> getRelativeOrdinalText(item: I): String = "-"
        protected open fun <I : Displayable> getDescription(item: I): CharSequence? =
            item.getDescription(context = itemView.context)

        protected open fun <I : Displayable> setImage(
            position: Int,
            dataset: List<I>,
            usePalette: Boolean,
        ) {
            image?.also {
                it.visibility = View.VISIBLE
                it.setImageDrawable(defaultIcon)
            }
        }

        protected open fun setImageText(holder: DisplayViewHolder, text: String) {
            holder.imageText?.also {
                it.visibility = View.VISIBLE
                it.text = text
            }
        }

        protected open val defaultIcon =
            AppCompatResources.getDrawable(itemView.context, R.drawable.default_album_art)

        protected open fun setPaletteColors(color: Int) {
            paletteColorContainer?.let { paletteColorContainer ->
                val context = itemView.context
                paletteColorContainer.setBackgroundColor(color)
                title?.setTextColor(context.primaryTextColor(color))
                text?.setTextColor(context.secondaryTextColor(color))
            }
        }
    }
}
