/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.resource

import lib.storage.textparser.ExternalFilePathParser
import player.phonograph.R
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
import player.phonograph.model.backup.BackupItem
import player.phonograph.model.coil.IMAGE_SOURCE_EXTERNAL_FILE
import player.phonograph.model.coil.IMAGE_SOURCE_J_AUDIO_TAGGER
import player.phonograph.model.coil.IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER
import player.phonograph.model.coil.IMAGE_SOURCE_MEDIA_STORE
import player.phonograph.model.lyrics.LyricsSource
import player.phonograph.model.lyrics.LyricsSource.Embedded
import player.phonograph.model.lyrics.LyricsSource.ExternalDecorated
import player.phonograph.model.lyrics.LyricsSource.ExternalPrecise
import player.phonograph.model.lyrics.LyricsSource.ManuallyLoaded
import player.phonograph.model.lyrics.LyricsSource.Unknown
import player.phonograph.model.metadata.AudioProperties
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.FileProperties
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.notification.NotificationAction
import player.phonograph.model.pages.HomePage
import player.phonograph.model.pages.PAGE_ALBUM
import player.phonograph.model.pages.PAGE_ARTIST
import player.phonograph.model.pages.PAGE_EMPTY
import player.phonograph.model.pages.PAGE_FILES
import player.phonograph.model.pages.PAGE_FOLDER
import player.phonograph.model.pages.PAGE_GENRE
import player.phonograph.model.pages.PAGE_PLAYLIST
import player.phonograph.model.pages.PAGE_SONG
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_FILE
import player.phonograph.model.playlist.PLAYLIST_TYPE_HISTORY
import player.phonograph.model.playlist.PLAYLIST_TYPE_LAST_ADDED
import player.phonograph.model.playlist.PLAYLIST_TYPE_MY_TOP_TRACK
import player.phonograph.model.playlist.PLAYLIST_TYPE_RANDOM
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.playlist.PlaylistType
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.TimeUnit
import android.Manifest
import android.content.Context
import android.content.pm.PermissionInfo
import android.content.res.Resources


object Texts {

    fun page(resources: Resources, @HomePage page: String?): String = when (page) {
        PAGE_SONG     -> resources.getString(R.string.label_songs)
        PAGE_ALBUM    -> resources.getString(R.string.label_albums)
        PAGE_ARTIST   -> resources.getString(R.string.label_artists)
        PAGE_PLAYLIST -> resources.getString(R.string.label_playlists)
        PAGE_GENRE    -> resources.getString(R.string.label_genres)
        PAGE_FOLDER   -> resources.getString(R.string.label_folders)
        PAGE_FILES    -> resources.getString(R.string.label_files)
        PAGE_EMPTY    -> resources.getString(R.string.msg_empty)
        else          -> "UNKNOWN"
    }

    fun playlistType(resources: Resources, @PlaylistType type: Int): String = when (type) {
        PLAYLIST_TYPE_FILE         -> resources.getString(R.string.label_file)
        PLAYLIST_TYPE_FAVORITE     -> resources.getString(R.string.playlist_favorites)
        PLAYLIST_TYPE_LAST_ADDED   -> resources.getString(R.string.playlist_last_added)
        PLAYLIST_TYPE_HISTORY      -> resources.getString(R.string.playlist_history)
        PLAYLIST_TYPE_MY_TOP_TRACK -> resources.getString(R.string.playlist_my_top_tracks)
        PLAYLIST_TYPE_RANDOM       -> resources.getString(R.string.action_shuffle_all)
        else                       -> resources.getString(R.string.label_playlists)
    }

    fun playlist(resources: Resources, location: PlaylistLocation): String = when (location) {
        is DatabasePlaylistLocation        -> "#${location.id()}"
        is FilePlaylistLocation            -> ExternalFilePathParser.bashPath(location.path) ?: location.path
        VirtualPlaylistLocation.Favorite   -> resources.getString(R.string.playlist_favorites)
        VirtualPlaylistLocation.LastAdded  -> resources.getString(R.string.playlist_last_added)
        VirtualPlaylistLocation.History    -> resources.getString(R.string.playlist_history)
        VirtualPlaylistLocation.MyTopTrack -> resources.getString(R.string.playlist_my_top_tracks)
        VirtualPlaylistLocation.Random     -> resources.getString(R.string.action_shuffle_all)
    }

