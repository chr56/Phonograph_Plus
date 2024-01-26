/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.actions.menu.ActionMenuProviders
import player.phonograph.coil.AbsPreloadImageCache
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Displayable
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

abstract class DisplayAdapter<I : Displayable>(
    protected val activity: FragmentActivity,
    var config: DisplayConfig,
) : RecyclerView.Adapter<DisplayAdapter.DisplayViewHolder<I>>(),
    FastScrollRecyclerView.SectionedAdapter,
    IMultiSelectableAdapter<I> {

    var dataset: List<I> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            preloadImages(value)
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


    override fun getItemViewType(position: Int): Int = config.layoutStyle.ordinal

    protected open fun inflatedView(parent: ViewGroup, viewType: Int): View =
        LayoutInflater.from(activity).inflate(ItemLayoutStyle.from(viewType).layout(), parent, false)

    override fun onBindViewHolder(holder: DisplayViewHolder<I>, position: Int) {
        val item: I = dataset[position]
        holder.bind(item, position, dataset, controller, config.useImageText, config.usePalette)
    }

    override fun getItemCount(): Int = dataset.size

    override fun getSectionName(position: Int): String =
        if (config.showSectionName) getSectionNameImp(position) else ""

    override fun onViewAttachedToWindow(holder: DisplayViewHolder<I>) {
        setImage(holder)
    }

    // override fun onViewDetachedFromWindow(holder: DisplayViewHolder<I>) {}

    protected var imageCache: DisplayPreloadImageCache<I> = DisplayPreloadImageCache(80)
        private set

    private fun preloadImages(items: Collection<I>) {
        imageCache = DisplayPreloadImageCache(items.size.coerceAtLeast(1))
        imageCache.imageLoaderScope.launch {
            for (item: I in items) {
                imageCache.preload(activity, item)
            }
        }
    }

    protected open fun setImage(holder: DisplayViewHolder<I>) {
        imageCache.imageLoaderScope.launch {
            val loaded = imageCache.fetch(activity, dataset[holder.bindingAdapterPosition])
            val usePalette = config.usePalette
            withContext(Dispatchers.Main) {
                holder.setImage(loaded.bitmap)
                if (usePalette) holder.setPaletteColors(loaded.paletteColor)
            }
        }
    }

    // for inheriting
    open fun getSectionNameImp(position: Int): String =
        dataset[position].defaultSortOrderReference()?.substring(0..1) ?: ""

    open class DisplayViewHolder<I : Displayable>(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        open fun bind(
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
            textSecondary?.text = item.getSecondaryText(itemView.context)
            textTertiary?.text = item.getTertiaryText(itemView.context)
            if (useImageText) {
                imageText?.visibility = View.VISIBLE
                setImageText(getRelativeOrdinalText(item))
            } else {
                image?.visibility = View.VISIBLE
                image?.setImageDrawable(defaultIcon)
            }
            controller.registerClicking(itemView, position) {
                onClick(position, dataset, image)
            }
            menu?.let {
                prepareMenu(item, it)
            }
        }

        @Suppress("UNCHECKED_CAST")
        open val clickActionProvider: ClickActionProviders.ClickActionProvider<I> =
            ClickActionProviders.EmptyClickActionProvider as ClickActionProviders.ClickActionProvider<I>

        protected open fun onClick(position: Int, dataset: List<I>, imageView: ImageView?): Boolean {
            return clickActionProvider.listClick(dataset, position, itemView.context, imageView)
        }

        open val menuProvider: ActionMenuProviders.ActionMenuProvider<I>? = null

        private fun prepareMenu(item: I, menuButtonView: View) {
            val provider = menuProvider
            if (provider != null) {
                menuButtonView.visibility = View.VISIBLE
                menuButtonView.setOnClickListener {
                    provider.prepareMenu(menuButtonView, item)
                }
            } else {
                menuButtonView.visibility = View.GONE
            }
        }


        protected open fun getRelativeOrdinalText(item: I): String = "-"
        protected open fun getDescription(item: I): CharSequence? =
            item.getDescription(context = itemView.context)

        open fun setImage(bitmap: Bitmap) {
            image?.setImageBitmap(bitmap)
        }

        protected open fun setImageText(text: String) {
            imageText?.text = text
        }

        protected open val defaultIcon =
            AppCompatResources.getDrawable(itemView.context, R.drawable.default_album_art)

        open fun setPaletteColors(color: Int) {
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

    class DisplayPreloadImageCache<I : Displayable>(size: Int) :
            AbsPreloadImageCache<I, PaletteBitmap>(size, IMPL_LRU) {

        @OptIn(ExperimentalCoroutinesApi::class)
        override suspend fun load(context: Context, key: I): PaletteBitmap =
            suspendCancellableCoroutine { continuation ->
                loadImage(context)
                    .from(key)
                    .into(
                        PaletteTargetBuilder(context)
                            .onResourceReady { result, palette ->
                                if (result is BitmapDrawable) {
                                    continuation.resume(PaletteBitmap(result.bitmap, palette)) { continuation.cancel() }
                                } else {
                                    continuation.cancel()
                                }
                            }
                            .build()
                    )
                    .enqueue()
            }

        override fun id(key: I): Long = key.getItemID()

        val imageLoaderScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

}
