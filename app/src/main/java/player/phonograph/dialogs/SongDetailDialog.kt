package player.phonograph.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import mt.pref.ThemeColor.accentColor
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.tag.EmptyFrameException
import org.jaudiotagger.tag.FieldDataInvalidException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.InvalidDataTypeException
import org.jaudiotagger.tag.InvalidFrameIdentifierException
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.TagNotFoundException
import org.jaudiotagger.tag.datatype.DataTypes
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.getFileSizeString
import player.phonograph.model.getReadableDurationString
import player.phonograph.notification.ErrorNotification
import androidx.fragment.app.DialogFragment
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class SongDetailDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context: Activity? = activity
        val song: Song = requireArguments().getParcelable("song")!!
        val dialog = MaterialDialog(context as Context)
            .title(R.string.label_details)
            .positiveButton(android.R.string.ok)
            .customView(
                viewRes = R.layout.dialog_file_details,
                horizontalPadding = true,
                scrollable = true
            ).apply {
                getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor(context))
            }

        val dialogView: View = dialog.getCustomView()

        val fileName: TextView = dialogView.findViewById(R.id.file_name)
        val filePath: TextView = dialogView.findViewById(R.id.file_path)
        val fileSize: TextView = dialogView.findViewById(R.id.file_size)
        val fileFormat: TextView = dialogView.findViewById(R.id.file_format)
        val trackLength: TextView = dialogView.findViewById(R.id.track_length)
        val bitRate: TextView = dialogView.findViewById(R.id.bitrate)
        val samplingRate: TextView = dialogView.findViewById(R.id.sampling_rate)

        val title: TextView = dialogView.findViewById(R.id.text_song_title)
        val artist: TextView = dialogView.findViewById(R.id.text_song_artist_name)
        val album: TextView = dialogView.findViewById(R.id.text_song_album_name)
        val albumArtist: TextView = dialogView.findViewById(R.id.text_song_album_artist)
        val year: TextView = dialogView.findViewById(R.id.text_song_year)
        val genre: TextView = dialogView.findViewById(R.id.text_song_genre)
        val track: TextView = dialogView.findViewById(R.id.text_song_track_number)
        val other: TextView = dialogView.findViewById(R.id.text_song_other)

        fileName.text = makeTextWithTitle(context, R.string.label_file_name, "-")
        filePath.text = makeTextWithTitle(context, R.string.label_file_path, "-")
        fileSize.text = makeTextWithTitle(context, R.string.label_file_size, "-")
        fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, "-")
        trackLength.text = makeTextWithTitle(context, R.string.label_track_length, "-")
        bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, "-")
        samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, "-")

        title.text = makeTextWithTitle(context, R.string.title, "-")
        artist.text = makeTextWithTitle(context, R.string.artist, "-")
        album.text = makeTextWithTitle(context, R.string.album, "-")
        albumArtist.text = makeTextWithTitle(context, R.string.album_artist, "-")
        year.text = makeTextWithTitle(context, R.string.year, "-")
        genre.text = makeTextWithTitle(context, R.string.genre, "-")
        track.text = makeTextWithTitle(context, R.string.track, "-")
        other.text = makeTextWithTitle(context, R.string.other_information, "-")

        val songFile = File(song.data)
        fileName.text = makeTextWithTitle(context, R.string.label_file_name, "N/A")
        trackLength.text = makeTextWithTitle(context, R.string.label_track_length, getReadableDurationString(song.duration))
        if (songFile.exists()) {
            fileName.text = makeTextWithTitle(context, R.string.label_file_name, songFile.name)
            filePath.text = makeTextWithTitle(context, R.string.label_file_path, songFile.absolutePath)
            fileSize.text = makeTextWithTitle(context, R.string.label_file_size, getFileSizeString(songFile.length()))
            runCatching {
                val audioFile: AudioFile = AudioFileIO.read(songFile)

                // files of the song
                val audioHeader: AudioHeader = audioFile.audioHeader
                fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, audioHeader.format)
                trackLength.text = makeTextWithTitle(context, R.string.label_track_length, getReadableDurationString((audioHeader.trackLength * 1000).toLong()))
                bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, audioHeader.bitRate + " kb/s")
                samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, audioHeader.sampleRate + " Hz")
                // tags of the song
                title.text = makeTextWithTitle(context, R.string.title, song.title)
                artist.text = makeTextWithTitle(context, R.string.artist, song.artistName!!)
                album.text = makeTextWithTitle(context, R.string.album, song.albumName!!)
                albumArtist.text = makeTextWithTitle(context, R.string.album_artist, audioFile.tag.getFirst(FieldKey.ALBUM_ARTIST))
                if (song.year != 0) year.text = makeTextWithTitle(context, R.string.year, song.year.toString())
                val songGenre = audioFile.tag.getFirst(FieldKey.GENRE)
                genre.text = makeTextWithTitle(context, R.string.genre, songGenre)
                if (song.trackNumber != 0) track.text = makeTextWithTitle(context, R.string.track, song.trackNumber.toString())

                val custInfoField = audioFile.tag.getFields("TXXX")
                var custInfo = "-"
                if (custInfoField != null && custInfoField.size > 0) {
                custInfo = "<br />"
                if (custInfoField.size <= 128) {
                    custInfoField.forEach { TagField ->
                        val frame = TagField as AbstractID3v2Frame
                        custInfo += frame.body.getObjectValue(DataTypes.OBJ_DESCRIPTION)
                        custInfo += ":<br />"
                        custInfo += frame.body.getObjectValue(DataTypes.OBJ_TEXT)
                        custInfo += "<br />"
                    }
                } else {
                    Toast.makeText(requireContext(),"Other tags in this song is too many, only show the first 128 entries",Toast.LENGTH_LONG).show()
                    for (index in 0 until 127){
                        val frame = custInfoField[index] as AbstractID3v2Frame
                        custInfo += frame.body.getObjectValue(DataTypes.OBJ_DESCRIPTION)
                        custInfo += ":<br />"
                        custInfo += frame.body.getObjectValue(DataTypes.OBJ_TEXT)
                        custInfo += "<br />"
                    }
                }
                }
                other.text = makeTextWithTitle(context, R.string.other_information, custInfo)
            }.apply {
                if (isFailure){
                    val errorMsg = when(val e = exceptionOrNull()){
                        is CannotReadException ->
                            "Can not read the song file $songFile"
                        is IOException ->
                            "IOException occurs when reading file"
                        is InvalidAudioFrameException ->
                            "AudioFrame not found"
                        is TagException -> {
                            val msg = when(e){
                                is TagNotFoundException -> "Tag not found: ${e.message}"
                                is FieldDataInvalidException -> "FieldDataInvalid: ${e.message}"
                                is EmptyFrameException -> "Find a Frame but it contains no data: ${e.message}"
                                is InvalidDataTypeException -> "InvalidDataType: ${e.message}"
                                is InvalidFrameIdentifierException -> "Frame Identifier is invalid: ${e.message}"
                                else -> "Unknown"
                            }
                            "Tag Error: $msg"
                        }
                        else ->
                            "Unknown"
                    }
                    Log.w(TAG, errorMsg)
                    ErrorNotification.postErrorNotification(
                        exceptionOrNull()?: Exception(),
                        "Error while reading the song file:\n$errorMsg",
                    )
                }
            }
        }
        return dialog
    }

    companion object {
        val TAG: String = SongDetailDialog::class.java.simpleName
        @JvmStatic
        fun create(song: Song): SongDetailDialog {
            val dialog = SongDetailDialog()
            val args = Bundle()
            args.putParcelable("song", song)
            dialog.arguments = args
            return dialog
        }

        private fun makeTextWithTitle(context: Context, titleResId: Int, text: String): Spanned {
            return Html.fromHtml("<b>" + context.resources.getString(titleResId) + ": " + "</b>" + text, Html.FROM_HTML_MODE_COMPACT)
        }
        private fun makeTextWithTitle(context: Context, title: String, text: String): Spanned {
            return Html.fromHtml("<b>$title: </b>$text", Html.FROM_HTML_MODE_COMPACT)
        }
    }
}
