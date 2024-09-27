/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.util.isEmbeddingOverflow
import player.phonograph.util.produceSafeId
import android.content.Context


suspend fun dumpAllSongIds(context: Context): Collection<Long> = Songs.all(context).map { it.id }

suspend fun checkEmbeddedIdOverflow(context: Context): Collection<Song> {

    val ids = dumpAllSongIds(context)
    val overflowed = ids.filter { isEmbeddingOverflow(it) }

    return if (overflowed.isNotEmpty()) {
        overflowed.map { Songs.id(context, it) }
    } else {
        emptyList()
    }
}

suspend fun checkIdConflict(context: Context): Collection<Song> {
    val ids = dumpAllSongIds(context)
    val safeIds = ids.mapIndexed { pos, id -> produceSafeId(id, pos) }

    val uniques = safeIds.toSet()
    if (uniques.size != ids.size) {
        val conflicted = ids - uniques
        return conflicted.map { Songs.id(context, it) }
    } else {
        return emptyList()
    }
}
