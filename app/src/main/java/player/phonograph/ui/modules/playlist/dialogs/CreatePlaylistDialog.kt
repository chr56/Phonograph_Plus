/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import lib.storage.launcher.SAFActivityResultContracts
import lib.storage.textparser.DocumentUriPathParser.documentTreeUriBasePath
import lib.storage.textparser.DocumentUriPathParser.documentUriBasePath
import player.phonograph.R
import player.phonograph.databinding.DialogCreatePlaylistBinding
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.mechanism.playlist.PlaylistProcessors.reader
import player.phonograph.mechanism.playlist.mediastore.duplicatePlaylistViaMediaStore
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.coroutineToast
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
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
import android.os.Environment
import android.provider.DocumentsContract
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

    private lateinit var binding: DialogCreatePlaylistBinding
    private val viewModel: DialogViewModel by viewModels<DialogViewModel>(ownerProducer = { requireActivity() })

    private var userAction: Int = -1
    private var songs: List<Song> = emptyList()
    private var playlists: List<Playlist> = emptyList()
    private lateinit var defaultName: String

    private fun readArgument(arguments: Bundle) {
        userAction = arguments.getInt(USER_ACTION, 0)
        when (userAction) {
            USER_ACTION_CREATE              -> {
                songs = arguments.parcelableArrayList<Song>(SONGS) ?: emptyList()
                defaultName = resources.getString(R.string.new_playlist_title)
                viewModel.updateName(defaultName)
            }

            USER_ACTION_DUPLICATE_PLAYLIST  -> {
                songs = arguments.parcelableArrayList<Song>(SONGS) ?: emptyList()
                defaultName = arguments.getString(NAME, null)
                viewModel.updateName(defaultName)
            }

            USER_ACTION_DUPLICATE_PLAYLISTS -> {
                playlists = arguments.parcelableArrayList<Playlist>(PLAYLISTS) ?: emptyList()
                defaultName = resources.getString(R.string.new_playlist_title)
                viewModel.updateName(defaultName)
                viewModel.updateMode(DialogViewModel.MODE_PLAYLISTS_SAF)
            }

            else                            -> throw IllegalStateException()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readArgument(requireArguments())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = when (userAction) {
            USER_ACTION_DUPLICATE_PLAYLIST  -> R.string.save_playlist_title
            USER_ACTION_DUPLICATE_PLAYLISTS -> R.string.save_playlists_title
            else                            -> R.string.new_playlist_title
        }
        alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
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

        with(binding.name) {
            editText?.setText(defaultName)
            editText?.addTextChangedListener { editable ->
                viewModel.updateName(editable?.toString())
            }
        }

        binding.location.setEndIconOnClickListener {
            coroutineScope.launch {
                viewModel.selectUri(requireActivity())
            }
        }

        binding.buttonCancel.setOnClickListener { alertDialog.dismiss() }

        binding.buttonCreate.setOnClickListener {
            val context = requireActivity()
            coroutineScope.launch {
                viewModel.execute(context, songs, playlists)
                alertDialog.dismiss()
            }
        }

        binding.spinnerContainer.visibility = if (userAction == USER_ACTION_CREATE) View.VISIBLE else View.GONE
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
                        1 -> viewModel.updateMode(DialogViewModel.MODE_DATABASE)
                        0 -> viewModel.updateMode(
                            if (viewModel.useSAF) DialogViewModel.MODE_FILE_SAF else DialogViewModel.MODE_FILE_MEDIASTORE
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
                        if (mode == DialogViewModel.MODE_FILE_SAF || mode == DialogViewModel.MODE_PLAYLISTS_SAF)
                            View.VISIBLE
                        else
                            View.INVISIBLE
                    binding.name.visibility =
                        if (mode == DialogViewModel.MODE_PLAYLISTS_SAF || mode == DialogViewModel.MODE_PLAYLISTS_MEDIASTORE)
                            View.INVISIBLE
                        else
                            View.VISIBLE
                    binding.checkBoxSaf.visibility =
                        if (mode == DialogViewModel.MODE_DATABASE) View.INVISIBLE else View.VISIBLE
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
        private var _useSAF: Boolean = true
        val useSAF get() = _useSAF
        fun updateUseSAF(useSAF: Boolean) {
            _useSAF = useSAF
            // check mode
            if (mode.value == MODE_FILE_SAF || mode.value == MODE_FILE_MEDIASTORE) {
                updateMode(if (useSAF) MODE_FILE_SAF else MODE_FILE_MEDIASTORE)
            }
            if (mode.value == MODE_PLAYLISTS_SAF || mode.value == MODE_PLAYLISTS_MEDIASTORE) {
                updateMode(if (useSAF) MODE_PLAYLISTS_SAF else MODE_PLAYLISTS_MEDIASTORE)
            }
        }


        private val _uri: MutableStateFlow<Uri?> = MutableStateFlow(null)
        val uri get() = _uri.asStateFlow()

        private val _location: MutableStateFlow<String?> = MutableStateFlow(null)
        val location get() = _location.asStateFlow()


        suspend fun selectUri(context: Context): Uri =
            if (mode.value == MODE_PLAYLISTS_SAF || mode.value == MODE_PLAYLISTS_MEDIASTORE) {
                selectDirectory(context)
            } else {
                selectFile(context)
            }

        suspend fun selectFile(context: Context): Uri = withContext(Dispatchers.IO) {
            val documentUri = makeNewFile(context, name.value ?: context.getString(R.string.new_playlist_title))
            _uri.update { documentUri }
            _location.update { documentUriBasePath(documentUri.pathSegments) }
            documentUri
        }

        suspend fun selectDirectory(context: Context): Uri = withContext(Dispatchers.IO) {
            val documentUri = chooseDirectory(context, Environment.DIRECTORY_MUSIC)
            _uri.update { documentUri }
            _location.update { documentTreeUriBasePath(documentUri.pathSegments) }
            documentUri
        }

        suspend fun execute(context: Context, songs: List<Song>, playlists: List<Playlist>) =
            withContext(Dispatchers.IO) {
                when (mode.value) {
                    MODE_FILE_SAF             -> createFromSAF(context, songs)
                    MODE_FILE_MEDIASTORE      -> createFromMediaStore(context, name.value, songs)
                    MODE_DATABASE             -> createFromDatabase(context, name.value, songs)
                    MODE_PLAYLISTS_SAF        -> duplicatePlaylistsFromSAF(context, playlists)
                    MODE_PLAYLISTS_MEDIASTORE -> duplicatePlaylistsFromMediaStore(context, playlists)
                    else                      -> throw IllegalStateException("Illegal mode ${mode.value}")
                }
            }

        private suspend fun makeNewFile(context: Context, playlistName: CharSequence): Uri =
            SAFActivityResultContracts.createFileViaSAF(context, "$playlistName.m3u")

        private suspend fun chooseDirectory(context: Context, path: String): Uri =
            SAFActivityResultContracts.chooseDirViaSAF(context, path)

        private suspend fun createFromDatabase(context: Context, name: String?, songs: List<Song>) {

        }

        private suspend fun createFromSAF(context: Context, songs: List<Song>) {
            val uri = _uri.value ?: selectFile(context)
            val result = PlaylistManager.create(context, songs, uri)
            val message = when (result) {
                true  -> context.getString(R.string.success)
                false -> context.getString(R.string.failed)
            }
            coroutineToast(context, message)
            _uri.value = null // clear
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


        private suspend fun duplicatePlaylistsFromSAF(context: Context, playlists: List<Playlist>) {
            val treeUri = _uri.value ?: selectFile(context)
            val parentDocumentUri =
                DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

            val failed = mutableListOf<Playlist>()

            for (playlist in playlists) {
                val childUri: Uri? = try {
                    DocumentsContract.createDocument(
                        context.contentResolver, parentDocumentUri, PLAYLIST_MIME_TYPE,
                        "${playlist.name}${dateTimeSuffix(currentDate())}"
                    )
                } catch (e: Exception) {
                    failed.add(playlist)
                    e.printStackTrace()
                    null
                }
                if (childUri != null) {
                    val songs = reader(playlist).allSongs(context)
                    val result = PlaylistManager.create(context, songs, childUri)
                    if (!result) failed.add(playlist)
                }
            }

            if (failed.isNotEmpty()) {
                val message = context.getString(
                    R.string.failed_to_save_playlist,
                    failed.fold("total ${failed.size}: ") { a, b -> "$a, ${b.name}" }
                )
                coroutineToast(context, message)
            }

            _uri.value = null
        }

        private suspend fun duplicatePlaylistsFromMediaStore(context: Context, playlists: List<Playlist>) {
            val names = playlists.map { it.name }
            val songBatches = withContext(Dispatchers.IO) { playlists.map { reader(it).allSongs(context) } }
            duplicatePlaylistViaMediaStore(context, songBatches, names)
        }

        companion object {
            const val MODE_FILE_SAF = 1
            const val MODE_FILE_MEDIASTORE = 2
            const val MODE_DATABASE = 4
            const val MODE_PLAYLISTS_SAF = 6
            const val MODE_PLAYLISTS_MEDIASTORE = 7
        }
    }

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val USER_ACTION = "user_action"
        private const val USER_ACTION_CREATE = 31
        private const val USER_ACTION_DUPLICATE_PLAYLIST = 15
        private const val USER_ACTION_DUPLICATE_PLAYLISTS = 14

        private const val NAME = "name"
        private const val SONGS = "songs"
        private const val PLAYLISTS = "playlist"

        fun create(songs: List<Song>): CreatePlaylistDialog =
            CreatePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putInt(USER_ACTION, USER_ACTION_CREATE)
                    putParcelableArrayList(SONGS, ArrayList(songs))
                }
            }

        fun duplicate(songs: List<Song>, name: String?): CreatePlaylistDialog =
            CreatePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putInt(USER_ACTION, USER_ACTION_DUPLICATE_PLAYLIST)
                    putString(NAME, name)
                    putParcelableArrayList(SONGS, ArrayList(songs))
                }
            }

        fun duplicate(playlists: List<Playlist>): CreatePlaylistDialog =
            CreatePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putInt(USER_ACTION, USER_ACTION_DUPLICATE_PLAYLISTS)
                    putParcelableArrayList(PLAYLISTS, ArrayList(playlists))
                }
            }

    }
}
