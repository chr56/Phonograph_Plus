/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.dialogs

import player.phonograph.model.config.ImageSourceConfig
import player.phonograph.ui.compose.components.SortableConfigAdapter
import player.phonograph.ui.compose.components.SortableConfigScreen
import player.phonograph.util.preferences.CoilImageSourceConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context

@Composable
fun ImageSourceConfigDialog(context: Context) {
    val config = remember { CoilImageSourceConfig.currentConfig }
    val adapter = remember { imageSourceConfigAdapter(config) }
    Box(modifier = Modifier.padding(24.dp)) {
        SortableConfigScreen(adapter = adapter)
    }
}

private fun imageSourceConfigAdapter(config: ImageSourceConfig): SortableConfigAdapter {
    return SortableConfigAdapter(
        config.sources
            .map { SortableConfigAdapter.Item(it.name, it.enabled, it.name.hashCode()) }
            .toMutableList()
    )
}