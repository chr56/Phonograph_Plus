/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import player.phonograph.R
import player.phonograph.model.coil.ImageSource
import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.util.theme.tintButtons
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class ImageSourceConfigDialog : DialogFragment() {


    private lateinit var adapter: ImageSourceConfigAdapter
    private lateinit var recyclerView: RecyclerView

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)

        val config: ImageSourceConfig = Setting(requireContext())[Keys.imageSourceConfig].data
        adapter = ImageSourceConfigAdapter(config).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        val dialog = MaterialDialog(requireContext())
            .title(R.string.image_source_config)
            .noAutoDismiss()
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                val sourceConfig = adapter.currentConfig
                if (sourceConfig.sources.none { it.enabled }) {
                    Toast.makeText(
                        requireActivity(),
                        R.string.tips_choose_at_least_one,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Setting(requireContext())[Keys.imageSourceConfig].data = sourceConfig
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss(); }
            .neutralButton(R.string.action_reset) {
                Setting(requireContext())[Keys.imageSourceConfig].data = ImageSourceConfig.DEFAULT
                dismiss()
            }
            .tintButtons()

        return dialog
    }

    class ImageSourceConfigAdapter(private val sourceConfig: ImageSourceConfig) :
            SortableListAdapter<ImageSource>() {


        override fun fetchDataset(): SortableList<ImageSource> {
            return SortableList(
                sourceConfig.sources.map {
                    SortableList.Item(it.imageSource, it.enabled)
                }
            )
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
            contentView.text = dataset[holder.bindingAdapterPosition].content.displayString(contentView.context)
        }

        val currentConfig: ImageSourceConfig
            get() = ImageSourceConfig.from(
                dataset.items.map { ImageSourceConfig.Item(it.content.key, it.checked) }
            )

        companion object {
            private const val TAG = "ImageSourceConfigAdapter"
        }
    }
}