/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package util.phonograph.tageditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

class TagEditorViewModel : ViewModel() {

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
    }
    override fun onCleared() {
        super.onCleared()
    }
}
