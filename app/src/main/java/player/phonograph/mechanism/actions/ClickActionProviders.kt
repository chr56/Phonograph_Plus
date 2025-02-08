/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.actions

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.SongClickMode
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
import player.phonograph.model.file.FileEntity
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Songs
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.NavigationUtil
import player.phonograph.util.testBit
import androidx.core.util.Pair
import android.content.Context
import android.view.View
import android.widget.ImageView
import kotlin.random.Random
import kotlinx.coroutines.runBlocking

object ClickActionProviders {

    object EmptyClickActionProvider : ClickActionProvider<Any> {
        override fun listClick(
            list: List<Any>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean = true
    }

    class SongClickActionProvider : ClickActionProvider<Song> {
        override fun listClick(
            list: List<Song>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            val setting = Setting(context)
            val base = setting[Keys.songItemClickMode].data
            val extra = setting[Keys.songItemClickExtraFlag].data
            return songClick(context, list, position, base, extra)
        }

        private fun songClick(
            context: Context,
            list: List<Song>,
            position: Int,
            baseMode: Int,
            extraFlag: Int,
        ): Boolean {
            var base = baseMode

            // pre-process extra mode
            if (MusicPlayerRemote.playingQueue.isEmpty() && extraFlag.testBit(FLAG_MASK_PLAY_QUEUE_IF_EMPTY)) {
                if (base in 100..109) {
                    base += 100
                } else {
                    base = SongClickMode.QUEUE_SWITCH_TO_POSITION
                }
            }

            if (extraFlag.testBit(FLAG_MASK_GOTO_POSITION_FIRST) && list == MusicPlayerRemote.playingQueue) {
                // same queue, jump
                MusicPlayerRemote.playSongAt(position)
                return true
            }


            // base mode
            when (base) {
                SONG_PLAY_NEXT            -> list[position].actionPlayNext()
                SONG_PLAY_NOW             -> list[position].actionPlayNow()
                SONG_APPEND_QUEUE         -> list[position].actionEnqueue()
                SONG_SINGLE_PLAY          -> listOf(list[position]).actionPlay(null, 0)
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
                    Setting(context)[Keys.songItemClickMode].data = SONG_PLAY_NOW // reset base mode
                    return false
                }
            }
            return true
        }

    }

    class AlbumClickActionProvider : ClickActionProvider<Album> {
        override fun listClick(
            list: List<Album>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            if (imageView != null) {
                NavigationUtil.goToAlbum(
                    context,
                    list[position].id,
                    Pair(
                        imageView,
                        imageView.resources.getString(R.string.transition_album_art)
                    )
                )
            } else {
                NavigationUtil.goToAlbum(
                    context,
                    list[position].id
                )
            }
            return true
        }

    }

    class ArtistClickActionProvider : ClickActionProvider<Artist> {
        override fun listClick(
            list: List<Artist>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            val artist = list[position]
            val sharedElements: Array<Pair<View, String>>? =
                imageView?.let { arrayOf(Pair(it, context.resources.getString(R.string.transition_artist_image))) }
            NavigationUtil.goToArtist(context, artist.id, sharedElements)
            return true
        }

    }

    class PlaylistClickActionProvider : ClickActionProvider<Playlist> {
        override fun listClick(
            list: List<Playlist>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            NavigationUtil.goToPlaylist(context, list[position])
            return true
        }
    }

    class GenreClickActionProvider : ClickActionProvider<Genre> {
        override fun listClick(
            list: List<Genre>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            NavigationUtil.goToGenre(context, list[position])
            return true
        }

    }

    class FileEntityClickActionProvider : ClickActionProvider<FileEntity> {
        override fun listClick(
            list: List<FileEntity>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean {
            val setting = Setting(context)
            val base = setting[Keys.songItemClickMode].data
            val extra = setting[Keys.songItemClickExtraFlag].data
            return runBlocking { fileClick(context, list, position, base, extra) }
        }

        /**
         * @param list entire list including folder
         * @param position in-list position
         */
        private suspend fun fileClick(
            context: Context,
            list: List<FileEntity>,
            position: Int,
            baseMode: Int,
            extraFlag: Int,
        ): Boolean {
            var base = baseMode
            val songRequest = filter(list, position, context)

            // pre-process extra mode
            if (MusicPlayerRemote.playingQueue.isEmpty() && extraFlag.testBit(FLAG_MASK_PLAY_QUEUE_IF_EMPTY)) {
                if (base in 100..109) {
                    base += 100
                } else {
                    base = QUEUE_SWITCH_TO_POSITION
                }
            }

            if (extraFlag.testBit(FLAG_MASK_GOTO_POSITION_FIRST) && songRequest.songs == MusicPlayerRemote.playingQueue) {
                // same queue, jump
                MusicPlayerRemote.playSongAt(songRequest.position)
                return true
            }

            when (base) {
                SONG_PLAY_NEXT,
                SONG_PLAY_NOW,
                SONG_APPEND_QUEUE,
                SONG_SINGLE_PLAY,
                                      -> {
                    val fileEntity = list[position] as? FileEntity.File ?: return false
                    val song = Songs.searchByFileEntity(context, fileEntity) ?: return false
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
                    Setting(context)[Keys.songItemClickMode].data = SONG_PLAY_NOW // reset base mode
                    return false
                }
            }
            return true
        }

        /**
         * filter folders and relocate position
         */
        private suspend fun filter(list: List<FileEntity>, position: Int, context: Context): PlayRequest.SongsRequest {
            var actualPosition: Int = position
            val actualFileList = ArrayList<Song>(position)
            for ((index, item) in list.withIndex()) {
                if (item is FileEntity.File) {
                    val entity = Songs.searchByFileEntity(context, item)
                    if (entity != null) {
                        actualFileList.add(entity)
                    }
                } else {
                    if (index < position) actualPosition--
                }
            }
            return PlayRequest.SongsRequest(actualFileList, actualPosition)
        }
    }

    interface ClickActionProvider<T> {
        /**
         * involve item click
         * @param list      a list that this Displayable is among
         * @param position  position where selected
         * @param context  relative context
         * @param imageView (optional) item's imagine for SceneTransitionAnimation
         * @return true if action have been processed
         */
        fun listClick(
            list: List<T>,
            position: Int,
            context: Context,
            imageView: ImageView?,
        ): Boolean
    }
}
