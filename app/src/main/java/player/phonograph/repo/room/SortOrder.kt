/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.room.entity.Columns.ALBUM_ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.ALBUM_COUNT
import player.phonograph.repo.room.entity.Columns.ALBUM_NAME
import player.phonograph.repo.room.entity.Columns.ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.COMPOSER
import player.phonograph.repo.room.entity.Columns.DATE_ADDED
import player.phonograph.repo.room.entity.Columns.DATE_MODIFIED
import player.phonograph.repo.room.entity.Columns.DURATION
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.Columns.SONG_COUNT
import player.phonograph.repo.room.entity.Columns.SONG_ID
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.Columns.YEAR
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

private fun SortMode.roomQuerySortRef(): String = when (sortRef) {
    SortRef.ID                -> SONG_ID
    SortRef.SONG_NAME         -> TITLE
    SortRef.ARTIST_NAME       -> ARTIST_NAME
    SortRef.ALBUM_NAME        -> ALBUM_NAME
    SortRef.ALBUM_ARTIST_NAME -> ALBUM_ARTIST_NAME
    SortRef.COMPOSER          -> COMPOSER
    SortRef.ADDED_DATE        -> DATE_ADDED
    SortRef.MODIFIED_DATE     -> DATE_MODIFIED
    SortRef.DURATION          -> DURATION
    SortRef.YEAR              -> YEAR
    SortRef.SONG_COUNT        -> SONG_COUNT
    SortRef.ALBUM_COUNT       -> ALBUM_COUNT
    SortRef.PATH              -> PATH
    SortRef.DISPLAY_NAME      -> SONG_ID //todo
    SortRef.SIZE              -> SONG_ID //todo
}

fun SortMode.roomQuerySortOrder(): String {
    val first = roomQuerySortRef()
    val second = if (revert) "DESC" else "ASC"
    return "$first $second"
}

internal fun songSortMode(context: Context): SortMode =
    Setting(context).Composites[Keys.songSortMode].data

internal fun albumSortMode(context: Context): SortMode =
    Setting(context).Composites[Keys.albumSortMode].data

internal fun artistSortMode(context: Context): SortMode =
    Setting(context).Composites[Keys.artistSortMode].data

internal val defaultSongSortMode: SortMode get() = SortMode(SortRef.MODIFIED_DATE, true)

internal val defaultAlbumSortMode: SortMode get() = SortMode(SortRef.SONG_COUNT, true)

internal val defaultArtistSortMode: SortMode get() = SortMode(SortRef.SONG_COUNT, true)

internal fun refOfDate(useModifiedDate: Boolean) = if (useModifiedDate) DATE_MODIFIED else DATE_ADDED
