/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.size.Dimension
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import player.phonograph.util.reportError
import androidx.core.graphics.BitmapCompat
import androidx.core.graphics.toRectF
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Matrix.ScaleToFit
import android.graphics.Rect
import android.net.Uri
import kotlin.math.min

/**
 * resize Bitmap (crop and scale to [size] if ratio is not proper)
 * @param size target size
 */
fun Bitmap.cropAndScaleTo(size: Size): Bitmap {
    if (size.isOriginal) return this

    val rawWidth = width
    val rawHeight = height

    val targetWidth = size.width.pxOrElse { rawWidth }
    val targetHeight = size.height.pxOrElse { rawHeight }

    if (shouldCrop(rawWidth, rawHeight)) {
        if (shouldScale(rawWidth, rawHeight, targetWidth, targetHeight)) {
            val minLength = min(rawHeight, rawWidth)
            val startX = (rawWidth - minLength) / 2
            val startY = (rawHeight - minLength) / 2
            val matrix = Matrix().apply {
                val selected = Rect(startX, startY, startX + minLength, startY + minLength).toRectF()
                val result = Rect(0, 0, targetWidth, targetHeight).toRectF()
                setRectToRect(selected, result, ScaleToFit.CENTER)
            }
            return try {
                Bitmap.createBitmap(targetWidth, targetHeight, config).also {
                    Canvas(it).drawBitmap(this, matrix, null)
                }
            } catch (e: Exception) {
                reportError(e, "BitmapResize", "Failed to resize $this using Matrix Scale: ")
                throw e
            }
        } else {
            val startX = (rawWidth - targetWidth) / 2
            val startY = (rawHeight - targetWidth) / 2
            return Bitmap.createBitmap(this, startX, startY, targetWidth, targetHeight)
        }
    } else {
        // just force scaled
        return BitmapCompat.createScaledBitmap(
            this,
            targetWidth,
            targetHeight,
            null,
            false
        )
    }
}


/**
 * determined by ratio
 */
private fun shouldCrop(rawWidth: Int, rawHeight: Int): Boolean =
    (rawWidth - rawHeight).toFloat() / (rawHeight + rawWidth) !in -0.08f..0.08f

/**
 * compare raw size with target size and determine whether to scale
 */
private fun shouldScale(rawWidth: Int, rawHeight: Int, targetWidth: Int, targetHeight: Int): Boolean =
    targetHeight > rawHeight || rawHeight.toFloat() / targetHeight > 2 // or target height too tiny
            || targetWidth > rawWidth || rawWidth.toFloat() / targetWidth > 2  // or target width too tiny

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
fun ByteArray.toBitmap(size: Size): Bitmap = toBitmap().cropAndScaleTo(size)


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
