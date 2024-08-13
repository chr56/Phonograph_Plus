/*
 * Copyright (c) 2021-2023 alorma
 */

package lib.phonograph.preference.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun SettingsMenuLink(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable (Boolean) -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface {
        InabilityColor(enabled = enabled) {
            Row(
                modifier = modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            enabled = enabled,
                            onClick = onClick,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsTileIcon(icon = icon)
                    SettingsTileTexts(title = title, subtitle = subtitle)
                }
                if (action != null) {
                    Divider(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .height(56.dp)
                            .width(1.dp),
                    )
                    SettingsTileAction {
                        action.invoke(enabled)
                    }
                } else {
                    Spacer(
                        modifier = Modifier
                            .width(16.dp)
                            .width(1.dp)
                    )
                }
            }
        }
    }
}
