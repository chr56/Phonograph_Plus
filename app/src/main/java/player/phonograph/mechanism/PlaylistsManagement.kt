/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import player.phonograph.model.playlist.FilePlaylist
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.util.Log

object PlaylistsManagement {
    private const val TAG: String = "PlaylistsManagement"

    fun searchPlaylist(context: Context, dir: DocumentFile, filePlaylists: List<FilePlaylist>): List<DocumentFile> {
        val fileNames = filePlaylists.map { it.associatedFilePath }
        if (fileNames.isEmpty()) {
            Log.w(TAG, "No playlist display name?")
            return mutableListOf()
        }
        return searchFiles(context, dir, fileNames)
    }

    fun searchFiles(context: Context, dir: DocumentFile, filenames: List<String>): List<DocumentFile> {
        if (dir.isFile or dir.isVirtual) return mutableListOf()
        val result = mutableListOf<DocumentFile>()
        if (dir.isDirectory) {
            dir.listFiles().forEach { sub ->
                if (sub.isFile) {
                    filenames.forEach { n ->
                        if (n == sub.name.orEmpty()) result.add(sub)
                    }
                }
                if (sub.isDirectory) {
                    result.addAll(
                        searchFiles(context, sub, filenames)
                    )
                }
            }
        }
        return result
    }

}
