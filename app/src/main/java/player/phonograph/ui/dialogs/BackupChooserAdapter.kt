/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.mechanism.backup.Backup
import player.phonograph.model.backup.BackupItem
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.util.theme.ThemeSettingsDelegate.textColorPrimary
import androidx.appcompat.widget.AppCompatTextView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class BackupChooserAdapter(
    private val config: List<BackupItem>,
    private val all: List<BackupItem>,
) : SortableListAdapter<BackupItem>() {

    override fun fetchDataset(): SortableList<BackupItem> {
        val disabled = all.toMutableList().also { it.removeAll(config) }
        return SortableList(
            config.map { SortableList.Item(it, true) } + disabled.map { SortableList.Item(it, false) }
        )
    }

    override fun onCreateContentView(parent: ViewGroup, viewType: Int): View {
        val context = parent.context
        return AppCompatTextView(context, null).apply {
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            setTextColor(textColorPrimary(context))
        }
    }

    override fun onBindContentView(contentView: View, holder: ViewHolder) {
        require(contentView is TextView) { "Receive ${contentView.javaClass.name}" }
        val item: BackupItem = dataset.items[holder.bindingAdapterPosition].content
        contentView.text = Backup.displayName(item, contentView.resources)
    }

    val currentConfig: List<BackupItem>
        get() = dataset.items.filter { it.checked }.map { it.content }

}