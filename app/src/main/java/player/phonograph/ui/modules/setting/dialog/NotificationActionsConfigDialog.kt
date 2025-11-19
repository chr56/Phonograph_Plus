/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.databinding.ItemRightCheckboxBinding
import player.phonograph.model.notification.NotificationAction
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.ui.compose.components.ActionItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast

class NotificationActionsConfigDialog : AbsSettingsDialog() {
    private var adapter: ActionConfigAdapter? = null

    @Composable
    override fun Content() {
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_notification_actions),
            actions = listOf(
                ActionItem(
                    Icons.Default.Refresh,
                    textRes = R.string.action_reset,
                    onClick = { actionReset() }
                ),
                ActionItem(
                    Icons.Default.Check,
                    textRes = android.R.string.ok,
                    onClick = { actionApply() }
                ),
            ),
        ) {
            Column {
                Text(
                    stringResource(R.string.tips_notification_actions),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
                )
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        @SuppressLint("UseGetLayoutInflater", "InflateParams")
                        val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_wrapped, null)
                        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

                        val config: NotificationActionsConfig = Setting(context)[Keys.notificationActions].data
                        val configAdapter = ActionConfigAdapter(config).also { it.init() }

                        adapter = configAdapter

                        recyclerView.layoutManager = LinearLayoutManager(context)
                        recyclerView.adapter = adapter
                        configAdapter.attachToRecyclerView(recyclerView)

                        view
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    private fun actionApply() {
        val config = adapter?.currentConfig
        if (config != null) {
            Setting(requireContext())[Keys.notificationActions].data = config
            dismiss()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.err_illegal_operation,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun actionReset() {
        Setting(requireContext())[Keys.notificationActions].data = NotificationActionsConfig.DEFAULT
        dismiss()
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
                        R.string.tips_unmatched_notification_actions,
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