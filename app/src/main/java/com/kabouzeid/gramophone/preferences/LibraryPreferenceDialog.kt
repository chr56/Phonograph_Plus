package com.kabouzeid.gramophone.preferences

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.adapter.CategoryInfoAdapter
import com.kabouzeid.gramophone.model.CategoryInfo
import com.kabouzeid.gramophone.util.PreferenceUtil

class LibraryPreferenceDialog : DialogFragment() {
    private var adapter: CategoryInfoAdapter? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.preference_dialog_library_categories, null)
        val categoryInfos: List<CategoryInfo>? = if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList(PreferenceUtil.LIBRARY_CATEGORIES)
        } else {
            PreferenceUtil.getInstance(requireContext()).libraryCategoryInfos
        }
        adapter = CategoryInfoAdapter(categoryInfos)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter!!.attachToRecyclerView(recyclerView)
        return MaterialDialog(requireContext())
            .title(R.string.library_categories)
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                updateCategories(adapter!!.categoryInfos)
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
            .neutralButton(R.string.reset_action) { adapter!!.categoryInfos = PreferenceUtil.getInstance(requireContext()).defaultLibraryCategoryInfos }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(PreferenceUtil.LIBRARY_CATEGORIES, ArrayList(adapter!!.categoryInfos))
    }

    private fun updateCategories(categories: List<CategoryInfo>) {
        if (getSelected(categories) == 0) return
        PreferenceUtil.getInstance(requireContext()).libraryCategoryInfos = categories
    }

    private fun getSelected(categories: List<CategoryInfo>): Int {
        var selected = 0
        for (categoryInfo in categories) {
            if (categoryInfo.visible) selected++
        }
        return selected
    }

    companion object {
        @JvmStatic
        fun newInstance(): LibraryPreferenceDialog {
            return LibraryPreferenceDialog()
        }
    }
}
