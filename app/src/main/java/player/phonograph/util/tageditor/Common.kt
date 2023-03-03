/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.tageditor

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.ArtworkFactory
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import player.phonograph.util.Util
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * shared process for all tag edit
 */
internal fun applyEditImpl(
    context: Context,
    songFile: File,
    allEditRequest: Map<FieldKey, String?>,
    needDeleteCover: Boolean,
    needReplaceCover: Boolean,
    newCoverUri: Uri?
) {
    require(songFile.canWrite()) { "can not write ${songFile.absoluteFile}" } // must writable
    if (allEditRequest.isNotEmpty()) {
        applyTagEditImpl(context, songFile, allEditRequest)
    }
    if (needReplaceCover) {
        replaceArtwork(context, songFile, newCoverUri!!)
    } else if (needDeleteCover) {
        deleteArtWork(songFile)
    }
}



fun selectNewArtwork(activity: Context): MutableState<Uri?> {
    val state = mutableStateOf<Uri?>(null)
    if (activity is IOpenFileStorageAccess) {
        val accessTool = activity.openFileStorageAccessTool
        val cfg = OpenDocumentContract.Config(mime_types = arrayOf("image/*"))
        accessTool.launch(cfg) { uri ->
            if (uri != null) {
                state.value = uri
            } else {
                Util.warning("replaceArtworkImpl", "Failed to select Image File")
            }
        }
    } else {
        throw IllegalStateException("${activity.javaClass} can not select file!")
    }
    return state
}

private fun writeTags(file: AudioFile, requests: Map<FieldKey, String?>) {
    val tagsHeader = file.tagOrCreateAndSetDefault
    for ((tagKey, value) in requests) {
        writeTag(tagsHeader, tagKey, value)
    }
}

private fun writeTag(tagsHeader: Tag, tagKey: FieldKey, value: String?) {
    try {
        if (value.isNullOrEmpty()) {
            tagsHeader.deleteField(tagKey)
        } else {
            tagsHeader.setField(tagKey, value)
        }
    } catch (e: KeyNotFoundException) {
        e.report("Unknown FieldKey: $tagKey")
    } catch (e: TagException) {
        e.report("Failed to save tag: $tagKey --> $$value")
    }
}

private inline fun safeEditTag(path: String, block: () -> Unit) {
    try {
        block()
    } catch (e: CannotReadException) {
        e.report("Failed to read file, $HINT!")
    } catch (e: CannotWriteException) {
        e.report("Failed to write file, $HINT!")
    } catch (e: IOException) {
        e.report("IO error, $HINT!")
    } catch (e: ReadOnlyFileException) {
        e.report("File is read only: $path.")
    } catch (e: InvalidAudioFrameException) {
        e.report("File maybe corrupted, $HINT!")
    } catch (e: TagException) {
        e.report("Tag(s) may have glitches, $HINT!")
    }
}

private fun Exception.report(message: String) = Util.reportError(this, LOGTAG, message)

private const val HINT = "please check file or storage permission"
private const val LOGTAG = "TagEdit"

private fun replaceArtwork(activity: Context, songFile: File, uri: Uri) {
    val cacheFile =
        try {
            copyImageToCache(activity, songFile, uri)
        } catch (e: Exception) {
            null
        }
    if (cacheFile != null) {
        try {
            replaceArtworkImpl(songFile, cacheFile)
        } catch (e: Exception) {
            Util.reportError(e, LOGTAG, "Failed to write tag!")
        }
    } else {
        Util.warning(LOGTAG, "Failed to replace Artwork!")
    }
    cacheFile?.delete()

}

private fun replaceArtworkImpl(songFile: File, imageFile: File) {
    val file = AudioFileIO.read(songFile)
    val artwork: Artwork? = ArtworkFactory.createArtworkFromFile(imageFile)
    val tag = file.tagOrCreateAndSetDefault
    tag.deleteArtworkField()
    tag.setField(artwork)
    file.commit()
}

private fun deleteArtWork(songFile: File) {
    safeEditTag(songFile.path) {
        val file = AudioFileIO.read(songFile)
        file.tagOrCreateAndSetDefault.also {
            it.deleteArtworkField()
        }
        file.commit()
    }
}

private fun applyTagEditImpl(context: Context, songFile: File, requests: Map<FieldKey, String?>) {
    safeEditTag(songFile.path) {
        val file = AudioFileIO.read(songFile)
        writeTags(file, requests)
        file.commit()
    }
}

private fun copyImageToCache(activity: Context, songFile: File, uri: Uri): File {
    val cache = File(activity.cacheDir, "Cover_${songFile.name}.png")
    activity.contentResolver.openInputStream(uri).use { inputStream ->
        if (inputStream != null) {
            inputStream.buffered(8192).use { bufferedInputStream ->
                cache.outputStream().buffered(8192).use { outputStream ->
                    // transfer stream
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (bufferedInputStream.read(buffer, 0, 8192).also { read = it } >= 0
                    ) {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }
        } else {
            Util.warning(LOGTAG, "Can not open selected file! (uri: $uri)")
        }
    }
    return cache
}