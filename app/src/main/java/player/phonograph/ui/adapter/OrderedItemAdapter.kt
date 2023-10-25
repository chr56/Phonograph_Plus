/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.model.Displayable
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

abstract class OrderedItemAdapter<I : Displayable>(
    protected val activity: FragmentActivity,
    protected val layoutRes: Int,
    protected var useImageText: Boolean = true,
    protected var showSectionName: Boolean = false,
) : RecyclerView.Adapter<OrderedItemAdapter.OrderedItemViewHolder<I>>(),
    FastScrollRecyclerView.SectionedAdapter,
    IMultiSelectableAdapter<I> {

    var dataset: List<I> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        @Suppress("LeakingThis")
        setHasStableIds(true)
    }


    protected val controller: MultiSelectionController<I>
            by lazy { MultiSelectionController(this, activity, allowMultiSelection) }

    protected open val allowMultiSelection: Boolean get() = true

    override fun getItemId(position: Int): Long = dataset[position].getItemID() // shl 3 + layoutType
    override fun getItem(datasetPosition: Int): I = dataset[datasetPosition]

    protected open fun inflatedView(parent: ViewGroup, viewType: Int): View =
        LayoutInflater.from(activity).inflate(layoutRes, parent, false)

    override fun onBindViewHolder(holder: OrderedItemViewHolder<I>, position: Int) {
        val item: I = dataset[position]
        holder.bind(item, position, dataset, controller, useImageText)
    }

    override fun getItemCount(): Int = dataset.size

    override fun getSectionName(position: Int): String =
        if (showSectionName) getSectionNameImp(position) else ""

    // for inheriting
    open fun getSectionNameImp(position: Int): String =
        dataset[position].defaultSortOrderReference()?.substring(0..1) ?: ""

    open class OrderedItemViewHolder<I : Displayable>(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        open fun bind(
            item: I,
            position: Int,
            dataset: List<I>,
            controller: MultiSelectionController<I>,
            useImageText: Boolean,
        ) {
            shortSeparator?.visibility = View.VISIBLE
            itemView.isActivated = controller.isSelected(item)
            title?.text = item.getDisplayTitle(context = itemView.context)
            text?.text = getDescription(item)
            textSecondary?.text = item.getSecondaryText(itemView.context)
            textTertiary?.text = item.getTertiaryText(itemView.context)
            if (useImageText) {
                setImageText(getRelativeOrdinalText(item, position))
            } else {
                setImage(position, dataset)
            }
            controller.registerClicking(itemView, position) {
                onClick(position, dataset, image)
            }
            menu?.let {
                prepareMenu(dataset[position], position, it)
            }
        }

        open fun onClick(position: Int, dataset: List<I>, imageView: ImageView?): Boolean = false

        protected open fun prepareMenu(item: I, position: Int, menuButtonView: View) {}

        protected open fun getRelativeOrdinalText(item: I, position: Int): String = "-"
        protected open fun getDescription(item: I): CharSequence? =
            item.getDescription(context = itemView.context)

        protected open fun setImage(
            position: Int,
            dataset: List<I>,
        ) {
            image?.also {
                it.visibility = View.VISIBLE
                it.setImageDrawable(defaultIcon)
            }
        }

        protected open fun setImageText(text: String) {
            imageText?.also {
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
                textSecondary?.setTextColor(context.secondaryTextColor(color))
                textTertiary?.setTextColor(context.secondaryTextColor(color))
            }
        }
    }
}
