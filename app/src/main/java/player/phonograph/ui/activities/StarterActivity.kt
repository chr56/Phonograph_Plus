/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import legacy.phonograph.MediaStoreCompat
import mt.pref.ThemeColor
import org.koin.android.ext.android.get
import org.koin.core.context.GlobalContext
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.appshortcuts.DynamicShortcutManager.Companion.reportShortcutUsed
import player.phonograph.appshortcuts.shortcuttype.LastAddedShortcutType
import player.phonograph.appshortcuts.shortcuttype.ShuffleAllShortcutType
import player.phonograph.appshortcuts.shortcuttype.TopTracksShortcutType
import player.phonograph.model.PlayRequest
import player.phonograph.model.PlayRequest.SongRequest
import player.phonograph.model.PlayRequest.SongsRequest
import player.phonograph.model.Song
import player.phonograph.model.SongClickMode
import player.phonograph.model.SongClickMode.SONG_APPEND_QUEUE
import player.phonograph.model.SongClickMode.SONG_PLAY_NEXT
import player.phonograph.model.SongClickMode.SONG_PLAY_NOW
import player.phonograph.model.SongClickMode.SONG_SINGLE_PLAY
import player.phonograph.model.SongClickMode.modeName
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import player.phonograph.repo.mediastore.processQuery
import player.phonograph.service.MusicService
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.service.queue.executePlayRequest
import player.phonograph.ui.components.viewcreater.buildDialogView
import player.phonograph.ui.components.viewcreater.buttonPanel
import player.phonograph.ui.components.viewcreater.contentPanel
import player.phonograph.ui.components.viewcreater.titlePanel
import player.phonograph.util.reportError
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.DocumentsContractCompat.getDocumentId
import androidx.core.view.setMargins
import androidx.fragment.app.FragmentActivity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout.LayoutParams
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random
import java.io.File

class StarterActivity : AppCompatActivity() {

    private fun debugLog(msg: String) {
        if (BuildConfig.DEBUG) Log.d("Starter", msg)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherIntent = intent
        val extras = launcherIntent.extras

        if (extras != null && extras.getBoolean(EXTRA_SHORTCUT_MODE, false)) {
            debugLog("ShortCut Mode")
            processShortCut(launcherIntent.extras?.getInt(SHORTCUT_TYPE) ?: SHORTCUT_TYPE_NONE)
            finish()
        } else {
            debugLog("Normal Mode")
            if (SDK_INT >= N_MR1) {
                DynamicShortcutManager(this).updateDynamicShortcuts()
            }
            processFrontGroundMode(launcherIntent)
        }
    }

    private fun processFrontGroundMode(intent: Intent) {
        val playRequest = lookupSongsFromIntent(intent)
        if (playRequest == null) {
            Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show()
            gotoMainActivity()
        } else {
            Dialog(this, playRequest) {
                gotoMainActivity()
            }.create().show()
        }
    }


    private fun lookupSongsFromIntent(intent: Intent): PlayRequest? {
        var playRequest: PlayRequest? = null
        // uri first
        playRequest = handleUriPlayRequest(intent)

        // then search
        if (playRequest == null) {
            playRequest = handleSearchRequest(intent)
        }
        // then
        if (playRequest == null) {
            playRequest = handleExtra(intent)
        }

        return playRequest
    }

