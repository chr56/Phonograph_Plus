/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.adapter.sortable.ImageSourceConfigAdapter
import player.phonograph.model.config.ImageSourceConfig
import player.phonograph.mechanism.setting.CoilImageSourceConfig
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.os.Bundle

class ImageSourceConfigDialog : DialogFragment() {


    private lateinit var adapter: ImageSourceConfigAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)

        val config: ImageSourceConfig = CoilImageSourceConfig.currentConfig
        adapter = ImageSourceConfigAdapter(config).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        val dialog = MaterialDialog(requireContext())
            .title(R.string.image_source_config)
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                CoilImageSourceConfig.currentConfig = adapter.currentConfig
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss(); }
            .neutralButton(R.string.reset_action) {
                CoilImageSourceConfig.resetToDefault()
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

}