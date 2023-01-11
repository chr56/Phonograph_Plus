/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.dialogs

import player.phonograph.R
import player.phonograph.model.config.ImageSourceConfig
import player.phonograph.ui.compose.components.SortableConfigAdapter
import player.phonograph.ui.compose.components.SortableConfigScreen
import player.phonograph.util.preferences.CoilImageSourceConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context

@Composable
fun ImageSourceConfigDialog(context: Context, onDismiss: () -> Unit) {
    val config = remember { CoilImageSourceConfig.currentConfig }
    val adapter = remember { imageSourceConfigAdapter(config, context) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(id = R.string.image_source_config),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.h6
        )
        SortableConfigScreen(adapter = adapter, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { reset(); onDismiss() },
                Modifier
                    .weight(2f)
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(stringResource(id = R.string.reset_action))
            }
            Spacer(modifier = Modifier.widthIn(48.dp))
            TextButton(
                onClick = onDismiss,
                Modifier
                    .weight(2f)
                    .wrapContentWidth(Alignment.End)
            ) {
                Text(stringResource(id = android.R.string.cancel))
            }
            TextButton(
                onClick = { saveImpl(adapter); onDismiss() },
                Modifier
                    .weight(2f)
                    .wrapContentWidth(Alignment.End)
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        }
    }
}

fun reset() {
    CoilImageSourceConfig.resetToDefault()
}

private fun saveImpl(adapter: SortableConfigAdapter) {
    val config = ImageSourceConfig.from(
        adapter.data.map { ImageSourceConfig.Item(it.key(), it.enabled()) }
    )
    CoilImageSourceConfig.currentConfig = config
}

private fun imageSourceConfigAdapter(
    config: ImageSourceConfig,
    context: Context
): SortableConfigAdapter {
    return SortableConfigAdapter(
        config.sources
            .map {
                SortableConfigAdapter.Item(
                    key = it.key,
                    text = it.imageSource.displayString(context),
                    enabled = it.enabled
                )
            }
            .toMutableList()
    )
}