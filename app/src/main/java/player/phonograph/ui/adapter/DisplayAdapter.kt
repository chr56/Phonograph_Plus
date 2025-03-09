/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.coil.palette.PaletteColorViewTarget
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.themeIconColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


open class DisplayAdapter<I>(
    val activity: FragmentActivity,
    var presenter: DisplayPresenter<I>,
) : RecyclerView.Adapter<DisplayAdapter.DisplayViewHolder<I>>(),
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
            by lazy { MultiSelectionController(this, activity, presenter.allowMultiSelection) }

    override fun getItemId(position: Int): Long = presenter.getItemID(dataset[position])
    override fun getItem(datasetPosition: Int): I = dataset[datasetPosition]

    override fun getItemCount(): Int = dataset.size

    override fun getItemViewType(position: Int): Int = presenter.layoutStyle.ordinal
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<I> {
        val view = LayoutInflater.from(activity).inflate(ItemLayoutStyle.from(viewType).layout(), parent, false)
        return DisplayViewHolder(view)
    }


    override fun onBindViewHolder(holder: DisplayViewHolder<I>, position: Int) {
        holder.bind(dataset[position], position, dataset, presenter, controller)
    }

    override fun getSectionName(position: Int): String =
        if (presenter.showSectionName) getSectionNameImp(position) else ""

    open fun getSectionNameImp(position: Int): String {
        val item = dataset[position]
        val sortMode = presenter.getSortOrderKey(activity)
        val text = if (sortMode != null) {
            presenter.getSortOrderReference(item, sortMode)
        } else {
            presenter.getNonSortOrderReference(item)
        }
        return text ?: "-"
    }


    open class DisplayViewHolder<I>(itemView: View) : UniversalMediaEntryViewHolder(itemView) {
        open fun bind(
            item: I,
            position: Int,
            dataset: List<I>,
            presenter: DisplayPresenter<I>,
            controller: MultiSelectionController<I>,
        ) {
            shortSeparator?.visibility = View.VISIBLE
            itemView.isActivated = controller.isSelected(item)

            // Text
            title?.text = presenter.getDisplayTitle(itemView.context, item)
            text?.text = presenter.getDescription(itemView.context, item)
            textSecondary?.text = presenter.getSecondaryText(itemView.context, item)
            textTertiary?.text = presenter.getTertiaryText(itemView.context, item)

            // Click
            val clickActionProvider = presenter.clickActionProvider
            controller.registerClicking(itemView, position) {
                clickActionProvider.listClick(dataset, position, itemView.context, image)
            }

            // Menu
            val menuButtonView = menu
            val menuProvider = presenter.menuProvider
            if (menuButtonView != null) {
                if (menuProvider != null) {
                    menuButtonView.visibility = View.VISIBLE
                    menuButtonView.setOnClickListener {
                        menuProvider.prepareMenu(menuButtonView, item)
                    }
                } else {
                    menuButtonView.visibility = View.GONE
                }
            }

            // Image
            loadImage(item, presenter.imageType, image, presenter)
        }

        protected fun loadImage(
            item: I,
            imageType: Int,
            imageView: ImageView?,
            presenter: DisplayPresenter<I>,
        ) {
            when (imageType) {
                DisplayPresenter.IMAGE_TYPE_FIXED_ICON -> {
                    val icon = presenter.getIcon(itemView.context, item)
                    if (imageView != null) {
                        imageView.visibility = View.VISIBLE
                        imageView.setColorFilter(themeIconColor(itemView.context), PorterDuff.Mode.SRC_IN)
                        imageView.setImageDrawable(icon)
                    }
                }

                DisplayPresenter.IMAGE_TYPE_IMAGE      -> {
                    if (imageView != null) {
                        image?.visibility = View.VISIBLE
                        loadJob?.dispose()
                        loadJob = presenter.startLoadingImage(
                            itemView.context, item,
                            PaletteColorViewTarget(
                                imageView,
                                ::setPaletteColors,
                                themeFooterColor(itemView.context),
                                presenter.usePalette
                            )
                        )
                    }
                }

                DisplayPresenter.IMAGE_TYPE_TEXT       -> {
                    val ordinalText = presenter.getRelativeOrdinalText(item) ?: "-"
                    imageText?.visibility = View.VISIBLE
                    imageText?.text = ordinalText
                }
            }
        }

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

        private var loadJob: Disposable? = null
    }
}