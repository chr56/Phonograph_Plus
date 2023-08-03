/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter.display

import mt.util.color.primaryTextColor
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.model.SongCollection
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageView

class SongCollectionDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<SongCollection>,
    layoutRes: Int,
    cfg: (DisplayAdapter<SongCollection>.() -> Unit)?
) : DisplayAdapter<SongCollection>(activity, dataSet, layoutRes, cfg) {

    var onClick: (bindingAdapterPosition: Int) -> Unit = {}

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        val context = holder.itemView.context
        holder.image?.setImageDrawable(
            context.getTintedDrawable(
                R.drawable.ic_folder_white_24dp, resolveColor(
                    context,
                    R.attr.iconColor,
                    context.primaryTextColor(context.nightMode)
                )
            )
        )
    }

    override fun onClickItem(bindingAdapterPosition: Int, view: View, imageView: ImageView?) {
        when (controller.isInQuickSelectMode) {
            true  -> controller.toggle(bindingAdapterPosition)
            false -> {
                onClick(bindingAdapterPosition)
            }
        }
    }
}