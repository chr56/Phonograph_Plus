/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter.file

import androidx.core.app.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileEntity

abstract class AbsFileAdapter<VH : AbsFileAdapter.ViewHolder>(
    activity: ComponentActivity,
    dataset: MutableList<FileEntity>,
    cabController: MultiSelectionCabController?
) : MultiSelectAdapter<VH, FileEntity>(activity, cabController), SectionedAdapter {

    var dataSet: MutableList<FileEntity> = dataset
        set(value) {
            field = value
            /* noinspection NotifyDataSetChanged**/
            notifyDataSetChanged()
        }

    override fun getItem(datasetPosition: Int): FileEntity = dataSet[datasetPosition]

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(dataSet[position], position)
    }

    override fun getSectionName(position: Int): String = dataSet[position].name.take(2)

    abstract class ViewHolder(var binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(item: FileEntity, position: Int)
    }

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)
}
