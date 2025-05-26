/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import lib.storage.launcher.SAFActivityResultContracts
import lib.storage.textparser.DocumentUriPathParser.documentTreeUriBasePath
import lib.storage.textparser.DocumentUriPathParser.documentUriBasePath
import player.phonograph.R
import player.phonograph.databinding.DialogCreatePlaylistBinding
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.mechanism.playlist.PlaylistProcessors.reader
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistCreator
import player.phonograph.ui.basis.DialogActivity
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.observe
import player.phonograph.util.parcelableArrayListExtra
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import player.phonograph.util.text.dateTimeSuffixCompat
import player.phonograph.util.ui.getScreenSize
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.content.res.Resources
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

class CreatePlaylistDialogActivity : DialogActivity(),
                                     IOpenFileStorageAccessible,
                                     IOpenDirStorageAccessible,
                                     ICreateFileStorageAccessible {

    private val viewModel: CreatePlaylistViewModel by viewModels<CreatePlaylistViewModel>()

    private var _binding: DialogCreatePlaylistBinding? = null
    private val binding: DialogCreatePlaylistBinding get() = _binding!!

    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()
    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.initArgs(intent, resources)
        super.onCreate(savedInstanceState)
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            createFileStorageAccessDelegate,
        )
        _binding = DialogCreatePlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.attributes = window.attributes.apply {
            width = getScreenSize().x / 7 * 6
        }

        setupMainView()
    }

    private fun setupMainView() {
        bindEvent()
        bindState()
        prefillData()
        with(binding) {
            spinnerContainer.visibility =
                if (viewModel.parameter.userAction == Parameter.USER_ACTION_CREATE) View.VISIBLE else View.GONE
            val options = listOf(
                getString(R.string.label_file_playlists),
                getString(R.string.label_database_playlists),
            )
            val callback: (Int) -> Unit = { position: Int ->
                when (position) {
                    1 -> viewModel.updateMode(CreatePlaylistViewModel.MODE_DATABASE)
                    0 -> viewModel.updateMode(
                        if (viewModel.useSAF) CreatePlaylistViewModel.MODE_FILE_SAF else CreatePlaylistViewModel.MODE_FILE_MEDIASTORE
                    )
                }
            }
            setupSpinner(spinner, options, callback)
        }
    }

    private fun prefillData() {
        with(binding) {
            name.editText?.setText(viewModel.parameter.defaultName)
            location.editText?.setText(null)
            checkBoxSaf.isChecked = true
        }
    }

    private fun bindEvent() {
        binding.buttonCancel.setOnClickListener { finish() }
        binding.buttonCreate.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.execute(this@CreatePlaylistDialogActivity)
            }
            finish()
        }
        binding.name.editText?.addTextChangedListener { editable ->
            viewModel.updateName(editable?.toString())
        }
        binding.location.setEndIconOnClickListener {
            lifecycleScope.launch {
                viewModel.selectUri(this@CreatePlaylistDialogActivity)
            }
        }
        binding.checkBoxSaf.setOnCheckedChangeListener { _, value ->
            viewModel.updateUseSAF(value)
        }
    }

    private fun bindState() {

        observe(viewModel.location, state = Lifecycle.State.STARTED) { path ->
            binding.location.editText?.setText(path)
        }
        observe(viewModel.mode, state = Lifecycle.State.STARTED) { mode ->
            binding.location.visibility =
                if (mode == CreatePlaylistViewModel.MODE_FILE_SAF || mode == CreatePlaylistViewModel.MODE_PLAYLISTS_SAF)
                    View.VISIBLE
                else
                    View.INVISIBLE
            binding.name.visibility =
                if (mode == CreatePlaylistViewModel.MODE_PLAYLISTS_SAF || mode == CreatePlaylistViewModel.MODE_PLAYLISTS_MEDIASTORE)
                    View.INVISIBLE
                else
                    View.VISIBLE
            binding.checkBoxSaf.visibility =
                if (mode == CreatePlaylistViewModel.MODE_DATABASE) View.INVISIBLE else View.VISIBLE
        }

    }

    private fun setupSpinner(
        spinner: AppCompatSpinner,
        options: List<String>,
        onclick: (Int) -> Unit,
    ) {
        spinner.setAdapter(ArrayAdapter(spinner.context, R.layout.item_dropdown, options))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onclick(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    class CreatePlaylistViewModel : ViewModel() {

        lateinit var parameter: Parameter
            private set

        fun initArgs(intent: Intent, resources: Resources) {
            parameter = Parameter.fromLaunchingIntent(intent, resources)
            updateName(parameter.defaultName)
            if (parameter.userAction == Parameter.USER_ACTION_DUPLICATE_PLAYLISTS) updateMode(MODE_PLAYLISTS_SAF)
        }

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
        suspend fun requireUri(context: Context): Uri = uri.value ?: selectUri(context)

        private val _location: MutableStateFlow<String?> = MutableStateFlow(null)
        val location get() = _location.asStateFlow()


        suspend fun selectUri(context: Context): Uri =
            if (mode.value == MODE_PLAYLISTS_SAF || mode.value == MODE_PLAYLISTS_MEDIASTORE) {
                selectDirectory(context)
            } else {
                selectFile(context)
            }

        suspend fun selectFile(context: Context): Uri = withContext(Dispatchers.IO) {
            val documentUri = makeNewFile(context, name.value ?: context.getString(R.string.title_new_playlist))
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

        suspend fun execute(context: Context) =
            withContext(Dispatchers.IO) {
                when (mode.value) {
                    MODE_FILE_SAF             -> createFromSAF(context, parameter.songs, requireUri(context))
                    MODE_FILE_MEDIASTORE      -> createFromMediaStore(context, parameter.songs, name.value)
                    MODE_DATABASE             -> createFromDatabase(context, name.value, parameter.songs)
                    MODE_PLAYLISTS_SAF        -> duplicatePlaylistsFromSAF(context, parameter.playlists)
                    MODE_PLAYLISTS_MEDIASTORE -> duplicatePlaylistsFromMediaStore(context, parameter.playlists)
                    else                      -> throw IllegalStateException("Illegal mode ${mode.value}")
                }
            }

        private suspend fun makeNewFile(context: Context, playlistName: CharSequence): Uri =
            SAFActivityResultContracts.createFileViaSAF(context, "$playlistName.m3u")

        private suspend fun chooseDirectory(context: Context, path: String): Uri =
            SAFActivityResultContracts.chooseDirViaSAF(context, path)

        private suspend fun createFromDatabase(context: Context, name: String?, songs: List<Song>) {
            PlaylistManager.create(songs).intoDatabase(
                context,
                if (name.isNullOrEmpty()) context.getString(R.string.title_new_playlist) else name
            )
        }

        private suspend fun createFromSAF(context: Context, songs: List<Song>, uri: Uri) {
            val result = PlaylistManager.create(songs).fromUri(context, uri)
            val message = when (result) {
                true  -> context.getString(R.string.success)
                false -> context.getString(R.string.failed)
            }
            coroutineToast(context, message)
            _uri.value = null // clear
        }

        private suspend fun createFromMediaStore(context: Context, songs: List<Song>, name: String?) {
            val result = PlaylistManager.create(songs).fromMediaStore(
                context, if (name.isNullOrEmpty()) context.getString(R.string.title_new_playlist) else name
            )
            val message = when (result) {
                PlaylistCreator.RESULT_ERROR   -> context.getString(R.string.failed)
                PlaylistCreator.RESULT_EXISTED -> context.getString(R.string.err_playlist_exists, name)
                else                           -> context.getString(R.string.success)
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
                    val result = PlaylistManager.create(songs).fromUri(context, childUri)
                    if (!result) failed.add(playlist)
                }
            }

            if (failed.isNotEmpty()) {
                val message = context.getString(
                    R.string.err_failed_to_save_playlist,
                    failed.fold("total ${failed.size}: ") { a, b -> "$a, ${b.name}" }
                )
                coroutineToast(context, message)
            }

            _uri.value = null
        }

        private suspend fun duplicatePlaylistsFromMediaStore(context: Context, playlists: List<Playlist>) {
            val timestamp = dateTimeSuffixCompat(currentDate())
            val failures = mutableListOf<Playlist>()
            for (playlist in playlists) {
                val name = "${playlist.name}_$timestamp"
                val songs = reader(playlist).allSongs(context)
                val result = PlaylistManager.create(songs).fromMediaStore(context, name)
                if (result < 0) failures.add(playlist)
            }
            if (failures.isNotEmpty()) {
                warning(context, "Playlist", "Playlists failed to save: $failures")
            }
        }


        companion object {
            const val MODE_FILE_SAF = 1
            const val MODE_FILE_MEDIASTORE = 2
            const val MODE_DATABASE = 4
            const val MODE_PLAYLISTS_SAF = 6
            const val MODE_PLAYLISTS_MEDIASTORE = 7
        }
    }

    class Parameter(
        var userAction: Int = -1,
        var defaultName: String? = null,
        var songs: List<Song> = emptyList(),
        var playlists: List<Playlist> = emptyList(),
    ) {
        companion object {
            const val USER_ACTION = "user_action"
            const val USER_ACTION_CREATE = 31
            const val USER_ACTION_DUPLICATE_PLAYLIST = 15
            const val USER_ACTION_DUPLICATE_PLAYLISTS = 14

            private const val NAME = "name"
            private const val SONGS = "songs"
            private const val PLAYLISTS = "playlist"

            fun fromLaunchingIntent(intent: Intent, resources: Resources): Parameter =
                when (val userAction = intent.getIntExtra(USER_ACTION, 0)) {
                    USER_ACTION_CREATE              -> {
                        val songs = intent.parcelableArrayListExtra<Song>(SONGS) ?: emptyList()
                        Parameter(
                            userAction = userAction,
                            defaultName = resources.getString(R.string.title_new_playlist),
                            songs = songs,
                        )
                    }

                    USER_ACTION_DUPLICATE_PLAYLIST  -> {
                        val songs = intent.parcelableArrayListExtra<Song>(SONGS) ?: emptyList()
                        val defaultName = intent.getStringExtra(NAME)
                        Parameter(
                            userAction = userAction,
                            defaultName = defaultName,
                            songs = songs,
                        )
                    }

                    USER_ACTION_DUPLICATE_PLAYLISTS -> {
                        val playlists = intent.parcelableArrayListExtra<Playlist>(PLAYLISTS) ?: emptyList()
                        Parameter(
                            userAction = userAction,
                            defaultName = resources.getString(R.string.title_new_playlist),
                            playlists = playlists,
                        )
                    }

                    else                            -> throw IllegalStateException("UserAction: $userAction")
                }

            fun buildLaunchingIntentForCreating(
                context: Context,
                songs: List<Song>,
            ): Intent = Intent(context, CreatePlaylistDialogActivity::class.java).apply {
                putExtra(USER_ACTION, USER_ACTION_CREATE)
                putExtra(SONGS, ArrayList(songs))
            }

            fun buildLaunchingIntentForDuplicate(
                context: Context,
                songs: List<Song>, name: String?,
            ): Intent = Intent(context, CreatePlaylistDialogActivity::class.java).apply {
                putExtra(USER_ACTION, USER_ACTION_DUPLICATE_PLAYLIST)
                putExtra(NAME, name)
                putParcelableArrayListExtra(SONGS, ArrayList(songs))
            }

            fun buildLaunchingIntentForDuplicate(
                context: Context,
                playlists: List<Playlist>,
            ): Intent = Intent(context, CreatePlaylistDialogActivity::class.java).apply {
                putExtra(USER_ACTION, USER_ACTION_DUPLICATE_PLAYLISTS)
                putParcelableArrayListExtra(PLAYLISTS, ArrayList(playlists))
            }
        }
    }

}