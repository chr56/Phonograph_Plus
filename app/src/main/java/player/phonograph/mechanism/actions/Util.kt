/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.actions

import lib.activityresultcontract.ActivityResultLauncherDelegate
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileEntity
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Songs
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.Context
import android.net.Uri

inline fun activity(context: Context, block: (Activity) -> Boolean): Boolean =
    if (context is Activity) {
        block(context)
    } else {
        false
    }

inline fun fragmentActivity(context: Context, block: (FragmentActivity) -> Boolean): Boolean =
    if (context is FragmentActivity) {
        block(context)
    } else {
        false
    }

class GetContentDelegate : ActivityResultLauncherDelegate<String, Uri?>() {
    override val key: String = "GetContent"
    override val contract: ActivityResultContract<String, Uri?> = ActivityResultContracts.GetContent()
}

interface IGetContentRequester {
    val getContentDelegate: GetContentDelegate
}