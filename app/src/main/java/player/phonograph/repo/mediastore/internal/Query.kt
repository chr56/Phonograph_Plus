/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import legacy.phonograph.MediaStoreCompat
import player.phonograph.foundation.error.record
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriSongs
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns


/**
 * [queryAudio] but
 * using [BASE_SONG_PROJECTION]
 */
fun querySongs(
    context: Context,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    sortOrder: String? = defaultSortOrder(context),
    withoutPathFilter: Boolean = false,
): Cursor? = queryAudio(
    context,
    BASE_SONG_PROJECTION,
    selection,
    selectionValues,
    sortOrder,
    withoutPathFilter
)

/**
 * [queryAudio] but
 * using [BASE_FILE_PROJECTION]
 */
fun querySongFiles(
    context: Context,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    sortOrder: String? = defaultSortOrder(context),
): Cursor? = queryAudio(
    context,
    BASE_FILE_PROJECTION,
    selection,
    selectionValues,
    sortOrder,
    false
)

/**
 * query audio via MediaStore
 */
fun queryAudio(
    context: Context,
    projection: Array<String>,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    sortOrder: String? = defaultSortOrder(context),
    withoutPathFilter: Boolean,
): Cursor? {

    val actual =
        withPathFilter(context, escape = withoutPathFilter) {
            SQLWhereClause(
                selection = withBaseAudioFilter { selection },
                selectionValues = selectionValues
            )
        }

    return try {
        context.contentResolver.query(
            mediastoreUriSongs(MEDIASTORE_VOLUME_EXTERNAL),
            projection,
            actual.selection,
            actual.selectionValues,
            sortOrder
        )
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        record(context, e, TAG)
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

private fun defaultSortOrder(context: Context) =
    Setting(context)[Keys.songSortMode].data.mediastoreQuerySortOrder()

fun SortMode.mediastoreQuerySortOrder(): String {
    val first = when (sortRef) {
        SortRef.ID                -> AudioColumns._ID
        SortRef.SONG_NAME         -> Audio.Media.DEFAULT_SORT_ORDER
        SortRef.ARTIST_NAME       -> Audio.Artists.DEFAULT_SORT_ORDER
        SortRef.ALBUM_NAME        -> Audio.Albums.DEFAULT_SORT_ORDER
        SortRef.ALBUM_ARTIST_NAME -> Audio.Media.ALBUM_ARTIST
        SortRef.COMPOSER          -> Audio.Media.COMPOSER
        SortRef.ADDED_DATE        -> Audio.Media.DATE_ADDED
        SortRef.MODIFIED_DATE     -> Audio.Media.DATE_MODIFIED
        SortRef.DURATION          -> Audio.Media.DURATION
        SortRef.YEAR              -> Audio.Media.YEAR
        // SortRef.SONG_COUNT        -> "" // todo
        // SortRef.ALBUM_COUNT       -> "" // todo
        else                      -> throw IllegalStateException("invalid sort mode")
    }
    val second = if (revert) "DESC" else "ASC"

    return "$first $second"
}

private const val TAG = "MediaStoreQuery"