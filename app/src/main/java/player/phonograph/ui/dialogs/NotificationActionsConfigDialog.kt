/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.databinding.ItemRightCheckboxBinding
import player.phonograph.mechanism.setting.NotificationAction
import player.phonograph.mechanism.setting.NotificationActionsConfig
import player.phonograph.mechanism.setting.NotificationConfig
import player.phonograph.ui.adapter.SortableListAdapter
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox

class NotificationActionsConfigDialog : DialogFragment() {
    private lateinit var adapter: ActionConfigAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)

        val config: NotificationActionsConfig = NotificationConfig.actions

        adapter = ActionConfigAdapter(config).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        @Suppress("DEPRECATION")
        val dialog = MaterialDialog(requireContext())
            .title(text = "NotificationConfig")
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                NotificationConfig.actions = adapter.currentConfig
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
            .neutralButton(R.string.reset_action) {
                NotificationConfig.actions = NotificationActionsConfig.DEFAULT
                dismiss()
            }
            .apply {
                val color = ThemeColor.accentColor(requireContext())
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
                getActionButton(WhichButton.NEUTRAL).updateTextColor(color)
            }

        return dialog
    }

    private class ActionConfigAdapter(private val actionConfig: NotificationActionsConfig) :
            SortableListAdapter<NotificationActionsConfig.Item>() {

        override fun fetchDataset(): SortableList<NotificationActionsConfig.Item> {
            val result: MutableList<SortableList.Item<NotificationActionsConfig.Item>> =
                actionConfig.actions.map { SortableList.Item(it, true) }.toMutableList()
            for (action in NotificationAction.ALL_ACTIONS) {
                if (result.firstOrNull { it.content.key == action.key } == null) {
                    result.add(
                        SortableList.Item(
                            NotificationActionsConfig.Item(
                                action.key, false
                            ), false
                        )
                    )
                }
            }
            return SortableList(result)
        }

        override fun onCreateContentView(parent: ViewGroup, viewType: Int): View {
            return LayoutInflater.from(parent.context).inflate(R.layout.item_right_checkbox, parent, false)
        }

        override fun onBindContentView(contentView: View, position: Int) {
            val binding = ItemRightCheckboxBinding.bind(contentView)
            val item = dataset[position].content
            val action = item.notificationAction
            binding.textview.text = if (action != null) contentView.resources.getText(action.stringRes) else item.key
            binding.checkbox.isChecked = item.displayInCompat
            binding.checkbox.setOnCheckedChangeListener { _, valve ->
                item.displayInCompat = valve
            }
        }

        override fun checkRequirement(): Boolean = dataset.checkedItems.size in 3..5

        val currentConfig: NotificationActionsConfig
            get() = NotificationActionsConfig(
                dataset.checkedItems.map { it.content }
            )

    }
}