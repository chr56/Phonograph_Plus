/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import player.phonograph.foundation.error.record
import player.phonograph.foundation.mediastore.BASE_FILE_PROJECTION
import player.phonograph.foundation.mediastore.BASE_SONG_PROJECTION
import player.phonograph.foundation.mediastore.mediastoreUriSongsExternal
import player.phonograph.repo.mediastore.defaultSongQuerySortOrder
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns


/**
 * [queryAudio] but
 * using [BASE_SONG_PROJECTION]
 */
suspend fun querySongs(
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
suspend fun querySongFiles(
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
suspend fun queryAudio(
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
            mediastoreUriSongsExternal(),
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
suspend fun locateSongs(
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
suspend fun locateSongs(
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

private const val TAG = "MediaStoreQuery"