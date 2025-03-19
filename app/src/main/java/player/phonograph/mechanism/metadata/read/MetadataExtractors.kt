/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.metadata.read

import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ExceptionCollector
import player.phonograph.util.image.decodeBitmapWithRestrictions
import player.phonograph.util.image.generatePalette
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MetadataExtractors {

    suspend fun extractMetadata(context: Context, song: Song, withImage: Boolean = true): SongDetail {
        val errors = mutableListOf<Throwable>()
        val metadata = extractMetadataImpl(context, song) { errors.add(it) }

        return if (withImage) {
            val (bitmap, paletteColor) = extractEmbeddedImage(song.data)
            SongDetail(song, metadata, errors, bitmap, paletteColor)
        } else {
            SongDetail(song, metadata, errors, null, null)
        }
    }

    fun extractMetadata(context: Context, songs: List<Song>): SongDetailCollection {
        val metadataMap = mutableMapOf<Song, AudioMetadata>()
        val errorMap = mutableMapOf<Song, List<Throwable>>()
        for (song in songs) {
            val errors = mutableListOf<Throwable>()
            val metadata = extractMetadataImpl(context, song) { errors.add(it) }
            metadataMap[song] = metadata
            if (errors.isNotEmpty()) errorMap[song] = errors
        }
        return SongDetailCollection.from(metadataMap, errorMap)
    }

    fun extractRawImage(path: String): ByteArray? {
        return JAudioTaggerExtractor.extractRawImage(path, null)
    }

    private fun extractMetadataImpl(context: Context, song: Song, collector: ExceptionCollector?): AudioMetadata {
        val metadata =
            JAudioTaggerExtractor.extractMetadata(song, collector)
                ?: DefaultMetadataExtractor.extractMetadata(song, null)
        return metadata
    }


    private const val MAX_PIXELS_IMAGE = 3_686_400
    private const val MAX_PIXELS_PALETTE_CALCULATION = 147_456

    /**
     * Read embedded image from metadata of [songFilePath], with its palette color.
     */
    suspend fun extractEmbeddedImage(songFilePath: String): Pair<Bitmap?, Color?> {
        val imageBytes = withContext(Dispatchers.IO) {
            JAudioTaggerExtractor.extractRawImage(songFilePath, null)
        } ?: return Pair(null, null)
        return try {
            withContext(Dispatchers.Default) {
                val bitmap = decodeBitmapWithRestrictions(imageBytes, MAX_PIXELS_IMAGE)
                if (bitmap != null) {
                    val downsampled = decodeBitmapWithRestrictions(imageBytes, MAX_PIXELS_PALETTE_CALCULATION)
                    if (downsampled != null) {
                        val palette = downsampled.generatePalette()
                        val swatch = with(palette) { vibrantSwatch ?: mutedSwatch }
                        val color = swatch?.rgb?.let { Color(it) }
                        Pair(bitmap, color)
                    } else {
                        Pair(bitmap, null)
                    }
                } else {
                    Pair(null, null)
                }
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }


}