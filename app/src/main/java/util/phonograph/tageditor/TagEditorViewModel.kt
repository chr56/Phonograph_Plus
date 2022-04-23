/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package util.phonograph.tageditor

import android.util.Log
import androidx.lifecycle.ViewModel
import java.io.File
import kotlinx.coroutines.Job
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO

class TagEditorViewModel : ViewModel() {

    fun getAudioFile(path: String): AudioFile {
        return try {
            AudioFileIO.read(File(path))
        } catch (e: Exception) {
            Log.e(TAG, "Could not read audio file $path", e)
            AudioFile()
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
