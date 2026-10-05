/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.album

import player.phonograph.model.Album
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.ALWAYS
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.IF_ROOM
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.NEVER
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.actions.actionAddToPlaylist
import player.phonograph.ui.actions.actionDelete
import player.phonograph.ui.actions.actionEnqueue
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.goToArtistDetail
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.web.LastFmDialog
import player.phonograph.ui.resource.ActionSemantics
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import android.content.Context
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
object AlbumDetailActionEntries {

    private fun entry(
        semantic: ActionSemantic,
        visibility: Int = IF_ROOM,
        dangerous: Boolean = false,
        action: suspend () -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(semantic, action, visibility, dangerous)

    fun Play(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.Play, visibility = ALWAYS) {
            item.allSongs(context).actionPlay(ShuffleMode.NONE, 0)
        }

    fun ShuffleAlbum(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.ShuffleAlbum, visibility = ALWAYS) {
            val songs = item.allSongs(context)
            songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
        }

    fun PlayNext(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            item.allSongs(context).actionPlayNext()
        }

    fun AddToPlayingQueue(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            item.allSongs(context).actionEnqueue()
        }

    fun AddToPlaylist(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            item.allSongs(context).actionAddToPlaylist(context)
        }

    fun GoToArtist(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.GoToArtist, visibility = NEVER) {
            goToArtistDetail(context, item)
        }

    fun TagEditor(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.TagEditor, visibility = NEVER) {
            val songs = item.allSongs(context)
            MultiTagBrowserActivity.launch(context, ArrayList(songs.map { it.data }))
        }

    fun DeleteFromDevice(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, visibility = NEVER, dangerous = true) {
            if (context is FragmentActivity) {
                item.allSongs(context).actionDelete(context)
            }
        }

    fun Wiki(context: ComponentActivity, item: Album): ActionEntry =
        entry(ActionSemantics.Wiki, visibility = NEVER) {
            if (context is FragmentActivity) {
                LastFmDialog.from(item).show(context.supportFragmentManager, "LastFmDialog")
            }
        }

    private suspend fun Album.allSongs(context: Context) =
        withContext(Dispatchers.IO) { Songs.album(context, id) }
}