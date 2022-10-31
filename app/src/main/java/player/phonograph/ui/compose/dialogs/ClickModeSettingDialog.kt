/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.dialogs

import player.phonograph.actions.baseModes
import player.phonograph.actions.modeName
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

@Composable
fun ClickModeSettingDialog(context: Context, onDismiss: () -> Unit) {
    PhonographTheme {
        BoxWithConstraints() {
            Column(Modifier
                       .widthIn(min = maxWidth * 3 / 5, max = maxWidth)
                       .verticalScroll(rememberScrollState())
                       .padding(8.dp)
            ) {

                Text(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                     text = "Mode Setting",
                     style = TextStyle(fontWeight = FontWeight.Bold,
                                       color = MaterialTheme.colors.onSurface,
                                       fontSize = 20.sp
                     )
                )

                Column {
                    Content(context)
                }

                Row(modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.End)
                    .padding(8.dp)) {
                    Text(text = "OK",
                         modifier = Modifier.clickable { onDismiss() },
                         color = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun Content(context: Context) {
    val currentMode = remember {
        mutableStateOf(Setting.instance.defaultSongItemClickBaseMode)
    }
    val setCurrentMode = { new: Int ->
        currentMode.value = new
        Setting.instance.defaultSongItemClickBaseMode = new
    }
    for (id in baseModes) {
        Row(Modifier
                .fillMaxWidth()
                .clickable { setCurrentMode(id) }
        ) {
            RadioButton(selected = currentMode.value == id, onClick = {})
            Text(text = modeName(context.resources, id),
                 Modifier
                     .padding(4.dp)
                     .fillMaxWidth()
                     .align(CenterVertically)
                     .alignByBaseline()
            )
        }
    }
}