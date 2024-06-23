/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import com.google.android.material.textfield.TextInputLayout
import lib.storage.launcher.SAFActivityResultContracts
import lib.storage.textparser.DocumentUriPathParser.documentUriBasePath
import player.phonograph.R
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.mechanism.playlist.mediastore.createPlaylistViaMediastore
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.util.coroutineToast
import player.phonograph.util.openOutputStreamSafe
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.reportError
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePlaylistDialog : DialogFragment() {

    private lateinit var alertDialog: AlertDialog
    private lateinit var songs: List<Song>

    private var selectedUri: Uri? = null

    private lateinit var nameBox: TextInputLayout
    private lateinit var locationBox: TextInputLayout
    private lateinit var cancelButton: AppCompatButton
    private lateinit var createButton: AppCompatButton
    private lateinit var useSafCheckbox: AppCompatCheckBox

    private lateinit var nameEditText: EditText
    private lateinit var locationEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        songs = requireArguments().parcelableArrayList<Song>(SONGS)!!
        alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_playlist_title)
            .setView(R.layout.dialog_create_playlist)
            .create()
        return alertDialog
    }

    override fun onStart() {
        super.onStart()
        bind(alertDialog)
        setupMainView(alertDialog)
    }

    private fun bind(alertDialog: AlertDialog) {
        nameBox = alertDialog.findViewById(R.id.name)!!
        locationBox = alertDialog.findViewById(R.id.location)!!
        cancelButton = alertDialog.findViewById(R.id.button_cancel)!!
        createButton = alertDialog.findViewById(R.id.button_create)!!
        useSafCheckbox = alertDialog.findViewById(R.id.checkBox_saf)!!

        nameEditText = nameBox.editText!!
        locationEditText = locationBox.editText!!
    }

    private fun setupMainView(alertDialog: AlertDialog) {
        nameEditText.setText(R.string.new_playlist_title)

        useSafCheckbox.setOnCheckedChangeListener { _, value ->
            locationBox.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        locationBox.setEndIconOnClickListener {
            coroutineScope.launch {
                selectedUri = selectFile()
            }
        }

        createButton.setOnClickListener {
            val context = it.context
            coroutineScope.launch {
                if (useSafCheckbox.isChecked) {
                    var uri = selectedUri
                    if (uri != null) {
                        writePlaylist(context, uri, songs)
                    } else {
                        uri = selectFile()
                        writePlaylist(context, uri, songs)
                    }
                } else {
                    createFromMediaStore(nameEditText.text.toString())
                }
                alertDialog.dismiss()
            }
        }

        cancelButton.setOnClickListener { alertDialog.dismiss() }
    }

    private suspend fun selectFile(): Uri {
        val context = requireContext()
        val playlistText = nameEditText.text
        val documentUri = createNewFile(context, playlistText)
        withContext(Dispatchers.Main) {
            locationEditText.setText(documentUriBasePath(documentUri.pathSegments))
        }
        return documentUri
    }

    private suspend fun createFromMediaStore(name: String) {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT).show()
            return
        }
        val activity = requireActivity()
        if (!PlaylistLoader.checkExistence(activity, name)) {
            coroutineScope.launch(Dispatchers.IO) {
                createPlaylistViaMediastore(activity, name, songs)
            }
        } else {
            Toast.makeText(
                activity,
                requireActivity().resources.getString(
                    R.string.playlist_exists,
                    name
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val SONGS = "songs"

        fun create(songs: List<Song>): CreatePlaylistDialog =
            CreatePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(SONGS, ArrayList(songs))
                }
            }

        private suspend fun createNewFile(
            context: Context,
            playlistName: CharSequence,
        ): Uri {
            // launch
            val documentUri = SAFActivityResultContracts.createFileViaSAF(context, "$playlistName.m3u")
            // process
            return documentUri
        }

        private suspend fun writePlaylist(context: Context, uri: Uri, songs: List<Song>) {
            openOutputStreamSafe(context, uri, "rwt")?.use { stream ->
                try {
                    M3UWriter.write(stream, songs, true)
                    coroutineToast(context, R.string.success)
                    delay(250)
                    sentPlaylistChangedLocalBoardCast()
                } catch (e: Exception) {
                    reportError(e, "Playlist", "Failed to write $uri")
                    coroutineToast(context, R.string.failed)
                }
            }
        }
    }
}