    private fun handleUriPlayRequest(intent: Intent): PlayRequest? {
        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            val songs = parseUri(uri)
            return PlayRequest.from(songs)
        }
        return null
    }

    private fun parseUri(uri: Uri): List<Song> {

        if (uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority != null) {
            val songId =
                when (uri.authority) {
                    AUTHORITY_MEDIA_PROVIDER -> getDocumentId(uri)!!.split(":")[1]
                    AUTHORITY_MEDIA          -> uri.lastPathSegment
                    else                     -> null
                }
            if (songId != null) {
                return listOf(Songs.id(this, songId.toLong()))
            }
        }

        val file: File? =
            if (uri.authority != null && uri.authority == AUTHORITY_DOCUMENTS_PROVIDER) {
                File(
                    Environment.getExternalStorageDirectory(),
                    uri.path!!.split(Regex("^.*:.*$"), 2)[1]
                )
            } else {
                val path = getFilePathFromUri(this, uri)
                when {
                    path != null     -> File(path)
                    uri.path != null -> File(uri.path!!)
                    else             -> null
                }
            }

        if (file != null) {
            return Songs.searchByPath(this, file.absolutePath, withoutPathFilter = true)
        }

        return emptyList()
    }

    private fun handleSearchRequest(intent: Intent): PlayRequest? {
        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = processQuery(this, intent.extras!!)
                return PlayRequest.from(songs)
            }
        }
        return null
    }

    private fun handleExtra(intent: Intent): PlayRequest? {
        when (intent.type) {
            MediaStoreCompat.Audio.Playlists.CONTENT_TYPE -> {
                val id = parseIdFromIntent(intent, "playlistId", "playlist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = PlaylistSongLoader.getPlaylistSongList(this, id).map { it.song }
                    if (songs.isNotEmpty()) return SongsRequest(songs, position)
                }
            }

            MediaStore.Audio.Albums.CONTENT_TYPE          -> {
                val id = parseIdFromIntent(intent, "albumId", "album")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = Songs.album(this, id)
                    if (songs.isNotEmpty()) return SongsRequest(songs, position)
                }
            }

            MediaStore.Audio.Artists.CONTENT_TYPE         -> {
                val id = parseIdFromIntent(intent, "artistId", "artist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = Songs.artist(this, id)
                    if (songs.isNotEmpty()) return SongsRequest(songs, position)
                }
            }
        }
        return null
    }


    private fun processShortCut(shortcutType: Int) {
        var shuffleMode = ShuffleMode.NONE
        val playlist: SmartPlaylist? = when (shortcutType) {
            SHORTCUT_TYPE_SHUFFLE_ALL -> {
                reportShortcutUsed(this, ShuffleAllShortcutType.id)
                shuffleMode = ShuffleMode.SHUFFLE
                SmartPlaylist.shuffleAllPlaylist

            }

            SHORTCUT_TYPE_TOP_TRACKS  -> {
                reportShortcutUsed(this, TopTracksShortcutType.id)
                SmartPlaylist.myTopTracksPlaylist
            }

            SHORTCUT_TYPE_LAST_ADDED  -> {
                reportShortcutUsed(this, LastAddedShortcutType.id)
                SmartPlaylist.lastAddedPlaylist
            }

            else                      -> null
        }
        val songs = playlist?.getSongs(applicationContext)

        if (!songs.isNullOrEmpty()) {
            val queueManager: QueueManager = get()
            queueManager.swapQueue(
                songs,
                if (shuffleMode == ShuffleMode.SHUFFLE) Random.nextInt(songs.size) else 0,
                false
            )
            queueManager.modifyShuffleMode(shuffleMode, false)
            queueManager.modifyPosition(0, false)
            startService(
                Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY
                }
            )
        } else {
            Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show()
        }
    }

    private fun gotoMainActivity() {
        startActivity(
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    class Dialog(
        private val context: FragmentActivity,
        private val playRequest: PlayRequest,
        private val callback: () -> Unit,
    ) {
        fun getString(id: Int) = context.getString(id)

        private var selected = -1
        fun create(): AlertDialog {

            val ok = { _: View ->
                val queueManager: QueueManager = (context as? AppCompatActivity)?.get() ?: GlobalContext.get().get()
                executePlayRequest(queueManager, playRequest, selected)
                callback()
            }

            val dialogView: View =
                when (playRequest) {
                    is SongsRequest -> {
                        val text = buildString {
                            append("${getString(R.string.action_play)}\n")
                            val songs = playRequest.songs
                            val count = songs.size
                            append("${context.resources.getQuantityString(R.plurals.item_songs, count, count)}\n")
                            songs.take(10).forEach {
                                append("${it.title}\n")
                            }
                            if (count > 10) append("...")
                        }

                        val buttons = SongClickMode.baseModes

                        createDialogView(text, buttons, { selected = it }, ok)

                    }

                    is SongRequest  -> {
                        val text = buildString {
                            append("${getString(R.string.action_play)}\n")
                            append("${playRequest.song.title}\n")
                        }

                        val buttons = intArrayOf(
                            SONG_PLAY_NEXT,
                            SONG_PLAY_NOW,
                            SONG_APPEND_QUEUE,
                            SONG_SINGLE_PLAY
                        )

                        createDialogView(text, buttons, { selected = it }, ok)

                    }

                    else            -> {
                        throw IllegalStateException("No song to play?!")
                    }
                }


            return AlertDialog.Builder(context, androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert)
                .setView(dialogView)
                .setOnCancelListener {
                    context.finish()
                }
                .create()
        }


        private fun createDialogView(
            hint: String,
            modes: IntArray,
            checkCallback: (Int) -> Unit,
            okCallback: (View) -> Unit,
        ): View {
            val primaryColor = ThemeColor.primaryColor(context)

            val titlePanel = titlePanel(context).apply {
                titleView.text = getString(R.string.app_name)
            }
            val contentPanel = contentPanel(context) {
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        // Text
                        TextView(context, null, androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Medium).apply {
                            text = hint
                        }.also {
                            addView(it)
                        }
                        // Buttons
                        RadioGroup(context).apply {
                            for (i in modes.indices) {
                                addView(
                                    RadioButton(context).apply {
                                        this.text = modeName(context.resources, modes[i])
                                        this.setOnClickListener {
                                            checkCallback(modes[i])
                                        }
                                    },
                                    MATCH_PARENT, WRAP_CONTENT
                                )
                            }
                        }.also {
                            addView(it)
                        }
                    },
                    LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(16) }
                )
            }
            val buttonPanel = buttonPanel(context) {
                button(0, getString(android.R.string.cancel), primaryColor) {}
                space(1)
                button(2, getString(android.R.string.ok), primaryColor, okCallback)
            }
            return buildDialogView(
                context, titlePanel, contentPanel, buttonPanel
            )
        }
    }

    companion object {
        const val TAG = "Starter"
        const val AUTHORITY_MEDIA_PROVIDER = "com.android.providers.media.documents"
        const val AUTHORITY_DOCUMENTS_PROVIDER = "com.android.externalstorage.documents"
        const val AUTHORITY_MEDIA = "media"

        fun getFilePathFromUri(context: Context, uri: Uri): String? {
            val column = "_data"
            val projection = arrayOf(column)

            val cursor: Cursor? =
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    null
                )
            try {
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndexOrThrow(column)
                        return it.getString(columnIndex)
                    }
                }
            } catch (e: Exception) {
                reportError(e, TAG, "Failed parse $uri")
            }
            return null
        }

        fun parseIdFromIntent(intent: Intent, longKey: String, stringKey: String): Long {
            var id = intent.getLongExtra(longKey, -1)
            if (id < 0) {
                val idString = intent.getStringExtra(stringKey)
                if (idString != null) {
                    try {
                        id = idString.toLong()
                    } catch (e: NumberFormatException) {
                        Log.w("Starter", e)
                    }
                }
            }
            return id
        }

        const val EXTRA_SHORTCUT_MODE = "player.phonograph.SHORTCUT_MODE"
        const val SHORTCUT_TYPE = "player.phonograph.appshortcuts.ShortcutType"
        const val SHORTCUT_TYPE_SHUFFLE_ALL = 0
        const val SHORTCUT_TYPE_TOP_TRACKS = 1
        const val SHORTCUT_TYPE_LAST_ADDED = 2
        const val SHORTCUT_TYPE_NONE = 3
    }

}