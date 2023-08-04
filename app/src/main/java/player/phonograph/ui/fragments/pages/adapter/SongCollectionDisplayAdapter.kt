/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages.adapter

import mt.util.color.primaryTextColor
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.model.SongCollection
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class SongCollectionDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<SongCollection>,
    layoutRes: Int,
    val onClick: (bindingAdapterPosition: Int) -> Unit,
) : DisplayAdapter<SongCollection>(activity, dataSet, layoutRes) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<SongCollection> =
        SongCollectionViewHolder(inflatedView(layoutRes, parent), onClick)

    class SongCollectionViewHolder(
        itemView: View,
        private val onClick: (bindingAdapterPosition: Int) -> Unit,
    ) : DisplayViewHolder<SongCollection>(itemView) {

        override fun onClick(position: Int, dataset: List<SongCollection>, imageView: ImageView?): Boolean {
            onClick(position)
            return true
        }

        override fun setImage(position: Int, dataset: List<SongCollection>, usePalette: Boolean) {
            super.setImage(position, dataset, usePalette)
            val context = itemView.context
            image?.setImageDrawable(
                context.getTintedDrawable(
                    R.drawable.ic_folder_white_24dp, resolveColor(
                        context,
                        R.attr.iconColor,
                        context.primaryTextColor(context.nightMode)
                    )
                )
            )
        }
    }
}