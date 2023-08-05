/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.model.pages.PageConfig
import player.phonograph.model.pages.Pages
import player.phonograph.ui.adapter.SortableListAdapter
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

class HomeTabConfigDialog : DialogFragment() {
    private lateinit var adapter: PageTabConfigAdapter
    private lateinit var recyclerView: RecyclerView

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)

        val config: PageConfig = HomeTabConfig.homeTabConfig

        adapter = PageTabConfigAdapter(config).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        Log.v(TAG, adapter.getState())

        val dialog = MaterialDialog(requireContext())
            .title(R.string.library_categories)
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                HomeTabConfig.homeTabConfig = adapter.currentConfig
                Log.v(TAG, adapter.getState())
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss(); Log.i(TAG, adapter.getState()) }
            .neutralButton(R.string.reset_action) {
                HomeTabConfig.homeTabConfig = PageConfig.DEFAULT_CONFIG
                Log.v(TAG, adapter.getState())
                dismiss()
            }
            .apply {
                // set button color
                val color = ThemeColor.accentColor(requireActivity())
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
                getActionButton(WhichButton.NEUTRAL).updateTextColor(color)
            }

        return dialog
    }

    companion object {
        private const val TAG = "HomeTabConfigDialog"
        fun newInstance(): HomeTabConfigDialog = HomeTabConfigDialog()
    }

    private class PageTabConfigAdapter(private val pageConfig: PageConfig) : SortableListAdapter<String>() {


        override fun fetchDataset(): SortableList<String> {
            val all = PageConfig.DEFAULT_CONFIG.toMutableSet()
            all.removeAll(pageConfig.tabList.toSet()).report("Strange PageConfig: $pageConfig")
            val visible = pageConfig.map { SortableList.Item(it, true) }
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

        override fun onBindContentView(contentView: View, position: Int) {
            require(contentView is TextView) { "Receive ${contentView.javaClass.name}" }
            contentView.text = Pages.getDisplayName(dataset[position].content, contentView.context)
        }

        val currentConfig: PageConfig
            get() = PageConfig.from(
                dataset.visibleItems().map { it.content }
            )

        companion object {
            private const val TAG = "PageTabConfigAdapter"
            private fun Boolean.report(msg: String): Boolean {
                if (!this) warning(TAG, msg)
                return this
            }
        }
    }
}
