/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = Tables.IMAGE_CACHE,
    primaryKeys = [
        Columns.CACHE_DOMAIN,
        Columns.CACHE_ID,
        Columns.CACHE_SOURCE,
    ]
)
data class ImageCacheEntity(
    @ColumnInfo(name = Columns.CACHE_DOMAIN) val domain: Int,
    @ColumnInfo(name = Columns.CACHE_ID) val id: Long,
    @ColumnInfo(name = Columns.CACHE_SOURCE) val type: Int,
    @ColumnInfo(name = Columns.CACHE_TIMESTAMP) val timestamp: Long,
    @ColumnInfo(name = Columns.CACHE_EMPTY) val empty: Boolean,
    @ColumnInfo(name = Columns.CACHE_FILENAME) val filename: String?,
) {
    companion object {
        const val DOMAIN_SONG = 0
        const val DOMAIN_ALBUM = 1
        const val DOMAIN_ARTIST = 2
    }

    @IntDef(DOMAIN_SONG, DOMAIN_ALBUM, DOMAIN_ARTIST)
    @Retention(AnnotationRetention.SOURCE)
    annotation class CacheDomain
}
