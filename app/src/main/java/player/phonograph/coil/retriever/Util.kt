/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.size.Dimension
import coil.size.Size
import coil.size.pxOrElse
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

fun Bitmap.resize(size: Size): Bitmap {
    if (size.width is Dimension.Undefined || size.height is Dimension.Undefined) return this
    return Bitmap.createScaledBitmap(
        this,
        size.width.pxOrElse { width },
        size.height.pxOrElse { height },
        false
    )
}

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
fun ByteArray.toBitmap(size: Size): Bitmap = toBitmap().resize(size)


internal val folderCoverFiles = arrayOf(
    "cover.jpg",
    "album.jpg",
    "folder.jpg",
    "cover.png",
    "album.png",
    "folder.png"
)

internal fun getMediaStoreAlbumCoverUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
