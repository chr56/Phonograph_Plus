/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.foundation.concurrent.lifecycleScopeOrNewOne
import player.phonograph.mechanism.coil.palette.PaletteColorViewTarget
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.adapter.DisplayOptions.Companion.CUSTOM_TEXT
import player.phonograph.ui.adapter.DisplayOptions.Companion.ICON
import player.phonograph.ui.adapter.DisplayOptions.Companion.IMAGE
import player.phonograph.ui.adapter.DisplayOptions.Companion.NONE
import player.phonograph.ui.adapter.DisplayOptions.Companion.ORDINAL_TEXT
import player.phonograph.ui.resource.Layouts
import player.phonograph.ui.theme.secondaryTextColorOn
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.theme.themeFooterColor
import player.phonograph.ui.theme.themeIconColor
import player.phonograph.ui.util.preparePopupMenu
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


open class DisplayAdapter<I>(
    val activity: FragmentActivity,
    var presenter: DisplayPresenter<I>,
    var options: DisplayOptions,
    allowMultiSelection: Boolean = true,
    stableId: Boolean = true,
) : RecyclerView.Adapter<DisplayAdapter.DisplayViewHolder<I>>(),
    FastScrollRecyclerView.SectionedAdapter,
    IMultiSelectableAdapter<I> {


    var dataset: List<I> = emptyList()
        set(value) {
            field = value
            @SuppressLint("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(stableId)
    }

    protected val controller: MultiSelectionController<I> =
        MultiSelectionController(this, activity, allowMultiSelection)

    override fun getItemId(position: Int): Long = presenter.getItemID(dataset[position])
    override fun getItem(datasetPosition: Int): I = dataset[datasetPosition]

    override fun getItemCount(): Int = dataset.size

    override fun getItemViewType(position: Int): Int = options.layoutStyle.ordinal
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<I> {
        val itemLayout = Layouts.itemLayoutStyle(ItemLayoutStyle.from(viewType))
        val view = LayoutInflater.from(activity).inflate(itemLayout, parent, false)
        return DisplayViewHolder(view)
    }


    override fun onBindViewHolder(holder: DisplayViewHolder<I>, position: Int) {
        holder.bind(dataset[position], position, dataset, presenter, controller, options)
    }

    override fun getSectionName(position: Int): String =
        if (options.showSectionName) {
            presenter.getSectionText(
                dataset[position], options.sortMode,
            ) ?: "-"
        } else {
            ""
        }

    fun setMultiSelectionEnabled(value: Boolean) {
        controller.enable = value
    }

    open class DisplayViewHolder<I>(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        open fun bind(
            item: I,
            position: Int,
            dataset: List<I>,
            presenter: DisplayPresenter<I>,
            controller: MultiSelectionController<I>,
            options: DisplayOptions,
        ) {
            val context = itemView.context
            // Text
            title?.text = presenter.getDisplayTitle(context, item)
            text?.text = presenter.getDescription(context, item)
            textSecondary?.text = presenter.getSecondaryText(context, item)
            textTertiary?.text = presenter.getTertiaryText(context, item)

            // Decorations
            shortSeparator?.visibility = View.VISIBLE
            itemView.isActivated = isSelected(item, controller)

            // Click
            controller.registerClicking(itemView, position) { context ->
                val entry = presenter.clickAction(context, item, position, dataset)
                if (entry != null) {
                    context.lifecycleScopeOrNewOne().launch(Dispatchers.Main.immediate) {
                        entry.action()
                    }
                }
            }

            // Menu
            val menuButtonView = menu
            if (menuButtonView != null) {
                if (options.showMenu) {
                    menuButtonView.visibility = View.VISIBLE
                    menuButtonView.setOnClickListener {
                        val context = menuButtonView.context
                        preparePopupMenu(context, menuButtonView, presenter.menuEntries(context, item, position))
                    }
                } else {
                    menuButtonView.visibility = View.GONE
                    menuButtonView.setOnClickListener(null)
                }
            }

            // Image
            setupImage(
                item, image, imageText,
                presenter, options,
            )
        }

        protected fun setupImage(
            item: I,
            imageView: ImageView?,
            imageText: TextView?,
            presenter: DisplayPresenter<I>,
            options: DisplayOptions,
        ) {
            imageView?.setImageDrawable(null)
            imageView?.visibility = View.GONE
            imageText?.visibility = View.GONE

            when (options.imageryType) {
                IMAGE        -> {
                    if (imageView != null) {
                        loadJob?.dispose()
                        loadJob = null
                        loadJob = loadImage(imageView, item, presenter, options.usePalette)
                    }
                }

                ICON         -> {
                    val icon = presenter.getIcon(itemView.context, item)
                    if (imageView != null && icon != null) {
                        setImageIcon(imageView, icon)
                    }
                }

                ORDINAL_TEXT -> {
                    val ordinalText = presenter.getOrdinalText(item, bindingAdapterPosition) ?: "-"
                    if (imageText != null) {
                        setImageText(imageText, ordinalText)
                    }
                }

                CUSTOM_TEXT  -> {
                    val customText = customText() ?: "-"
                    if (imageText != null) {
                        setImageText(imageText, customText)
                    }
                }

                NONE         -> {}
            }
        }

        protected fun loadImage(
            imageView: ImageView,
            item: I,
            presenter: DisplayPresenter<I>,
            usePalette: Boolean,
        ): Disposable? {
            val context = imageView.context
            imageView.visibility = View.VISIBLE
            imageView.clearColorFilter()
            val target = PaletteColorViewTarget(
                imageView,
                ::setPaletteColors,
                themeFooterColor(context),
                usePalette,
            )
            val icon = presenter.getIcon(context, item)
            target.onStart(
                icon ?: AppCompatResources.getDrawable(context, R.drawable.default_album_art)
            )
            return presenter.loadImageInto(context, item, target)
        }

        protected open fun setPaletteColors(color: Int) {
            paletteColorContainer?.let { paletteColorContainer ->
                val context = itemView.context
                val textColor = textColorOn(context, color)
                val secondaryTextColor = secondaryTextColorOn(context, color)
                paletteColorContainer.setBackgroundColor(color)
                title?.setTextColor(textColor)
                text?.setTextColor(secondaryTextColor)
                textSecondary?.setTextColor(secondaryTextColor)
                textTertiary?.setTextColor(secondaryTextColor)
            }
        }

        protected fun setImageIcon(imageView: ImageView, icon: Drawable) {
            imageView.visibility = View.VISIBLE
            imageView.setColorFilter(themeIconColor(itemView.context), PorterDuff.Mode.SRC_IN)
            imageView.setImageDrawable(icon)
        }

        protected fun setImageText(imageText: TextView, text: String) {
            imageText.visibility = View.VISIBLE
            imageText.text = text
        }

        protected open fun customText(): String? = null

        open fun isSelected(item: I, controller: MultiSelectionController<I>): Boolean = controller.isSelected(item)

        private var loadJob: Disposable? = null
    }
}