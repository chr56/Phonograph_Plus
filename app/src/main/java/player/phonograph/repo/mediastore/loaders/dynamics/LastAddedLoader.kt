/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.repo.mediastore.loaders.dynamics

import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
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
