package player.phonograph.preferences

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import player.phonograph.R
import player.phonograph.adapter.CategoryInfoAdapter
import player.phonograph.model.CategoryInfo
import player.phonograph.settings.Setting
import util.mdcolor.pref.ThemeColor

class LibraryPreferenceDialog : DialogFragment() {
    private var adapter: CategoryInfoAdapter? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.preference_dialog_library_categories, null)
        val categoryInfos: List<CategoryInfo>? = if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList(Setting.LIBRARY_CATEGORIES)
        } else {
            Setting.instance.libraryCategoryInfos
        }
        adapter = CategoryInfoAdapter(categoryInfos)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter!!.attachToRecyclerView(recyclerView)
        val dialog = MaterialDialog(requireContext())
            .title(R.string.library_categories)
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                updateCategories(adapter!!.categoryInfos)
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
            .neutralButton(R.string.reset_action) { adapter!!.categoryInfos = Setting.instance.defaultLibraryCategoryInfos }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(Setting.LIBRARY_CATEGORIES, ArrayList(adapter!!.categoryInfos))
    }

    private fun updateCategories(categories: List<CategoryInfo>) {
        if (getSelected(categories) == 0) return
        Setting.instance.libraryCategoryInfos = categories
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
