/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.AbsPreloadImageCache
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.mechanism.actions.ClickActionProviders
import player.phonograph.model.Displayable
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayConfig.Companion.IMAGE_TYPE_FIXED_ICON
import player.phonograph.ui.adapter.DisplayConfig.Companion.IMAGE_TYPE_IMAGE
import player.phonograph.ui.adapter.DisplayConfig.Companion.IMAGE_TYPE_TEXT
import player.phonograph.ui.adapter.DisplayConfig.Companion.ImageType
import player.phonograph.util.theme.themeFooterColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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
            if (config.layoutStyle.hasImage) imageCacheDelegate.startPreloadImages(activity, value)
            notifyDataSetChanged()
        }

    init {
        @Suppress("LeakingThis")
        setHasStableIds(true)
    }

    private val imageCacheDelegate: ImageCacheDelegate<I> = ImageCacheDelegate(config)

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
        holder.bind(item, position, dataset, controller, config.imageType, config.usePalette, imageCacheDelegate)
    }

    override fun getItemCount(): Int = dataset.size

    override fun getSectionName(position: Int): String =
        if (config.showSectionName) getSectionNameImp(position) else ""

    // for inheriting
    open fun getSectionNameImp(position: Int): String =
        dataset[position].defaultSortOrderReference()?.substring(0..1) ?: ""

    open class DisplayViewHolder<I : Displayable>(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        open fun bind(
            item: I,
            position: Int,
            dataset: List<I>,
            controller: MultiSelectionController<I>,
            @ImageType imageType: Int,
            usePalette: Boolean,
            imageCacheDelegate: ImageCacheDelegate<I>,
        ) {
            shortSeparator?.visibility = View.VISIBLE
            itemView.isActivated = controller.isSelected(item)
            title?.text = item.getDisplayTitle(context = itemView.context)
            text?.text = getDescription(item)
            textSecondary?.text = item.getSecondaryText(itemView.context)
            textTertiary?.text = item.getTertiaryText(itemView.context)

            prepareImage(imageType, item, usePalette, imageCacheDelegate)

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

        protected open fun prepareImage(
            @ImageType imageType: Int,
            item: I,
            usePalette: Boolean,
            imageCacheDelegate: ImageCacheDelegate<I>,
        ) {
            when (imageType) {
                IMAGE_TYPE_FIXED_ICON -> {
                    image?.visibility = View.VISIBLE
                    image?.setImageDrawable(getIcon(item))
                }

                IMAGE_TYPE_IMAGE      -> {
                    image?.visibility = View.VISIBLE
                    image?.let { imageView -> fetchImage(item, imageView, usePalette, imageCacheDelegate) }
                }

                IMAGE_TYPE_TEXT       -> {
                    imageText?.visibility = View.VISIBLE
                    setImageText(getRelativeOrdinalText(item))
                }
            }
        }

        protected open fun fetchImage(
            item: I,
            imageView: ImageView,
            usePalette: Boolean,
            imageCacheDelegate: ImageCacheDelegate<I>,
        ) {
            val context = imageView.context
            val cached = imageCacheDelegate.peek(item)
            if (cached != null) {
                imageView.setImageBitmap(cached.bitmap)
                if (usePalette) setPaletteColors(cached.paletteColor)
            } else {
                coroutineScope.launch {
                    loadJob?.dispose()
                    loadJob = loadImage(context)
                        .from(item)
                        .into(
                            PaletteTargetBuilder()
                                .defaultColor(themeFooterColor(context))
                                .view(imageView)
                                .onStart {
                                    imageView.setImageDrawable(defaultIcon)
                                    setPaletteColors(themeFooterColor(itemView.context))
                                }
                                .onResourceReady { result, paletteColor ->
                                    imageView.setImageDrawable(result)
                                    if (usePalette) setPaletteColors(paletteColor)
                                }
                                .build()
                        )
                        .enqueue()
                }
            }
        }


        protected open fun getIcon(item: I): Drawable? = defaultIcon

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

        private var loadJob: Disposable? = null

        companion object {
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }

    class ImageCacheDelegate<I : Displayable>(val config: DisplayConfig) {

        private var _imageCache: DisplayPreloadImageCache<I>? = null
        private var imageCache: DisplayPreloadImageCache<I>
            get() {
                if (_imageCache == null) {
                    _imageCache = DisplayPreloadImageCache(1)
                }
                return _imageCache!!
            }
            set(value) {
                _imageCache = value
            }

        fun startPreloadImages(context: Context, items: Collection<I>) {
            if (!enabledPreload || config.imageType != IMAGE_TYPE_IMAGE) return

            imageCache = DisplayPreloadImageCache(items.size.coerceAtLeast(1))
            imageCache.imageLoaderScope.launch {
                for (item: I in items) {
                    imageCache.preload(context, item)
                }
            }
        }

        fun peek(item: I): PaletteBitmap? = imageCache.peek(item)

        private val enabledPreload: Boolean = Setting(App.instance)[Keys.preloadImages].data
    }

    class DisplayPreloadImageCache<I : Displayable>(size: Int) :
            AbsPreloadImageCache<I, PaletteBitmap>(size, if (SDK_INT >= Q) IMPL_SPARSE_ARRAY else IMPL_SCATTER_MAP) {

        override suspend fun load(context: Context, key: I): PaletteBitmap =
            suspendCancellableCoroutine { continuation ->
                loadImage(context)
                    .from(key)
                    .into(
                        PaletteTargetBuilder()
                            .defaultColor(themeFooterColor(context))
                            .onResourceReady { result, palette ->
                                if (result is BitmapDrawable) {
                                    continuation.resume(
                                        PaletteBitmap(result.bitmap, palette)
                                    ) { cause, _, _ ->
                                        continuation.cancel(cause)
                                    }
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
