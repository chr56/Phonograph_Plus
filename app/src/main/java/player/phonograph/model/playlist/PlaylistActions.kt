/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.playlist

import android.content.Context
import android.net.Uri

interface PlaylistCreator {

    suspend fun fromUri(context: Context, uri: Uri): Boolean

    suspend fun fromMediaStore(context: Context, name: String): Long

    suspend fun intoDatabase(context: Context, name: String): Boolean

    companion object {
        const val RESULT_ERROR = -1L
        const val RESULT_EXISTED = -2L
    }

}

abstract class PlaylistDeleter(
    protected val playlist: Playlist,
    protected val preferSaf: Boolean,
) {
    abstract suspend fun delete(context: Context): Boolean
}