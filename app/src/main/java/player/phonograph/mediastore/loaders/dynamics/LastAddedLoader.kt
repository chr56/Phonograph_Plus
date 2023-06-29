/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore.loaders.dynamics

import player.phonograph.mediastore.intoSongs
import player.phonograph.mediastore.querySongs
import player.phonograph.model.Song
import player.phonograph.settings.Setting
import android.content.Context
import android.provider.MediaStore

object LastAddedLoader {
    fun lastAddedSongs(context: Context, cutoff: Long = Setting.instance.lastAddedCutoff): List<Song> =
        querySongs(
            context,
            MediaStore.Audio.Media.DATE_ADDED + ">?",
            arrayOf(cutoff.toString()),
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        ).intoSongs()
}
