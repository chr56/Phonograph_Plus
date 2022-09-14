/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.util

import android.content.ContentUris
import android.net.Uri

internal fun getMediaStoreAlbumCoverUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)