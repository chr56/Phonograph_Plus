/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import legacy.phonograph.MediaStoreCompat
import player.phonograph.foundation.error.record
import player.phonograph.repo.mediastore.defaultSongQuerySortOrder
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
    sortOrder: String? = defaultSongQuerySortOrder(context),
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
    sortOrder: String? = defaultSongQuerySortOrder(context),
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
    sortOrder: String? = defaultSongQuerySortOrder(context),
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

/**
 * search songs by keywords for tittle, artist, album fields
 */
fun locateSongs(
    context: Context,
    title: String?,
    album: String?,
    artist: String?,
): Cursor? {
    val selections = mutableListOf<String>()
    val selectionValues = mutableListOf<String>()

    if (artist != null) {
        selections.add(ARTIST_SELECTION)
        selectionValues.add(artist.lowercase().trim())
    }

    if (album != null) {
        selections.add(ALBUM_SELECTION)
        selectionValues.add(album.lowercase().trim())
    }

    if (title != null) {
        selections.add(TITLE_SELECTION)
        selectionValues.add(title.lowercase().trim())
    }

    return if (selections.isNotEmpty()) {
        querySongs(context, selections.joinToString(separator = AND), selectionValues.toTypedArray())
    } else {
        null
    }
}

/**
 * search songs by keyword for tittle, artist, album fields
 * @param keyword keyword
 */
fun locateSongs(
    context: Context,
    keyword: String,
): Cursor? {
    for (selection in listOf(TITLE_SELECTION, ALBUM_SELECTION, ARTIST_SELECTION)) {
        val cursor = querySongs(context, selection, arrayOf(keyword.trim()))
        if (cursor != null) {
            return cursor
        }
    }
    return null
}

private const val TITLE_SELECTION = "lower(${AudioColumns.TITLE}) LIKE ?"
private const val ALBUM_SELECTION = "lower(${AudioColumns.ALBUM}) LIKE ?"
private const val ARTIST_SELECTION = "lower(${AudioColumns.ARTIST}) LIKE ?"
private const val AND = " AND "

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

private const val TAG = "MediaStoreQuery"