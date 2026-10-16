/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.actions

import player.phonograph.foundation.testBit
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.SongClickMode.FLAG_MASK_GOTO_POSITION_FIRST
import player.phonograph.model.SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY
import player.phonograph.model.SongClickMode.QUEUE_APPEND_QUEUE
import player.phonograph.model.SongClickMode.QUEUE_PLAY_NEXT
import player.phonograph.model.SongClickMode.QUEUE_PLAY_NOW
import player.phonograph.model.SongClickMode.QUEUE_SHUFFLE
import player.phonograph.model.SongClickMode.QUEUE_SWITCH_TO_BEGINNING
import player.phonograph.model.SongClickMode.QUEUE_SWITCH_TO_POSITION
import player.phonograph.model.SongClickMode.SONG_APPEND_QUEUE
import player.phonograph.model.SongClickMode.SONG_PLAY_NEXT
import player.phonograph.model.SongClickMode.SONG_PLAY_NOW
import player.phonograph.model.SongClickMode.SONG_SINGLE_PLAY
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Songs
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.goToAlbumDetail
import player.phonograph.ui.goToArtistDetail
import player.phonograph.ui.goToGenreDetail
import player.phonograph.ui.goToPlaylistDetail
import player.phonograph.ui.resource.ActionSemantics
import android.content.Context
import kotlin.random.Random

@Suppress("FunctionName")
object ItemClickActions {

