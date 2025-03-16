/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import player.phonograph.R
import player.phonograph.model.pages.Pages
import player.phonograph.model.pages.PagesConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.warning
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class HomeTabConfigDialog : DialogFragment() {
    private lateinit var adapter: PageTabConfigAdapter
    private lateinit var recyclerView: RecyclerView

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)


        val config: PagesConfig = Setting(requireContext()).Composites[Keys.homeTabConfig].data

        adapter = PageTabConfigAdapter(config).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        Log.v(TAG, adapter.getState())

        val dialog = MaterialDialog(requireContext())
            .title(R.string.library_categories)
            .customView(view = view, dialogWrapContent = false)
            .noAutoDismiss()
            .positiveButton(android.R.string.ok) {
                Log.v(TAG, adapter.getState())
                val pageConfig = adapter.currentConfig
                if (pageConfig != null) {
                    Setting(requireContext()).Composites[Keys.homeTabConfig].data = pageConfig
                    dismiss()
                } else {
                    Toast.makeText(
                        it.context,
                        R.string.you_have_to_select_at_least_one_category,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .negativeButton(android.R.string.cancel) { dismiss(); Log.i(TAG, adapter.getState()) }
            .neutralButton(R.string.reset_action) {
                Setting(requireContext()).Composites[Keys.homeTabConfig].data = PagesConfig.DEFAULT_CONFIG
                Log.v(TAG, adapter.getState())
                dismiss()
            }
            .tintButtons()

        return dialog
    }

    companion object {
        private const val TAG = "HomeTabConfigDialog"
        fun newInstance(): HomeTabConfigDialog = HomeTabConfigDialog()
    }

    private class PageTabConfigAdapter(private val pagesConfig: PagesConfig) : SortableListAdapter<String>() {


        override fun fetchDataset(): SortableList<String> {
            val all = PagesConfig.DEFAULT_CONFIG.toMutableSet()
            all.removeAll(pagesConfig.tabs.toSet()).report("Strange PageConfig: $pagesConfig")
            val visible = pagesConfig.map { SortableList.Item(it, true) }
            val invisible = all.toList().map { SortableList.Item(it, false) }
            return SortableList(visible + invisible)
        }

        override fun onCreateContentView(parent: ViewGroup, viewType: Int): View {
            return TextView(parent.context).apply {
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
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
            private fun Boolean.report(msg: String): Boolean {
                if (!this) warning(TAG, msg)
                return this
            }
        }
    }
}
