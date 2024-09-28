/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import lib.storage.launcher.SAFActivityResultContracts
import lib.storage.textparser.DocumentUriPathParser.documentUriBasePath
import player.phonograph.R
import player.phonograph.databinding.DialogCreatePlaylistBinding
import player.phonograph.mechanism.playlist.mediastore.createPlaylistViaMediastore
import player.phonograph.mechanism.playlist.saf.writePlaylist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.util.parcelableArrayList
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePlaylistDialog : DialogFragment() {

    private lateinit var alertDialog: AlertDialog
    private lateinit var songs: List<Song>

    private lateinit var binding: DialogCreatePlaylistBinding
    private val viewModel: DialogViewModel by viewModels<DialogViewModel>(ownerProducer = { requireActivity() })

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
        binding = DialogCreatePlaylistBinding.bind(alertDialog.findViewById(R.id.content_container)!!)
        setupMainView(alertDialog)
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage")
    private fun setupMainView(alertDialog: AlertDialog) {

        binding.checkBoxSaf.setOnCheckedChangeListener { _, value ->
            viewModel.updateMode(value)
            binding.location.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        binding.name.editText?.addTextChangedListener { editable ->
            viewModel.updateName(editable?.toString())
        }

        binding.location.setEndIconOnClickListener {
            coroutineScope.launch {
                viewModel.selectFile(requireActivity())
            }
        }

        binding.buttonCancel.setOnClickListener { alertDialog.dismiss() }

        binding.buttonCreate.setOnClickListener {
            val context = requireActivity()
            coroutineScope.launch {
                viewModel.execute(context, songs)
                alertDialog.dismiss()
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.location.collect { path ->
                    binding.location.editText?.setText(path)
                }
            }
        }

    }

    class DialogViewModel : ViewModel() {

        private val _name: MutableStateFlow<String?> = MutableStateFlow(null)
        val name get() = _name.asStateFlow()
        fun updateName(name: String?) {
            _name.update { name }
        }

        // Use SAF
        private val _mode: MutableStateFlow<Boolean> = MutableStateFlow(true)
        val mode get() = _mode.asStateFlow()
        fun updateMode(mode: Boolean) {
            _mode.update { mode }
        }


        private val _uri: MutableStateFlow<Uri?> = MutableStateFlow(null)
        val uri get() = _uri.asStateFlow()

        private val _location: MutableStateFlow<String?> = MutableStateFlow(null)
        val location get() = _location.asStateFlow()

        suspend fun selectFile(context: Context): Uri = withContext(Dispatchers.IO) {
            val documentUri = createNewFile(context, name.value ?: context.getString(R.string.new_playlist_title))
            _uri.update { documentUri }
            _location.update { documentUriBasePath(documentUri.pathSegments) }
            documentUri
        }

        suspend fun execute(context: Context, songs: List<Song>) = withContext(Dispatchers.IO) {
            if (mode.value) {
                var uri = _uri.value
                if (uri != null) {
                    writePlaylist(context, uri, songs)
                } else {
                    uri = selectFile(context)
                    writePlaylist(context, uri, songs)
                }
            } else {
                createFromMediaStore(context, name.value ?: context.getString(R.string.new_playlist_title), songs)
            }
        }


        private suspend fun createNewFile(
            context: Context,
            playlistName: CharSequence,
        ): Uri = SAFActivityResultContracts.createFileViaSAF(context, "$playlistName.m3u")

        private suspend fun createFromMediaStore(activity: Context, name: String, songs: List<Song>) {
            if (name.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, activity.getString(R.string.failed), Toast.LENGTH_SHORT).show()
                }
                return
            }
            if (!PlaylistLoader.checkExistence(activity, name)) {
                val id = createPlaylistViaMediastore(activity, name, songs)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        activity,
                        if (id != -1L) activity.getString(R.string.success) else activity.getString(R.string.failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.playlist_exists, name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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

    }
}
