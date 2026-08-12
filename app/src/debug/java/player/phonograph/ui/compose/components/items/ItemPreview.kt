/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.R
import player.phonograph.ui.compose.PhonographTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.res.Configuration

@Preview(name = "Dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
annotation class ItemPreview

private const val PreviewTitle = "Never gonna to give you up"
private const val PreviewDescription = "Rick Astley | Whenever You Need Somebody"
private const val PreviewSecondary = "Rick Astley"
private const val PreviewTertiary = "Whenever You Need Somebody"
private const val PreviewLongTitle = "Never gonna to give you up by Rick Astley"

@Composable
private fun BoxScope.PreviewArtwork() {
    ItemArtwork(painterResource(R.drawable.default_album_art))
}



@ItemPreview
@Composable
fun ItemList2LPreviews() = PhonographTheme {
    Column {
        ItemList(
            title = PreviewTitle,
            description = PreviewDescription,
            leadingContent = { PreviewArtwork() },
            onMenuClick = {},
        )
        ItemListExtended(
            title = PreviewLongTitle,
            description = PreviewDescription,
            leadingContent = { PreviewArtwork() },
            onMenuClick = {},
        )
        ItemListNoImage(
            title = PreviewTitle,
            description = PreviewDescription,
            onMenuClick = {},
        )

        ItemList(
            title = PreviewTitle,
            description = PreviewDescription,
            leadingContent = { ItemOrdinal("X", colorResource(R.color.default_text_color_primary)) },
            onMenuClick = {},
            showDragHandle = true,
        )
        ItemListExtended(
            title = PreviewLongTitle,
            description = PreviewDescription,
            leadingContent = { ItemOrdinal("Y", colorResource(R.color.default_text_color_primary)) },
            onMenuClick = {},
            showDragHandle = true,
        )
        ItemListNoImage(
            title = PreviewTitle,
            description = PreviewDescription,
            onMenuClick = {},
            selected = true,
        )
    }
}

@ItemPreview
@Composable
fun ItemList3LPreviews() = PhonographTheme {
    Column {
        ItemList3L(
            title = PreviewTitle,
            secondaryText = PreviewSecondary,
            tertiaryText = PreviewTertiary,
            leadingContent = { PreviewArtwork() },
            onMenuClick = {},
        )
        ItemList3LExtended(
            title = PreviewLongTitle,
            secondaryText = PreviewSecondary,
            tertiaryText = PreviewTertiary,
            leadingContent = { PreviewArtwork() },
            onMenuClick = {},
        )
        ItemList3LNoImage(
            title = PreviewLongTitle,
            secondaryText = PreviewSecondary,
            tertiaryText = PreviewTertiary,
            onMenuClick = {},
        )
        ItemList3L(
            title = PreviewTitle,
            secondaryText = PreviewSecondary,
            tertiaryText = PreviewTertiary,
            leadingContent = { ItemOrdinal("X", colorResource(R.color.default_text_color_primary)) },
            onMenuClick = {},
            showDragHandle = true,
        )
        ItemList3LExtended(
            title = PreviewLongTitle,
            secondaryText = PreviewSecondary,
            tertiaryText = PreviewTertiary,
            leadingContent = { ItemOrdinal("Y", colorResource(R.color.default_text_color_primary)) },
            onMenuClick = {},
            showDragHandle = true,
        )
        ItemList3LNoImage(
            title = PreviewTitle,
            secondaryText = PreviewSecondary,
            tertiaryText = PreviewTertiary,
            onMenuClick = {},
            selected = true,
        )
    }
}

@ItemPreview
@Composable
fun ItemList1LPreviews() = PhonographTheme {
    Column {
        ItemListSingleRow(PreviewTitle, leadingContent = { PreviewArtwork() }, onMenuClick = {})
    }
}