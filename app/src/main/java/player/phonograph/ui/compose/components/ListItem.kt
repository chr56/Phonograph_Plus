/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    painter: Painter? = null,
) {
    ItemRowLayout(modifier.wrapContentHeight(Alignment.Top)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .maxPercentage(0.15f)
        ) {
            if (painter != null)
                Image(
                    painter,
                    null,
                    modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                        .fillMaxSize()
                )
        }
        Column(
            Modifier
                .maxPercentage(0.7f)
                .clickable(onClick = onClick)
                .padding(16.dp, 8.dp)
                .wrapContentHeight()
                .fillMaxWidth()
                .layoutId(MAIN_CONTENT)
        ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.body1)
            Text(subtitle, style = MaterialTheme.typography.body1)
        }
        Icon(
            Icons.Outlined.MoreVert, stringResource(id = R.string.more_actions),
            Modifier
                .maxPercentage(0.15f)
                .fillMaxSize(0.7f)
                .clickable(onClick = onMenuClick)
                .padding(8.dp)
        )
    }
}