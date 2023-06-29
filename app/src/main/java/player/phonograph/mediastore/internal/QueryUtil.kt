/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mediastore.internal

import legacy.phonograph.MediaStoreCompat
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns
import player.phonograph.settings.Setting


/**
 * [queryAudio] but
 * using [BASE_SONG_PROJECTION]
 */
fun querySongs(
    context: Context,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    sortOrder: String? = Setting.instance.songSortMode.SQLQuerySortOrder,
): Cursor? = queryAudio(
    context,
    BASE_SONG_PROJECTION,
    selection,
    selectionValues,
    sortOrder
)

/**
 * [queryAudio] but
 * using [BASE_FILE_PROJECTION]
 */
fun querySongFiles(
    context: Context,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    sortOrder: String? = Setting.instance.songSortMode.SQLQuerySortOrder,
): Cursor? = queryAudio(
    context,
    BASE_FILE_PROJECTION,
    selection,
    selectionValues,
    sortOrder
)

/**
 * query audio via MediaStore
 */
fun queryAudio(
    context: Context,
    projection: Array<String>,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    sortOrder: String? = Setting.instance.songSortMode.SQLQuerySortOrder,
): Cursor? {

    val actual =
        withPathFilter(context) {
            SQLWhereClause(
                selection = withBaseAudioFilter { selection },
                selectionValues = selectionValues
            )
        }

    return try {
        context.contentResolver.query(
            Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            actual.selection,
            actual.selectionValues,
            sortOrder
        )
    } catch (e: SecurityException) {
        null
    }
}

val BASE_SONG_PROJECTION = arrayOf(
    BaseColumns._ID, // 0
    AudioColumns.TITLE, // 1
    AudioColumns.TRACK, // 2
    AudioColumns.YEAR, // 3
    AudioColumns.DURATION, // 4
    AudioColumns.DATA, // 5
    AudioColumns.DATE_ADDED, // 6
    AudioColumns.DATE_MODIFIED, // 7
    AudioColumns.ALBUM_ID, // 8
    AudioColumns.ALBUM, // 9
    AudioColumns.ARTIST_ID, // 10
    AudioColumns.ARTIST, // 11
    AudioColumns.ALBUM_ARTIST, // 12 (hidden api before R)
    AudioColumns.COMPOSER, // 13 (hidden api before R)
)

val BASE_FILE_PROJECTION = arrayOf(
    BaseColumns._ID, // 0
    AudioColumns.DISPLAY_NAME, // 1
    AudioColumns.DATA, // 2
    AudioColumns.SIZE, // 3
    AudioColumns.DATE_ADDED, // 4
    AudioColumns.DATE_MODIFIED, // 5
)

const val BASE_AUDIO_SELECTION =
    "${AudioColumns.IS_MUSIC} =1 "

const val BASE_PLAYLIST_SELECTION =
    "${MediaStoreCompat.Audio.PlaylistsColumns.NAME} != '' "