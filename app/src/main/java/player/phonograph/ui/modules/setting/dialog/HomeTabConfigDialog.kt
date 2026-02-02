/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.model.pages.Pages
import player.phonograph.model.pages.PagesConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.util.theme.ThemeSettingsDelegate.textColorPrimary
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class HomeTabConfigDialog : AbsSettingsDialog() {
    private var adapter: PageTabConfigAdapter? = null

    @Composable
    override fun Content() {
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.label_library_categories),
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
            )
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    @SuppressLint("UseGetLayoutInflater", "InflateParams")
                    val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_wrapped, null)
                    val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

                    val config: PagesConfig = Setting(requireContext())[Keys.homeTabConfig].data
                    val configAdapter = PageTabConfigAdapter(config).also { it.init() }

                    adapter = configAdapter

                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = adapter
                    configAdapter.attachToRecyclerView(recyclerView)

                    view
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    private fun actionApply() {
        val pageConfig = adapter?.currentConfig
        if (pageConfig != null) {
            Setting(requireContext())[Keys.homeTabConfig].data = pageConfig
            dismiss()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.tips_select_at_least_one_category,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun actionReset() {
        Setting(requireContext())[Keys.homeTabConfig].data = PagesConfig.DEFAULT_CONFIG
        dismiss()
    }

    private class PageTabConfigAdapter(private val pagesConfig: PagesConfig) : SortableListAdapter<String>() {


        override fun fetchDataset(): SortableList<String> {
            val all = PagesConfig.DEFAULT_CONFIG.toMutableSet().apply {
                removeAll(pagesConfig.tabs.toSet())
            }
            val visible = pagesConfig.map { SortableList.Item(it, true) }
            val invisible = all.toList().map { SortableList.Item(it, false) }
            return SortableList(visible + invisible)
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
            contentView.text = Pages.getDisplayName(dataset[holder.bindingAdapterPosition].content, contentView.context)
        }

        val currentConfig: PagesConfig?
            get() = PagesConfig(dataset.checkedItems.map { it.content }.ifEmpty { return null })

        companion object {
            private const val TAG = "PageTabConfigAdapter"
        }
    }

    companion object {
        private const val TAG = "HomeTabConfigDialog"
        fun newInstance(): HomeTabConfigDialog = HomeTabConfigDialog()
    }

}
