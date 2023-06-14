/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import legacy.phonograph.MediaStoreCompat
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.actions.click.mode.SongClickMode
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_APPEND_QUEUE
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_PLAY_NEXT
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_PLAY_NOW
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_SHUFFLE
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_SWITCH_TO_BEGINNING
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_SWITCH_TO_POSITION
import player.phonograph.actions.click.mode.SongClickMode.SONG_APPEND_QUEUE
import player.phonograph.actions.click.mode.SongClickMode.SONG_PLAY_NEXT
import player.phonograph.actions.click.mode.SongClickMode.SONG_PLAY_NOW
import player.phonograph.actions.click.mode.SongClickMode.SONG_SINGLE_PLAY
import player.phonograph.actions.click.mode.SongClickMode.modeName
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.appshortcuts.DynamicShortcutManager.Companion.reportShortcutUsed
import player.phonograph.appshortcuts.shortcuttype.LastAddedShortcutType
import player.phonograph.appshortcuts.shortcuttype.ShuffleAllShortcutType
import player.phonograph.appshortcuts.shortcuttype.TopTracksShortcutType
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.mediastore.SongLoader
import player.phonograph.mediastore.getSongs
import player.phonograph.mediastore.processQuery
import player.phonograph.mediastore.querySongs
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.ShuffleAllPlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicService
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.components.viewcreater.buildDialogView
import player.phonograph.ui.components.viewcreater.buttonPanel
import player.phonograph.ui.components.viewcreater.contentPanel
import player.phonograph.ui.components.viewcreater.titlePanel
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.DocumentsContractCompat.getDocumentId
import androidx.core.view.setMargins
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
            if (songs != null) return PlayRequest(songs, 0)
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun parseUri(uri: Uri): List<Song>? {
        var songs: List<Song>? = null

        if (uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority != null) {
            val songId =
                when (uri.authority) {
                    AUTHORITY_MEDIA_PROVIDER -> getDocumentId(uri)!!.split(":")[1]
                    AUTHORITY_MEDIA          -> uri.lastPathSegment
                    else                     -> null
                }
            if (songId != null) {
                songs = listOf(SongLoader.getSong(this, songId.toLong()))
            }
        }

        if (songs == null) {
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
                songs =
                    querySongs(
                        this,
                        "${MediaStore.Audio.AudioColumns.DATA}=?",
                        arrayOf(file.absolutePath)
                    ).getSongs()
            }
        }

        return songs
    }

    private fun handleSearchRequest(intent: Intent): PlayRequest? {
        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = processQuery(this, intent.extras!!)
                if (songs.isNotEmpty()) return PlayRequest(songs, 0)
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
                    val songs = PlaylistSongLoader.getPlaylistSongList(this, id)
                    if (songs.isNotEmpty()) return PlayRequest(songs, position)
                }
            }

            MediaStore.Audio.Albums.CONTENT_TYPE          -> {
                val id = parseIdFromIntent(intent, "albumId", "album")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = AlbumLoader.getAlbum(this, id).songs
                    if (songs.isNotEmpty()) return PlayRequest(songs, position)
                }
            }

            MediaStore.Audio.Artists.CONTENT_TYPE         -> {
                val id = parseIdFromIntent(intent, "artistId", "artist")
                if (id >= 0) {
                    val position = intent.getIntExtra("position", 0)
                    val songs = ArtistLoader.getArtist(this, id).songs
                    if (songs.isNotEmpty()) return PlayRequest(songs, position)
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
                ShuffleAllPlaylist(applicationContext)

            }

            SHORTCUT_TYPE_TOP_TRACKS  -> {
                reportShortcutUsed(this, TopTracksShortcutType.id)
                MyTopTracksPlaylist(applicationContext)

            }

            SHORTCUT_TYPE_LAST_ADDED  -> {
                reportShortcutUsed(this, LastAddedShortcutType.id)
                LastAddedPlaylist(applicationContext)
            }

            else                      -> null
        }
        val songs = playlist?.getSongs(applicationContext)

        if (songs != null) {
            val queueManager = App.instance.queueManager
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
        private val context: Context,
        private val playRequest: PlayRequest,
        private val callback: () -> Unit,
    ) {
        fun getString(id: Int) = context.getString(id)

        private var selected = -1
        fun create(): AlertDialog {

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
            val list = playRequest.songs
            val targetPosition = playRequest.position

            val song = list[targetPosition]

            val ok = { _: View ->
                val queueManager = App.instance.queueManager
                val currentPosition = queueManager.currentSongPosition
                when (selected) {
                    SONG_PLAY_NEXT            -> queueManager.addSong(song, currentPosition + 1)
                    SONG_PLAY_NOW             -> queueManager.addSong(song, currentPosition)
                    SONG_APPEND_QUEUE         -> queueManager.addSong(song)
                    SONG_SINGLE_PLAY          -> queueManager.swapQueue(listOf(song), 0, false)
                    QUEUE_PLAY_NOW            -> queueManager.addSongs(list, currentPosition)
                    QUEUE_PLAY_NEXT           -> queueManager.addSongs(list, currentPosition + 1)
                    QUEUE_APPEND_QUEUE        -> queueManager.addSongs(list)
                    QUEUE_SWITCH_TO_BEGINNING -> queueManager.swapQueue(list, 0, false)
                    QUEUE_SWITCH_TO_POSITION  -> queueManager.swapQueue(list, targetPosition, false)
                    QUEUE_SHUFFLE             -> {
                        queueManager.swapQueue(list, 0, false)
                        queueManager.modifyShuffleMode(ShuffleMode.SHUFFLE, false)
                    }

                    else  /* invalided */     -> {}
                }
                queueManager.modifyPosition(0, false)
                callback()
            }

            val dialogView = createDialogView(text, buttons, { selected = it }, ok)
            return AlertDialog.Builder(context, androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert)
                .setView(dialogView).create()
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
                        TextView(context).apply {
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
                ErrorNotification.postErrorNotification(e)
                e.printStackTrace()
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