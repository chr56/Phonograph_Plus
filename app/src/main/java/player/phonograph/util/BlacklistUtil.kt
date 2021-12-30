/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.provider.BlacklistStore
import java.io.File

object BlacklistUtil {
    fun addToBlacklist(context: Context, file: File) {
        if (file.isDirectory)
            addToBlacklistImpl(context, file.absolutePath)
        else
            addToBlacklistImpl(context, file.absolutePath.dropLastWhile { it != '/' }.dropLast(1))
    }

    fun addToBlacklist(context: Context, song: Song) {
        if (song.data.isNotBlank())
            addToBlacklistImpl(context, song.data.dropLastWhile { it != '/' }.dropLast(1)) // last char is '/'
    }

    private fun addToBlacklistImpl(context: Context, string: String) {
        var path: String = string // parent folder

        val candidatesPath = mutableListOf<String>()
        while (path.isNotEmpty()) {
            if (path.endsWith("/emulated/0", true)
                or path.endsWith("/emulated", true)
                or path.endsWith("/storage", true)
            ) break // no junk paths
            candidatesPath.add(path)
            path = path.dropLastWhile { it != '/' }.dropLast(1) // last char is '/'
        }

        MaterialDialog(context)
            .title(R.string.label_file_path)
            .noAutoDismiss()
            .listItemsSingleChoice(items = candidatesPath) { dialog, _, pathText ->
                if (pathText.isNotBlank()) {
                    MaterialDialog(context)
                        .title(R.string.add_blacklist)
                        .message(text = pathText)
                        .positiveButton(android.R.string.ok) {
                            BlacklistStore.getInstance(App.instance).addPath(File(pathText as String))
                            dialog.dismiss()
                        }
                        .negativeButton(android.R.string.cancel)
                        .show()
                }
            }
            .show()
    }
}
