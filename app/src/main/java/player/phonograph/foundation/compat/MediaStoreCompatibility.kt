/*
 *  Copyright (c) 2022~2025 chr_56
 */

@file:Suppress("DEPRECATION", "ConstPropertyName")

package player.phonograph.foundation.compat

import android.content.ContentResolver
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.provider.MediaStore

/**
 * Mediastore Storage Volume `external`
 *
 * @see [MediaStore.VOLUME_EXTERNAL]
 */
val MEDIASTORE_VOLUME_EXTERNAL = if (SDK_INT >= VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL else "external"


/**
 * Redirect all the "deprecated" :)
 */
object MediaStoreCompat {
    object Audio {
        object Playlists {

            const val _ID = MediaStore.Audio.Playlists._ID
            const val CONTENT_TYPE = MediaStore.Audio.Playlists.CONTENT_TYPE
            const val ENTRY_CONTENT_TYPE = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE
            const val DEFAULT_SORT_ORDER = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER

            val EXTERNAL_CONTENT_URI: Uri get() = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
            val INTERNAL_CONTENT_URI: Uri get() = MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI

            fun getContentUri(volumeName: String?): Uri =
                MediaStore.Audio.Playlists.getContentUri(volumeName)

            object Members {
                const val _ID = MediaStore.Audio.Playlists.Members._ID
                const val AUDIO_ID = MediaStore.Audio.Playlists.Members.AUDIO_ID
                const val PLAY_ORDER = MediaStore.Audio.Playlists.Members.PLAY_ORDER
                const val DEFAULT_SORT_ORDER = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER

                fun getContentUri(volumeName: String?, playlistId: Long): Uri =
                    MediaStore.Audio.Playlists.Members.getContentUri(volumeName, playlistId)

                fun moveItem(contentResolver: ContentResolver, playlistId: Long, from: Int, to: Int): Boolean =
                    MediaStore.Audio.Playlists.Members.moveItem(contentResolver, playlistId, from, to)

            }
        }

        object PlaylistsColumns {
            const val NAME = MediaStore.Audio.PlaylistsColumns.NAME
            const val DATA = MediaStore.Audio.PlaylistsColumns.DATA
            const val DATE_ADDED = MediaStore.Audio.PlaylistsColumns.DATE_ADDED
            const val DATE_MODIFIED = MediaStore.Audio.PlaylistsColumns.DATE_MODIFIED
        }
    }
}