    private fun entry(
        semantic: ActionSemantic,
        action: suspend () -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(
        semantic = semantic,
        visibility = ActionEntry.ToolbarEntry.NEVER,
        action = action,
    )

    fun Song(context: Context, item: Song, position: Int, list: List<Song>): ActionEntry =
        entry(ActionSemantics.Play) {
            val settings = Settings(context)
            val base = settings[Keys.songItemClickMode].data
            val extra = settings[Keys.songItemClickExtraFlag].data
            songClick(context, item, list, position, base, extra)
        }

    fun Album(context: Context, item: Album): ActionEntry =
        entry(ActionSemantics.GoToAlbum) { goToAlbumDetail(context, item.id) }

    fun Artist(context: Context, item: Artist): ActionEntry =
        entry(ActionSemantics.GoToArtist) { goToArtistDetail(context, item.id) }

    fun Playlist(context: Context, item: Playlist): ActionEntry =
        entry(ActionSemantics.Details) { goToPlaylistDetail(context, item) }

    fun Genre(context: Context, item: Genre): ActionEntry =
        entry(ActionSemantics.Details) { goToGenreDetail(context, item) }

    fun PlayingQueue(position: Int): ActionEntry =
        entry(ActionSemantics.Play) { MusicPlayerRemote.playSongAt(position) }

    fun Folder(item: FileItem, onSwitch: (FileItem) -> Unit): ActionEntry =
        entry(ActionSemantics.Select) { onSwitch(item) }

    fun File(context: Context, item: FileItem, position: Int, list: List<FileItem>): ActionEntry =
        entry(ActionSemantics.Play) {
            val settings = Settings(context)
            val base = settings[Keys.songItemClickMode].data
            val extra = settings[Keys.songItemClickExtraFlag].data
            fileClick(context, item, list, position, base, extra)
        }

    private fun songClick(
        context: Context,
        item: Song,
        list: List<Song>,
        position: Int,
        baseMode: Int,
        extraFlag: Int,
    ) {
        var base = baseMode

        // pre-process extra mode
        if (MusicPlayerRemote.playingQueue.isEmpty() && extraFlag.testBit(FLAG_MASK_PLAY_QUEUE_IF_EMPTY)) {
            if (base in 100..109) {
                base += 100
            } else {
                base = QUEUE_SWITCH_TO_POSITION
            }
        }

        if (extraFlag.testBit(FLAG_MASK_GOTO_POSITION_FIRST) && list == MusicPlayerRemote.playingQueue) {
            // same queue, jump
            MusicPlayerRemote.playSongAt(position)
            return
        }

        // base mode
        when (base) {
            SONG_PLAY_NEXT            -> item.actionPlayNext()
            SONG_PLAY_NOW             -> item.actionPlayNow()
            SONG_APPEND_QUEUE         -> item.actionEnqueue()
            SONG_SINGLE_PLAY          -> listOf(item).actionPlay(null, 0)
            QUEUE_PLAY_NOW            -> list.actionPlayNow()
            QUEUE_PLAY_NEXT           -> list.actionPlayNext()
            QUEUE_APPEND_QUEUE        -> list.actionEnqueue()
            QUEUE_SWITCH_TO_BEGINNING -> list.actionPlay(ShuffleMode.NONE, 0)
            QUEUE_SWITCH_TO_POSITION  -> list.actionPlay(ShuffleMode.NONE, position)
            QUEUE_SHUFFLE             -> list.actionPlay(
                ShuffleMode.SHUFFLE,
                Random.nextInt(list.size)
            )

            else  /* invalided */     -> {
                Settings(context)[Keys.songItemClickMode].data = SONG_PLAY_NOW // reset base mode
            }
        }
    }

    /**
     * @param list entire list including folder
     * @param position in-list position
     */
    private suspend fun fileClick(
        context: Context,
        item: FileItem,
        list: List<FileItem>,
        position: Int,
        baseMode: Int,
        extraFlag: Int,
    ) {
        var base = baseMode

        // pre-process extra mode
        if (MusicPlayerRemote.playingQueue.isEmpty() && extraFlag.testBit(FLAG_MASK_PLAY_QUEUE_IF_EMPTY)) {
            if (base in 100..109) {
                base += 100
            } else {
                base = QUEUE_SWITCH_TO_POSITION
            }
        }

        if (extraFlag.testBit(FLAG_MASK_GOTO_POSITION_FIRST)) {
            val songRequest = filter(list, position)
            if (songRequest.songs == MusicPlayerRemote.playingQueue) {
                // same queue, jump
                MusicPlayerRemote.playSongAt(songRequest.position)
                return
            }
        }

        when (base) {
            SONG_PLAY_NEXT,
            SONG_PLAY_NOW,
            SONG_APPEND_QUEUE,
            SONG_SINGLE_PLAY,
                                  -> {
                val song = when (item.content) {
                    FileItem.MediaContent   -> Songs.path(context, item.path)
                    is FileItem.SongContent -> item.content.song
                    else                    -> null
                } ?: return
                when (base) {
                    SONG_PLAY_NEXT    -> song.actionPlayNext()
                    SONG_PLAY_NOW     -> song.actionPlayNow()
                    SONG_APPEND_QUEUE -> song.actionEnqueue()
                    SONG_SINGLE_PLAY  -> listOf(song).actionPlay(null, 0)
                }
            }

            QUEUE_PLAY_NOW,
            QUEUE_PLAY_NEXT,
            QUEUE_APPEND_QUEUE,
            QUEUE_SWITCH_TO_BEGINNING,
            QUEUE_SWITCH_TO_POSITION,
            QUEUE_SHUFFLE,
                                  -> {
                val songRequest = filter(list, position)
                val songs = songRequest.songs
                val actualPosition = songRequest.position

                when (base) {
                    QUEUE_PLAY_NOW            -> songs.actionPlayNow()
                    QUEUE_PLAY_NEXT           -> songs.actionPlayNext()
                    QUEUE_APPEND_QUEUE        -> songs.actionEnqueue()
                    QUEUE_SWITCH_TO_BEGINNING -> songs.actionPlay(ShuffleMode.NONE, 0)
                    QUEUE_SWITCH_TO_POSITION  -> songs.actionPlay(ShuffleMode.NONE, actualPosition)
                    QUEUE_SHUFFLE             ->
                        if (songs.isNotEmpty()) songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
                }
            }

            else  /* invalided */ -> {
                Settings(context)[Keys.songItemClickMode].data = SONG_PLAY_NOW // reset base mode
            }
        }
    }

    /** filter folders and relocate position */
    private fun filter(list: List<FileItem>, clickPosition: Int): PlayRequest.SongsRequest {
        var actualPosition: Int = clickPosition
        val actualSongList = mutableListOf<Song>()
        for ((index, item) in list.withIndex()) {
            if (item.content is FileItem.SongContent) {
                actualSongList.add(item.content.song)
            } else {
                if (index < clickPosition) actualPosition--
            }
        }
        return PlayRequest.SongsRequest(actualSongList, actualPosition)
    }
}