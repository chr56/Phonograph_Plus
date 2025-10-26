/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileItem
import player.phonograph.ui.adapter.IMultiSelectableAdapter
import player.phonograph.ui.adapter.MultiSelectionController
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint

sealed class AbsFilesAdapter<VH : AbsFilesAdapter.ViewHolder>(
    val activity: ComponentActivity,
    dataset: Collection<FileItem>,
    allowMultiSelection: Boolean,
) : RecyclerView.Adapter<VH>(),
    SectionedAdapter,
    IMultiSelectableAdapter<FileItem> {

    var dataSet: MutableList<FileItem> = dataset.toMutableList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    //todo
    protected val controller: MultiSelectionController<FileItem> =
        MultiSelectionController(
            this,
            activity,
            allowMultiSelection
        )

    override fun getItem(datasetPosition: Int): FileItem = dataSet[datasetPosition]

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(dataSet[position], position, controller)
    }

    override fun getSectionName(position: Int): String = dataSet[position].name.take(2)

    sealed class ViewHolder(var binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(item: FileItem, position: Int, controller: MultiSelectionController<FileItem>)
    }

}
