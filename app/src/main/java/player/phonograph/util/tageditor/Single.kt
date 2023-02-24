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
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.IOpenFileStorageAccess
import player.phonograph.misc.OpenDocumentContract
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.notification.BackgroundNotification
import player.phonograph.util.Util.reportError
import player.phonograph.util.Util.warning
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import android.app.Activity
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException


fun applyEdit(
    scope: CoroutineScope,
    context: Context,
    songFile: File,
    allEditRequest: Map<FieldKey, String?>,
    needDeleteCover: Boolean,
    needReplaceCover: Boolean,
    newCoverUri: Uri?
) {
    scope.launch(Dispatchers.Default) {
        // notify user first
        BackgroundNotification.post(
            App.instance.getString(R.string.action_tag_editor),
            App.instance.getString(R.string.saving_changes),
            TAG_EDITOR_NOTIFICATION_CODE
        )
        // process
        withContext(Dispatchers.IO) {
            if (allEditRequest.isNotEmpty()) {
                applyTagEditImpl(context, songFile, allEditRequest)
            }
            if (needReplaceCover) {
                replaceArtwork(context, songFile, newCoverUri!!)
            } else if (needDeleteCover) {
                deleteArtWork(songFile)
            }
        }
        // notify user
        BackgroundNotification.remove(TAG_EDITOR_NOTIFICATION_CODE)
        // refresh media store
        val listener =
            if (context is Activity)
                UpdateToastMediaScannerCompletionListener(
                    context,
                    arrayOf(songFile.path)
                ) else null
        yield()
        MediaScannerConnection.scanFile(
            App.instance, arrayOf(songFile.path), null, listener
        )
    }
}

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
            reportError(e, LOGTAG, "Failed to write tag!")
        }
    } else {
        warning(LOGTAG, "Failed to replace Artwork!")
    }
    cacheFile?.delete()

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
            warning(LOGTAG, "Can not open selected file! (uri: $uri)")
        }
    }
    return cache
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


private const val TAG_EDITOR_NOTIFICATION_CODE = 824_3348


private fun applyTagEditImpl(context: Context, songFile: File, requests: Map<FieldKey, String?>) {
    safeEditTag(songFile.path) {
        val file = AudioFileIO.read(songFile)
        writeTags(file, requests)
        file.commit()
    }
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

private fun Exception.report(message: String) = reportError(this, LOGTAG, message)

private const val HINT = "please check file or storage permission"
private const val LOGTAG = "TagEdit"

fun selectNewArtwork(activity: Context): MutableState<Uri?> {
    val state = mutableStateOf<Uri?>(null)
    if (activity is IOpenFileStorageAccess) {
        val accessTool = activity.openFileStorageAccessTool
        val cfg = OpenDocumentContract.Cfg(null, mime_types = arrayOf("image/*"))
        accessTool.launch(cfg) { uri ->
            if (uri != null) {
                state.value = uri
            } else {
                warning("replaceArtworkImpl", "Failed to select Image File")
            }
        }
    } else {
        throw IllegalStateException("${activity.javaClass} can not select file!")
    }
    return state
}
