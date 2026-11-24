/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import coil.Coil
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import player.phonograph.R
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.IMAGERY_ICON
import player.phonograph.ui.adapter.IMAGERY_IMAGE
import player.phonograph.ui.adapter.IMAGERY_ORDINAL_TEXT
import player.phonograph.ui.adapter.ImageryType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
fun <T : Any> DisplayItem(
    item: T,
    presenter: DisplayPresenter<T>,
    layoutStyle: ItemLayoutStyle,
    imageryType: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongSelect: () -> Unit,
    onMenuClick: () -> Unit,
    menuContent: @Composable (() -> Unit),
) {
    val context = LocalContext.current
    val title = remember(item) { presenter.getDisplayTitle(context, item).toString() }
    when (layoutStyle) {
        ItemLayoutStyle.LIST                 -> ItemList(
            title = title,
            description = presenter.getDescription(context, item)?.toString(),
            selected = selected,
            leadingContent = { Artwork(item, presenter, imageryType) },
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.LIST_EXTENDED        -> ItemListExtended(
            title = title,
            description = presenter.getDescription(context, item)?.toString(),
            selected = selected,
            leadingContent = { Artwork(item, presenter, imageryType) },
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.LIST_SINGLE_ROW      -> ItemListSingleRow(
            title = title,
            selected = selected,
            leadingContent = { Artwork(item, presenter, imageryType) },
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.LIST_NO_IMAGE        -> ItemListNoImage(
            title = title,
            description = presenter.getDescription(context, item)?.toString(),
            selected = selected,
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.LIST_3L              -> ItemList3L(
            title = title,
            secondaryText = presenter.getSecondaryText(context, item).toString(),
            tertiaryText = presenter.getTertiaryText(context, item)?.toString(),
            selected = selected,
            leadingContent = { Artwork(item, presenter, imageryType) },
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.LIST_3L_EXTENDED     -> ItemList3LExtended(
            title = title,
            secondaryText = presenter.getSecondaryText(context, item).toString(),
            tertiaryText = presenter.getTertiaryText(context, item)?.toString(),
            selected = selected,
            leadingContent = { Artwork(item, presenter, imageryType) },
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.LIST_3L_NO_IMAGE     -> ItemList3LNoImage(
            title = title,
            secondaryText = presenter.getSecondaryText(context, item).toString(),
            tertiaryText = presenter.getTertiaryText(context, item)?.toString(),
            selected = selected,
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )

        ItemLayoutStyle.GRID                 -> ItemGrid(
            title = title,
            description = presenter.getDescription(context, item)?.toString(),
            selected = selected,
            artwork = { Artwork(item, presenter, imageryType, Modifier.matchParentSize()) },
            onClick = onClick,
            onLongClick = onLongSelect,
        )

        ItemLayoutStyle.GRID_CARD_HORIZONTAL -> ItemGridCardHorizontal(
            title = title,
            description = presenter.getDescription(context, item)?.toString(),
            selected = selected,
            artwork = { Artwork(item, presenter, imageryType, Modifier.matchParentSize()) },
            onClick = onClick,
            onLongClick = onLongSelect,
        )

        else                                 -> ItemList3L(
            title = title,
            secondaryText = presenter.getSecondaryText(context, item).toString(),
            tertiaryText = presenter.getTertiaryText(context, item)?.toString(),
            selected = selected,
            leadingContent = { Artwork(item, presenter, imageryType) },
            onClick = onClick,
            onLongClick = onLongSelect,
            onMenuClick = onMenuClick,
            menuContent = menuContent,
        )
    }
}

@Composable
private fun <T : Any> BoxScope.Artwork(
    item: T,
    presenter: DisplayPresenter<T>,
    @ImageryType imageryType: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .align(Alignment.Center)
            .then(modifier)
    ) {
        when (imageryType) {
            IMAGERY_IMAGE        -> ImageArtwork(item, presenter)
            IMAGERY_ICON         -> IconArtwork(item, presenter)
            IMAGERY_ORDINAL_TEXT -> OrdinalText(item, presenter)
            else                 -> {}
        }
    }
}

@Composable
private fun <T : Any> BoxScope.ImageArtwork(item: T, presenter: DisplayPresenter<T>) {
    val fallback = remember(item, presenter) {
        presenter.getDefaultArtwork(item).takeIf { it > 0 } ?: R.drawable.default_album_art
    }
    if (presenter.hasArtwork(item)) {
        val context = LocalContext.current
        val request = remember(context, item, fallback) {
            ImageRequest.Builder(context)
                .data(item).placeholder(fallback).error(fallback)
                .build()
        }
        val painter = rememberAsyncImagePainter(
            model = request,
            imageLoader = Coil.imageLoader(context)
        )
        ItemArtwork(painter)
    } else {
        Icon(
            painterResource(fallback), null,
            modifier = Modifier.matchParentSize(), tint = ItemDefaults.listColors().icon
        )
    }
}


@Composable
private fun <T : Any> BoxScope.IconArtwork(item: T, presenter: DisplayPresenter<T>) {
    val fallback = remember(item) { presenter.getDefaultArtwork(item) }
    Icon(
        painterResource(fallback), null,
        modifier = Modifier.matchParentSize(),
        tint = ItemDefaults.listColors().icon,
    )
}

@Composable
private fun <T : Any> BoxScope.OrdinalText(item: T, presenter: DisplayPresenter<T>) {
    val text = remember(item) { presenter.getOrdinalText(item, -1) ?: "-" }
    ItemOrdinal(text, ItemDefaults.listColors().icon)
}