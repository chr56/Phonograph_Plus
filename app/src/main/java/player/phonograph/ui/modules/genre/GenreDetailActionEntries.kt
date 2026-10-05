/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.genre

import player.phonograph.model.Genre
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.ALWAYS
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.IF_ROOM
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Genres
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.resource.ActionSemantics
import androidx.activity.ComponentActivity
import android.content.Context
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
object GenreDetailActionEntries {

    private fun entry(
        semantic: ActionSemantic,
        visibility: Int = IF_ROOM,
        dangerous: Boolean = false,
        action: suspend () -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(semantic, action, visibility, dangerous)

    fun Play(context: ComponentActivity, item: Genre): ActionEntry =
        entry(ActionSemantics.Play, visibility = ALWAYS) {
            item.allSongs(context).actionPlay(ShuffleMode.NONE, 0)
        }

    fun ShufflePlaylist(context: ComponentActivity, item: Genre): ActionEntry =
        entry(ActionSemantics.ShufflePlaylist, visibility = ALWAYS) {
            val songs = item.allSongs(context)
            songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
        }

    fun PlayNext(context: ComponentActivity, item: Genre): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            item.allSongs(context).actionPlayNext()
        }

    private suspend fun Genre.allSongs(context: Context) =
        withContext(Dispatchers.IO) { Genres.songs(context, id) }
}