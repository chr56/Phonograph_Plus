package com.kabouzeid.gramophone.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.MusicUtil
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import java.io.File
import java.io.IOException
// Todo Completed
/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class SongDetailDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context: Activity? = activity
        val song: Song? = requireArguments().getParcelable("song")
        val dialog = MaterialDialog(context as Context)
            .title(R.string.label_details)
            .positiveButton(android.R.string.ok)
            .customView(viewRes = R.layout.dialog_file_details,horizontalPadding = true)

        val dialogView: View = dialog.getCustomView()

        val fileName: TextView = dialogView.findViewById(R.id.file_name)
        val filePath: TextView = dialogView.findViewById(R.id.file_path)
        val fileSize: TextView = dialogView.findViewById(R.id.file_size)
        val fileFormat: TextView = dialogView.findViewById(R.id.file_format)
        val trackLength: TextView = dialogView.findViewById(R.id.track_length)
        val bitRate: TextView = dialogView.findViewById(R.id.bitrate)
        val samplingRate: TextView = dialogView.findViewById(R.id.sampling_rate)
        fileName.text = makeTextWithTitle(context, R.string.label_file_name, "-")
        filePath.text = makeTextWithTitle(context, R.string.label_file_path, "-")
        fileSize.text = makeTextWithTitle(context, R.string.label_file_size, "-")
        fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, "-")
        trackLength.text = makeTextWithTitle(context, R.string.label_track_length, "-")
        bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, "-")
        samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, "-")
        if (song != null) {
            val songFile = File(song.data)
            if (songFile.exists()) {
                fileName.text = makeTextWithTitle(context, R.string.label_file_name, songFile.name)
                filePath.text = makeTextWithTitle(context, R.string.label_file_path, songFile.absolutePath)
                fileSize.text = makeTextWithTitle(context, R.string.label_file_size, getFileSizeString(songFile.length()))
                try {
                    val audioFile: AudioFile = AudioFileIO.read(songFile)
                    val audioHeader: AudioHeader = audioFile.audioHeader
                    fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, audioHeader.format)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString((audioHeader.trackLength * 1000).toLong()))
                    bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, audioHeader.bitRate + " kb/s")
                    samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, audioHeader.sampleRate + " Hz")
                } catch (e: CannotReadException) {
                    Log.e(TAG, "error while reading the song file", e)
                    // fallback
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: IOException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: TagException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: ReadOnlyFileException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: InvalidAudioFrameException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                }
            } else {
                // fallback
                fileName.text = makeTextWithTitle(context, R.string.label_file_name, song.title)
                trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
            }
        }
        return dialog
    }

    companion object {
        val TAG: String = SongDetailDialog::class.java.simpleName
        @JvmStatic
        fun create(song: Song?): SongDetailDialog {
            val dialog = SongDetailDialog()
            val args = Bundle()
            args.putParcelable("song", song)
            dialog.arguments = args
            return dialog
        }

        private fun makeTextWithTitle(context: Context, titleResId: Int, text: String): Spanned {
            return Html.fromHtml("<b>" + context.resources.getString(titleResId) + ": " + "</b>" + text)
        }

        private fun getFileSizeString(sizeInBytes: Long): String {
            val fileSizeInKB = sizeInBytes / 1024
            val fileSizeInMB = fileSizeInKB / 1024
            return "$fileSizeInMB MB"
        }
    }
}
