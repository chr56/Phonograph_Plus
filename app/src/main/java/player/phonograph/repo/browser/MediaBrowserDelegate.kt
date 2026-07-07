/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.browser

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaLibraryService.LibraryParams
import android.content.Context
import android.os.Process
import android.util.Log

object MediaBrowserDelegate {
    private const val TAG = "MediaBrowser"

    fun root(context: Context, clientPackageName: String, clientUid: Int, params: LibraryParams?): MediaItem? =
        if (validate(context, clientPackageName, clientUid)) {
            val id = if (params == null) {
                MediaItemPath.ROOT_PATH
            } else {
                when {
                    params.isRecent    -> MediaItemPath.pageLastAdded.mediaId
                    params.isSuggested -> MediaItemPath.pageTopTracks.mediaId
                    else               -> MediaItemPath.ROOT_PATH
                }
            }
            MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        } else {
            null
        }

    suspend fun listChildren(path: String, context: Context): List<MediaItem> =
        MediaItemProviders.of(path).browser(context)

    fun error(context: Context): List<MediaItem> = listOf(MediaItemProviders.error(context))

    // todo: validate package names & signatures
    private fun validate(context: Context, clientPackageName: String, clientUid: Int): Boolean {
        return if (clientUid == Process.SYSTEM_UID) {
            true
        } else if (checkPackageName(clientPackageName)) {
            if (checkSignatures(context, clientPackageName)) {
                true
            } else {
                Log.e(TAG, "Invalidate Signature of $clientPackageName")
                false
            }
        } else {
            Log.e(TAG, "Unknown: $clientPackageName")
            false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkPackageName(clientPackageName: String): Boolean {
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkSignatures(context: Context, clientPackageName: String): Boolean {
        // fetchPackageSignatures(context, clientPackageName)
        return true
    }

}