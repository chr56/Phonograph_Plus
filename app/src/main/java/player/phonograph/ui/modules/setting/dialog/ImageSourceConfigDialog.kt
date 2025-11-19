/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.model.coil.ImageSource
import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.SortableListAdapter
import player.phonograph.ui.compose.components.ActionItem
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

class ImageSourceConfigDialog : AbsSettingsDialog() {
    private var adapter: ImageSourceConfigAdapter? = null

    @Composable
    override fun Content() {
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.image_source_config),
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

                    val config: ImageSourceConfig = Setting(requireContext())[Keys.imageSourceConfig].data
                    val configAdapter = ImageSourceConfigAdapter(config).also { it.init() }

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
        val sourceConfig = adapter?.currentConfig
        if (sourceConfig != null) {
            if (sourceConfig.sources.none { it.enabled }) {
                Toast.makeText(
                    requireActivity(),
                    R.string.tips_choose_at_least_one,
                    Toast.LENGTH_SHORT
                ).show()
            }
            Setting(requireContext())[Keys.imageSourceConfig].data = sourceConfig
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
        Setting(requireContext())[Keys.imageSourceConfig].data = ImageSourceConfig.DEFAULT
        dismiss()
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