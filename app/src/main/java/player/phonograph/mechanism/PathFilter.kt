/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.theme.tintButtons
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object PathFilter {
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

        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            val candidatesPath = mutableListOf<String>()
            while (path.isNotEmpty()) {
                if (path.endsWith("/emulated/0", true)
                    or path.endsWith("/emulated", true)
                    or path.endsWith("/storage", true)
                ) break // no junk paths
                candidatesPath.add(path)
                path = path.dropLastWhile { it != '/' }.dropLast(1) // last char is '/'
            }

            withContext(Dispatchers.Main) {
                MaterialDialog(context)
                    .title(R.string.label_file_path)
                    .noAutoDismiss()
                    .listItemsSingleChoice(items = candidatesPath) { parentDialog, _, pathText ->
                        if (pathText.isNotBlank()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                MaterialDialog(context)
                                    .title(R.string.tips_add_to_blacklist)
                                    .message(text = pathText)
                                    .positiveButton(android.R.string.ok) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val preference = Setting(context)[Keys.pathFilterExcludePaths]
                                            val paths = preference.read().toMutableSet()
                                            if (paths.add(pathText.toString())) {
                                                preference.edit { paths }
                                            }
                                        }
                                        parentDialog.dismiss()
                                    }
                                    .negativeButton(android.R.string.cancel)
                                    .tintButtons()
                                    .show()
                            }
                        }
                    }
                    .tintButtons()
                    .show()
            }
        }
    }
}
