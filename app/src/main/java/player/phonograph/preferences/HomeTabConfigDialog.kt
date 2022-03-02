/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.preferences

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import util.mdcolor.pref.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import player.phonograph.R
import player.phonograph.adapter.HomeTabConfigAdapter
import player.phonograph.adapter.PageConfig
import player.phonograph.settings.PreferenceUtil

class HomeTabConfigDialog : DialogFragment() {
    private lateinit var adapter: HomeTabConfigAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.preference_dialog_library_categories, null)

//        PreferenceUtil.getInstance(requireContext()).homeTabConfig = PageConfig.DEFAULT_CONFIG

        val config: PageConfig = PreferenceUtil.getInstance(requireContext()).homeTabConfig

        adapter = HomeTabConfigAdapter(config)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        Log.v(TAG, adapter.getState())

        val dialog = MaterialDialog(requireContext())
            .title(R.string.library_categories)
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                PreferenceUtil.getInstance(requireContext()).homeTabConfig = adapter.currentConfig
                Log.v(TAG, adapter.getState())
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss(); Log.i(TAG, adapter.getState()) }
            .neutralButton(R.string.reset_action) {
                PreferenceUtil.getInstance(requireContext()).homeTabConfig = PageConfig.DEFAULT_CONFIG
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
        @JvmStatic
        fun newInstance(): HomeTabConfigDialog = HomeTabConfigDialog()
    }
}
