/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package util.phonograph.tageditor

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.ArtworkFactory
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.notification.BackgroundNotification
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.MusicUtil
import player.phonograph.util.MusicUtil.createAlbumArtFile

class TagEditorViewModel : ViewModel() {

    var id: Long = -1

    fun getAudioFile(path: String): AudioFile {
        return try {
            AudioFileIO.read(File(path))
        } catch (e: Exception) {
            Log.e(TAG, "Could not read audio file $path", e)
            AudioFile()
        }
    }

    fun getSongTitle(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.TITLE)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getAlbumTitle(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.ALBUM)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getArtistName(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.ARTIST)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getAlbumArtistName(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.ALBUM_ARTIST)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getGenreName(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.GENRE)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getSongYear(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.YEAR)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getTrackNumber(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.TRACK)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getLyrics(): String? {
        return try {
            getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.getFirst(FieldKey.LYRICS)
        } catch (ignored: Exception) {
            null
        }
    }

    fun getAlbumArt(): Bitmap? {
        return try {
            val artworkTag = getAudioFile(songPaths!![0]).tagOrCreateAndSetDefault.firstArtwork
            if (artworkTag != null) {
                val artworkBinaryData = artworkTag.binaryData
                return BitmapFactory.decodeByteArray(artworkBinaryData, 0, artworkBinaryData.size)
            }
            null
        } catch (ignored: Exception) {
            null
        }
    }

    var songPaths: List<String>? = null

    companion object {
        private const val TAG: String = "TagEditorViewModel"

        private const val NOTIFICATION_CODE = 824_3348
    }

    override fun onCleared() {
        super.onCleared()
        writeTagsJob?.cancel()
    }

    var writeTagsJob: Job? = null

    fun writeTagsToSong(info: LoadingInfo, c: Context) {

        val contextWeakWrapper = WeakReference(c)

        val result = viewModelScope.async(Dispatchers.IO) {
            try {

                BackgroundNotification.post(
                    App.instance.getString(R.string.action_tag_editor),
                    App.instance.getString(R.string.saving_changes),
                    NOTIFICATION_CODE
                )

                var artwork: Artwork? = null
                var albumArtFile: File? = null

                if (info.artworkInfo?.artwork != null) {
                    try {
                        albumArtFile = createAlbumArtFile().canonicalFile
                        info.artworkInfo.artwork.compress(Bitmap.CompressFormat.PNG, 0, FileOutputStream(albumArtFile))
                        artwork = ArtworkFactory.createArtworkFromFile(albumArtFile)
                    } catch (e: IOException) {
                        Log.w(TAG, e)
                        ErrorNotification.postErrorNotification(e, "Failed to save artwork\n")
                    }
                }

                var wroteArtwork = false
                var deletedArtwork = false

                // var counter = 0

                for (filePath in info.filePaths) {
                    // counter++
                    try {
                        AudioFileIO.read(File(filePath)).also { file ->

                            val tag = file.tagOrCreateAndSetDefault

                            if (info.fieldKeyValueMap != null) {
                                for ((key, value) in info.fieldKeyValueMap) {
                                    try {
                                        tag.setField(key, value)
                                    } catch (e: Exception) {
                                        Log.w(TAG, e)
                                        ErrorNotification.postErrorNotification(
                                            e, "Failed to save tag:${key.name}<->${value}\n"
                                        )
                                    }
                                }
                            }

                            if (info.artworkInfo != null) {
                                if (info.artworkInfo.artwork == null) {
                                    tag.deleteArtworkField()
                                    deletedArtwork = true
                                } else if (artwork != null) {
                                    tag.deleteArtworkField()
                                    tag.setField(artwork)
                                    wroteArtwork = true
                                }
                            }
                        }.commit()
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                        when (e) {
                            is CannotReadException,
                            is IOException,
                            is CannotWriteException,
                            is InvalidAudioFrameException,
                            is ReadOnlyFileException,
                            is TagException,
                            -> {
                                ErrorNotification.postErrorNotification(e, "Failed to save tags")
                            }
                            else -> throw e
                        }
                    }
                }

                if (wroteArtwork) {
                    MusicUtil.insertAlbumArt(App.instance, info.artworkInfo!!.albumId, albumArtFile!!.path)
                } else if (deletedArtwork) {
                    MusicUtil.deleteAlbumArt(App.instance, info.artworkInfo!!.albumId)
                }

                info.filePaths.toTypedArray()
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorNotification.postErrorNotification(e, "Failed to save tags")
                null
            } finally {
                BackgroundNotification.remove(NOTIFICATION_CODE)
            }
        }

        writeTagsJob = result

        viewModelScope.launch {
            val modifiedFiles = result.await()
            val context = contextWeakWrapper.get()

            if (modifiedFiles != null) {
                val listener =
                    if (context is Activity) UpdateToastMediaScannerCompletionListener(context, modifiedFiles) else null

                MediaScannerConnection.scanFile(
                    App.instance,
                    modifiedFiles,
                    null,
                    listener
                )
            } else {
                ErrorNotification.postErrorNotification(
                    IllegalStateException("No Songs Modified!?"),
                    "No Songs Modified!? Maybe fail save tags?"
                )
            }
        }
    }
}

class LoadingInfo constructor(
    val filePaths: Collection<String>,
    val fieldKeyValueMap: Map<FieldKey, String>?,
    val artworkInfo: ArtworkInfo?,
)

class ArtworkInfo(val albumId: Long, val artwork: Bitmap?)
