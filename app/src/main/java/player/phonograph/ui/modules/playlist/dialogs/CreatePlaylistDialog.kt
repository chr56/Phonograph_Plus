/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import lib.storage.launcher.SAFActivityResultContracts
import lib.storage.textparser.DocumentUriPathParser.documentUriBasePath
import player.phonograph.R
import player.phonograph.databinding.DialogCreatePlaylistBinding
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.util.coroutineToast
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
            .show()
        binding = DialogCreatePlaylistBinding.bind(alertDialog.findViewById(R.id.content_container)!!)
        return alertDialog
    }

    @Deprecated("Deprecated")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupMainView(alertDialog)
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage")
    private fun setupMainView(alertDialog: AlertDialog) {

        binding.checkBoxSaf.setOnCheckedChangeListener { _, value ->
            viewModel.updateUseSAF(value)
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

        with(binding.spinner) {
            val options = listOf(
                getString(R.string.file_playlists),
                // getString(R.string.database_playlists),
            )
            val adapter = ArrayAdapter(context, R.layout.item_dropdown, options)
            setAdapter(adapter)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position) {
                        1 -> viewModel.updateMode(DialogViewModel.MODE_FILE_DATABASE)
                        0 -> viewModel.updateMode(
                            if (viewModel.useSAF.value) DialogViewModel.MODE_FILE_SAF else DialogViewModel.MODE_FILE_MEDIASTORE
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.location.collect { path ->
                    binding.location.editText?.setText(path)
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mode.collect { mode ->
                    binding.location.visibility =
                        if (mode == DialogViewModel.MODE_FILE_SAF) View.VISIBLE else View.INVISIBLE
                    binding.checkBoxSaf.visibility =
                        if (mode == DialogViewModel.MODE_FILE_DATABASE) View.INVISIBLE else View.VISIBLE
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

        private val _mode: MutableStateFlow<Int> = MutableStateFlow(MODE_FILE_SAF)
        val mode get() = _mode.asStateFlow()
        fun updateMode(mode: Int) {
            _mode.update { mode }
        }

        // Use SAF
        private val _useSAF: MutableStateFlow<Boolean> = MutableStateFlow(true)
        val useSAF get() = _useSAF.asStateFlow()
        fun updateUseSAF(useSAF: Boolean) {
            _useSAF.update { useSAF }
        }


        private val _uri: MutableStateFlow<Uri?> = MutableStateFlow(null)
        val uri get() = _uri.asStateFlow()

        private val _location: MutableStateFlow<String?> = MutableStateFlow(null)
        val location get() = _location.asStateFlow()

        suspend fun selectFile(context: Context): Uri = withContext(Dispatchers.IO) {
            val documentUri = makeNewFile(context, name.value ?: context.getString(R.string.new_playlist_title))
            _uri.update { documentUri }
            _location.update { documentUriBasePath(documentUri.pathSegments) }
            documentUri
        }

        suspend fun execute(context: Context, songs: List<Song>) = withContext(Dispatchers.IO) {
            when (mode.value) {
                MODE_FILE_SAF        -> createFromSAF(context, songs)
                MODE_FILE_MEDIASTORE -> createFromMediaStore(context, name.value, songs)
                MODE_FILE_DATABASE   -> createFromDatabase(context, name.value, songs)
                else                 -> throw IllegalStateException("Illegal mode ${mode.value}")
            }
        }

        private suspend fun makeNewFile(context: Context, playlistName: CharSequence): Uri =
            SAFActivityResultContracts.createFileViaSAF(context, "$playlistName.m3u")

        private suspend fun createFromDatabase(context: Context, name: String?, songs: List<Song>) {

        }

        private suspend fun createFromSAF(context: Context, songs: List<Song>) {
            val uri = _uri.value ?: selectFile(context)
            PlaylistManager.create(context, songs, uri)
        }

        private suspend fun createFromMediaStore(context: Context, name: String?, songs: List<Song>) {
            val result = PlaylistManager.create(context, songs, name)
            val message = when (result) {
                -1L  -> context.getString(R.string.failed)
                -2L  -> context.getString(R.string.playlist_exists, name)
                else -> context.getString(R.string.success)
            }
            coroutineToast(context, message)
        }

        companion object {
            const val MODE_FILE_SAF = 1
            const val MODE_FILE_MEDIASTORE = 2
            const val MODE_FILE_DATABASE = 4
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
