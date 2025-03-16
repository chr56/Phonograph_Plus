/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import player.phonograph.R
import player.phonograph.databinding.ItemRightCheckboxBinding
import player.phonograph.model.notification.NotificationAction
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.util.theme.tintButtons
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

class NotificationActionsConfigDialog : DialogFragment() {
    private lateinit var adapter: ActionConfigAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)

        val config: NotificationActionsConfig = Setting(requireContext()).Composites[Keys.notificationActions].data

        adapter = ActionConfigAdapter(config).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        view.findViewById<TextView>(R.id.description).apply {
            setText(R.string.help_notification_actions)
            visibility = View.VISIBLE
        }

        @Suppress("DEPRECATION")
        val dialog = MaterialDialog(requireContext())
            .title(R.string.pref_title_notification_actions)
            .customView(view = view, dialogWrapContent = false)
            .noAutoDismiss()
            .positiveButton(android.R.string.ok) {
                val actionsConfig = adapter.currentConfig
                if (actionsConfig != null) {
                    Setting(requireContext()).Composites[Keys.notificationActions].data = actionsConfig
                    dismiss()
                } else {
                    Toast.makeText(
                        it.context,
                        R.string.illegal_operation,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
            .neutralButton(R.string.reset_action) {
                Setting(requireContext()).Composites[Keys.notificationActions].data = NotificationActionsConfig.DEFAULT
                dismiss()
            }
            .tintButtons()

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

        override fun onBindContentView(contentView: View, holder: ViewHolder) {
            val binding = ItemRightCheckboxBinding.bind(contentView)
            val item = dataset[holder.bindingAdapterPosition].content
            binding.textview.text = contentView.resources.getText(item.notificationAction.stringRes)
            binding.checkbox.isChecked = item.displayInCompat
            binding.checkbox.setOnClickListener { view ->
                val position = holder.bindingAdapterPosition
                if (dataset[position].checked) {
                    item.displayInCompat = !item.displayInCompat
                } else {
                    Toast.makeText(
                        view.context,
                        R.string.help_unmatched_notification_actions,
                        Toast.LENGTH_SHORT
                    ).show()
                    (view as CheckBox).toggle()
                }
            }
        }

        override val clickByCheckboxOnly: Boolean get() = true

        val currentConfig: NotificationActionsConfig?
            get() = NotificationActionsConfig(
                dataset.checkedItems.map { it.content }
            ).let(::validate)

        private fun validate(raw: NotificationActionsConfig): NotificationActionsConfig? {
            if (raw.actions.size !in 1..5) return null
            if (raw.actions.filter { it.displayInCompat }.size !in 1..3) return null
            return raw
        }

    }
}