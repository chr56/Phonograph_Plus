/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun TempPopupContent(dismissPopup: () -> Unit, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(elevation = 2.dp) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                content()
            }
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                TextButton(dismissPopup) {
                    Text(
                        stringResource(android.R.string.cancel),
                        style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton({ dismissPopup(); onClick() }) {
                    Text(
                        stringResource(android.R.string.ok),
                        style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    )
                }
            }
        }
    }
}