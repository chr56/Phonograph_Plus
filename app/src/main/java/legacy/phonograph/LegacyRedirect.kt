/*
 * Copyright (c) 2023 chr_56
 */

@file:Suppress("PropertyName", "ObjectPropertyName")

package legacy.phonograph

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

/**
 * This is just to move all "deprecated" stuffs to one place :)
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

            object Members {
                const val _ID = MediaStore.Audio.Playlists.Members._ID
                const val AUDIO_ID = MediaStore.Audio.Playlists.Members.AUDIO_ID
                const val PLAY_ORDER = MediaStore.Audio.Playlists.Members.PLAY_ORDER
                const val DEFAULT_SORT_ORDER = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER

                fun getContentUri(volumeName: String?, playlistId: Long): Uri =
                    MediaStore.Audio.Playlists.Members
                        .getContentUri(volumeName, playlistId)

                fun moveItem(
                    contentResolver: ContentResolver,
                    playlistId: Long,
                    from: Int,
                    to: Int
                ): Boolean =
                    MediaStore.Audio.Playlists.Members
                        .moveItem(contentResolver, playlistId, from, to)

            }
        }

        object PlaylistsColumns {
            const val NAME = MediaStore.Audio.PlaylistsColumns.NAME
            const val DATA = MediaStore.Audio.PlaylistsColumns.DATA
        }
    }
}