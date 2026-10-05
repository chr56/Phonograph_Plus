/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.artist

import player.phonograph.R
import player.phonograph.foundation.content.IGetContentRequester
import player.phonograph.mechanism.coil.CustomArtistImageStore
import player.phonograph.model.Artist
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.IF_ROOM
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.ALWAYS
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.NEVER
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.actions.actionAddToPlaylist
import player.phonograph.ui.actions.actionDelete
import player.phonograph.ui.actions.actionEnqueue
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.web.LastFmDialog
import player.phonograph.ui.resource.ActionSemantics
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.widget.Toast
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
object ArtistDetailActionEntries {

    private fun entry(
        semantic: ActionSemantic,
        visibility: Int = IF_ROOM,
        dangerous: Boolean = false,
        action: suspend () -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(semantic, action, visibility, dangerous)

    fun Play(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.Play, visibility = ALWAYS) {
            item.allSongs(context).actionPlay(ShuffleMode.NONE, 0)
        }

    fun ShuffleArtist(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.ShuffleArtist, visibility = ALWAYS) {
            val songs = item.allSongs(context)
            songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
        }

    fun PlayNext(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            item.allSongs(context).actionPlayNext()
        }

    fun AddToPlayingQueue(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            item.allSongs(context).actionEnqueue()
        }

    fun AddToPlaylist(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            item.allSongs(context).actionAddToPlaylist(context)
        }

    fun SetArtistImage(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.SetArtistImage, visibility = NEVER) {
            if (context is IGetContentRequester) {
                context.getContentDelegate.launch("image/*") {
                    if (it != null) CustomArtistImageStore.instance(context)
                        .setCustomArtistImage(context, item.id, item.name, it)
                }
            }
        }

    fun ResetArtistImage(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.ResetArtistImage, visibility = NEVER) {
            Toast.makeText(context, R.string.state_updating, Toast.LENGTH_SHORT).show()
            CustomArtistImageStore.instance(context).resetCustomArtistImage(context, item.id, item.name)
        }

    fun TagEditor(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.TagEditor, visibility = NEVER) {
            val songs = item.allSongs(context)
            MultiTagBrowserActivity.launch(context, ArrayList(songs.map { it.data }))
        }

    fun DeleteFromDevice(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, visibility = NEVER, dangerous = true) {
            item.allSongs(context).actionDelete(context)
        }

    fun Biography(context: ComponentActivity, item: Artist): ActionEntry =
        entry(ActionSemantics.Biography, visibility = NEVER) {
            if (context is FragmentActivity) {
                LastFmDialog.from(item).show(context.supportFragmentManager, "LastFmDialog")
            }
        }

    private suspend fun Artist.allSongs(context: Context) =
        withContext(Dispatchers.IO) { Songs.artist(context, id) }
}