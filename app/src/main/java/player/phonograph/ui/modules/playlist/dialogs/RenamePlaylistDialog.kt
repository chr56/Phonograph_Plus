/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import com.google.android.material.textfield.TextInputLayout
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.parcelable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenamePlaylistDialog : DialogFragment() {

    private lateinit var alertDialog: AlertDialog
    private lateinit var playlist: Playlist


    private lateinit var nameBox: TextInputLayout
    private lateinit var nameEditText: EditText
    private lateinit var cancelButton: AppCompatButton
    private lateinit var renameButton: AppCompatButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        playlist = requireArguments().parcelable<Playlist>(PLAYLIST)!!
        alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename_playlist_title)
            .setView(R.layout.dialog_rename_playlist)
            .create()
        return alertDialog
    }

    private fun bind(alertDialog: AlertDialog) {
        nameBox = alertDialog.findViewById(R.id.name)!!
        nameEditText = nameBox.editText!!

        cancelButton = alertDialog.findViewById(R.id.button_cancel)!!
        renameButton = alertDialog.findViewById(R.id.button_rename)!!
    }


    override fun onStart() {
        super.onStart()
        bind(alertDialog)

        nameEditText.setText(playlist.name)

        cancelButton.setOnClickListener { alertDialog.dismiss() }
        renameButton.setOnClickListener {
            val context = requireContext()
            coroutineScope.launch {
                rename(context, playlist, nameEditText.text.toString())
            }
            alertDialog.dismiss()
        }
    }

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val PLAYLIST = "playlist"

        fun create(playlist: Playlist): RenamePlaylistDialog =
            RenamePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(PLAYLIST, playlist)
                }
            }

        private suspend fun rename(context: Context, playlist: Playlist, name: String) {
            if (name.trim().isNotEmpty()) {
                val result = PlaylistProcessors.writer(playlist)!!.rename(context, name)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (result) R.string.success else R.string.failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }
}