    fun songClickMode(resources: Resources, id: Int): String = when (id) {
        SONG_PLAY_NEXT            -> resources.getString(R.string.mode_song_play_next)
        SONG_PLAY_NOW             -> resources.getString(R.string.mode_song_play_now)
        SONG_APPEND_QUEUE         -> resources.getString(R.string.mode_song_append_queue)
        SONG_SINGLE_PLAY          -> resources.getString(R.string.mode_song_single_play)
        QUEUE_PLAY_NEXT           -> resources.getString(R.string.mode_queue_play_next)
        QUEUE_PLAY_NOW            -> resources.getString(R.string.mode_queue_play_now)
        QUEUE_APPEND_QUEUE        -> resources.getString(R.string.mode_queue_append_queue)
        QUEUE_SWITCH_TO_BEGINNING -> resources.getString(R.string.mode_queue_switch_to_beginning)
        QUEUE_SWITCH_TO_POSITION  -> resources.getString(R.string.mode_queue_switch_to_position)
        QUEUE_SHUFFLE             -> resources.getString(R.string.mode_queue_shuffle)
        else                      -> "UNKNOWN MODE $id"
    }

    fun imageSource(resources: Resources, key: String): String = resources.getString(
        when (key) {
            IMAGE_SOURCE_MEDIA_STORE              -> R.string.image_source_media_store
            IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER -> R.string.image_source_media_metadata_retriever
            IMAGE_SOURCE_J_AUDIO_TAGGER           -> R.string.image_source_jaudio_tagger
            IMAGE_SOURCE_EXTERNAL_FILE            -> R.string.image_source_external_file
            else                                  -> throw IllegalStateException("Unknown ImageSource: $key")
        }
    )

    fun lyricsSource(resources: Resources, item: LyricsSource): String = when (item) {
        Embedded                           -> resources.getString(R.string.label_embedded_lyrics)
        ExternalDecorated, ExternalPrecise -> resources.getString(R.string.label_external_lyrics)
        ManuallyLoaded                     -> resources.getString(R.string.label_loaded)
        Unknown                            -> "N/A"
    }

    fun backupItem(resources: Resources, backupItem: BackupItem): CharSequence = with(resources) {
        when (backupItem) {
            BackupItem.Settings              -> getString(R.string.action_settings)
            BackupItem.PathFilter            -> getString(R.string.path_filter)
            BackupItem.Favorites             -> getString(R.string.playlist_favorites)
            BackupItem.PlayingQueues         -> getString(R.string.label_playing_queue)
            BackupItem.InternalPlaylists     -> getString(R.string.label_database_playlists)
            BackupItem.MainDatabase          -> "[${getString(R.string.label_databases)}] ${getString(R.string.pref_header_library)}"
            BackupItem.FavoriteDatabase      -> "[${getString(R.string.label_databases)}] ${getString(R.string.playlist_favorites)}"
            BackupItem.PathFilterDatabase    -> "[${getString(R.string.label_databases)}] ${getString(R.string.path_filter)}"
            BackupItem.HistoryDatabase       -> "[${getString(R.string.label_databases)}] ${getString(R.string.playlist_history)}"
            BackupItem.SongPlayCountDatabase -> "[${getString(R.string.label_databases)}] ${getString(R.string.playlist_my_top_tracks)}"
            BackupItem.PlayingQueuesDatabase -> "[${getString(R.string.label_databases)}] ${getString(R.string.label_playing_queue)}"
        }
    }

    fun timeUnit(resources: Resources, timeUnit: TimeUnit) = when (timeUnit) {
        TimeUnit.Year   -> resources.getString(R.string.timeunit_year)
        TimeUnit.Month  -> resources.getString(R.string.timeunit_month)
        TimeUnit.Week   -> resources.getString(R.string.timeunit_week)
        TimeUnit.Day    -> resources.getString(R.string.timeunit_day)
        TimeUnit.Hour   -> resources.getString(R.string.timeunit_hour)
        TimeUnit.Minute -> resources.getString(R.string.timeunit_minute)
        TimeUnit.Second -> resources.getString(R.string.timeunit_second)
    }

    fun timeIntervalCalculationMode(resources: Resources, item: TimeIntervalCalculationMode) = when (item) {
        TimeIntervalCalculationMode.PAST   -> resources.getString(R.string.interval_past)
        TimeIntervalCalculationMode.RECENT -> resources.getString(R.string.interval_recent)
        TimeIntervalCalculationMode.EVERY  -> resources.getString(R.string.interval_every)

    }

    fun duration(resources: Resources, duration: Duration, interval: TimeIntervalCalculationMode): String =
        resources.getString(
            R.string.time_interval_text,
            timeIntervalCalculationMode(resources, interval),
            duration.value,
            timeUnit(resources, duration.unit)
        )


    fun notificationAction(resources: Resources, notification: NotificationAction) =
        when (notification) {
            NotificationAction.PlayPause   -> resources.getString(R.string.action_play_pause)
            NotificationAction.Prev        -> resources.getString(R.string.action_previous)
            NotificationAction.Next        -> resources.getString(R.string.action_next)
            NotificationAction.Repeat      -> resources.getString(R.string.action_repeat_mode)
            NotificationAction.Shuffle     -> resources.getString(R.string.action_shuffle_mode)
            NotificationAction.FastForward -> resources.getString(R.string.action_fast_forward)
            NotificationAction.FastRewind  -> resources.getString(R.string.action_fast_rewind)
            NotificationAction.Fav         -> resources.getString(R.string.playlist_favorites)
            NotificationAction.Close       -> resources.getString(R.string.action_exit)
            NotificationAction.Invalid     -> resources.getString(R.string.msg_unknown)
        }

