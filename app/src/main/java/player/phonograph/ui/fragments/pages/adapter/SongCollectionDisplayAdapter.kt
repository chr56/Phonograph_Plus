/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import player.phonograph.R
import player.phonograph.mechanism.actions.ClickActionProviders
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.SongCollection
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayConfig
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.themeIconColor
import androidx.appcompat.app.AppCompatActivity
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class SongCollectionDisplayAdapter(
    activity: AppCompatActivity,
    val onClick: (bindingAdapterPosition: Int) -> Unit,
) : DisplayAdapter<SongCollection>(
    activity,
    ConstDisplayConfig(ItemLayoutStyle.LIST, imageType = DisplayConfig.IMAGE_TYPE_FIXED_ICON)
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<SongCollection> =
        SongCollectionViewHolder(inflatedView(parent, viewType), onClick)

    class SongCollectionViewHolder(
        itemView: View,
        private val onClick: (bindingAdapterPosition: Int) -> Unit,
    ) : DisplayViewHolder<SongCollection>(itemView) {

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<SongCollection>
            get() = super.clickActionProvider

        override fun onClick(position: Int, dataset: List<SongCollection>, imageView: ImageView?): Boolean {
            onClick(position)
            return true
        }

        override fun getIcon(item: SongCollection): Drawable? {
            val context = itemView.context
            return context.getTintedDrawable(
                R.drawable.ic_folder_white_24dp, themeIconColor(context)
            )
        }

    }
}