    fun metadataKey(resources: Resources, key: Metadata.Key): String = when (key) {
        AudioProperties.Key.AudioFormat  -> resources.getString(R.string.label_file_format)
        AudioProperties.Key.BitRate      -> resources.getString(R.string.label_bit_rate)
        AudioProperties.Key.SamplingRate -> resources.getString(R.string.label_sampling_rate)
        AudioProperties.Key.TrackLength  -> resources.getString(R.string.label_track_length)
        FileProperties.Key.DateAdded     -> resources.getString(R.string.label_created_at)
        FileProperties.Key.DateModified  -> resources.getString(R.string.label_last_modified_at)
        FileProperties.Key.Name          -> resources.getString(R.string.label_file_name)
        FileProperties.Key.Path          -> resources.getString(R.string.label_file_path)
        FileProperties.Key.Size          -> resources.getString(R.string.label_file_size)
        ConventionalMusicMetadataKey     -> metadataTagKey(resources, key as ConventionalMusicMetadataKey)
        else                             -> key.toString()
    }

    fun metadataTagKey(resources: Resources, key: ConventionalMusicMetadataKey): String = when (key) {
        ConventionalMusicMetadataKey.ALBUM        -> resources.getString(R.string.label_album)
        ConventionalMusicMetadataKey.ALBUM_ARTIST -> resources.getString(R.string.label_album_artist)
        ConventionalMusicMetadataKey.ARTIST       -> resources.getString(R.string.label_artist)
        ConventionalMusicMetadataKey.COMMENT      -> resources.getString(R.string.label_comment)
        ConventionalMusicMetadataKey.COMPOSER     -> resources.getString(R.string.label_composer)
        ConventionalMusicMetadataKey.DISC_NO      -> resources.getString(R.string.label_disk_number)
        ConventionalMusicMetadataKey.DISC_TOTAL   -> resources.getString(R.string.label_disk_number_total)
        ConventionalMusicMetadataKey.GENRE        -> resources.getString(R.string.label_genre)
        ConventionalMusicMetadataKey.LYRICIST     -> resources.getString(R.string.label_lyricist)
        ConventionalMusicMetadataKey.LYRICS       -> resources.getString(R.string.label_lyrics)
        ConventionalMusicMetadataKey.RATING       -> resources.getString(R.string.label_rating)
        ConventionalMusicMetadataKey.TITLE        -> resources.getString(R.string.label_title)
        ConventionalMusicMetadataKey.TRACK        -> resources.getString(R.string.label_track)
        ConventionalMusicMetadataKey.TRACK_TOTAL  -> resources.getString(R.string.label_track_total)
        ConventionalMusicMetadataKey.YEAR         -> resources.getString(R.string.label_year)
        else                                      -> key.name
    }

    private fun permissionInfo(context: Context, permissionId: String): PermissionInfo =
        context.packageManager.getPermissionInfo(permissionId, 0)

    fun permissionName(context: Context, permissionId: String): CharSequence {
        val stringRes = when (permissionId) {
            Manifest.permission.POST_NOTIFICATIONS      -> R.string.permission_name_post_notifications
            Manifest.permission.READ_MEDIA_AUDIO        -> R.string.permission_name_read_media_audio
            Manifest.permission.READ_EXTERNAL_STORAGE   -> R.string.permission_name_read_external_storage
            Manifest.permission.WRITE_EXTERNAL_STORAGE  -> R.string.permission_name_write_external_storage
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> R.string.permission_name_manage_external_storage
            else                                        -> 0
        }
        return if (stringRes > 0) {
            context.getString(stringRes)
        } else {
            permissionInfo(context, permissionId).loadLabel(context.packageManager)
        }
    }

    fun permissionDescription(context: Context, permissionId: String): CharSequence? {
        val stringRes = when (permissionId) {
            Manifest.permission.POST_NOTIFICATIONS      -> R.string.permission_desc_post_notifications
            Manifest.permission.READ_MEDIA_AUDIO        -> R.string.permission_desc_read_media_audio
            Manifest.permission.READ_EXTERNAL_STORAGE   -> R.string.permission_desc_read_external_storage
            Manifest.permission.WRITE_EXTERNAL_STORAGE  -> R.string.permission_desc_write_external_storage
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> R.string.permission_desc_manage_external_storage
            else                                        -> 0
        }
        return if (stringRes > 0) {
            context.getString(stringRes)
        } else {
            permissionInfo(context, permissionId).loadDescription(context.packageManager)
        }
    }

}