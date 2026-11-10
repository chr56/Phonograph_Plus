/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.playlist

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.ALWAYS
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.IF_ROOM
import player.phonograph.model.menu.ActionEntry.ToolbarEntry.Companion.NEVER
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.ui.UIMode
import player.phonograph.repo.loader.FavoriteTracks
import player.phonograph.ui.actions.ItemActions
import player.phonograph.ui.goToSongDetail
import player.phonograph.ui.modules.setting.dialog.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.resource.ActionSemantics
import player.phonograph.ui.theme.tintButtons
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("FunctionName", "UNUSED_PARAMETER")
object PlaylistDetailActionEntries {

    private fun entry(
        semantic: ActionSemantic,
        visibility: Int = IF_ROOM,
        dangerous: Boolean = false,
        action: suspend () -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(semantic, action, visibility, dangerous)

    fun Play(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.Play, visibility = ALWAYS) {
            ItemActions.play(context, item)
        }

    fun ShufflePlaylist(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.ShufflePlaylist, visibility = ALWAYS) {
            ItemActions.shuffleAndPlay(context, item)
        }

    fun PlayNext(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            ItemActions.playNext(context, item)
        }

    fun AddToPlayingQueue(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            ItemActions.addToCurrentQueue(context, item)
        }

    fun AddToPlaylist(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            if (context is FragmentActivity) {
                ItemActions.addToPlaylist(context, item)
            }
        }

    fun Rename(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.Rename) {
            if (context is FragmentActivity) {
                ItemActions.renamePlaylist(context, item)
            }
        }

    fun SavePlaylist(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.SavePlaylist, visibility = NEVER) {
            if (context is FragmentActivity) {
                ItemActions.savePlaylist(context, item)
            }
        }

    fun Delete(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(
            if (item.isVirtual()) ActionSemantics.Clear else ActionSemantics.Delete,
            visibility = NEVER,
            dangerous = true,
        ) {
            if (context is FragmentActivity) {
                ItemActions.deletePlaylist(context, item)
            }
        }

    fun TagEditor(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.TagEditor, visibility = NEVER) {
            val paths = withContext(Dispatchers.IO) {
                PlaylistSongsActions.reader(item).allSongs(context).map { it.data }
            }
            goToSongDetail(context, ArrayList(paths))
        }

    fun Search(context: ComponentActivity, onAction: (PlaylistAction) -> Boolean): ActionEntry =
        entry(ActionSemantics.Search) {
            onAction(PlaylistAction.UpdateMode(UIMode.Search))
        }

    fun Refresh(context: ComponentActivity, onAction: (PlaylistAction) -> Boolean): ActionEntry =
        entry(ActionSemantics.Refresh) {
            onAction(PlaylistAction.Load(scan = true))
        }

    fun Edit(context: ComponentActivity, onAction: (PlaylistAction) -> Boolean): ActionEntry =
        entry(ActionSemantics.Edit, visibility = NEVER) {
            onAction(PlaylistAction.UpdateMode(UIMode.Editor))
        }

    fun LastAddedInterval(context: ComponentActivity, onAction: (PlaylistAction) -> Boolean): ActionEntry =
        entry(ActionSemantics.LastAddedInterval, visibility = NEVER) {
            if (context is FragmentActivity) {
                val dialog = LastAddedPlaylistIntervalDialog()
                dialog.show(context.supportFragmentManager, "LAST_ADDED")
                dialog.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        onAction(PlaylistAction.Load())
                    }
                })
            }
        }

    fun Clean(context: ComponentActivity, item: Playlist): ActionEntry =
        entry(ActionSemantics.Clean, visibility = NEVER, dangerous = true) {
            if (context is FragmentActivity) {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.action_clean)
                    .setMessage(R.string.action_clean_missing_items)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        context.lifecycleScope.launch {
                            FavoriteTracks.cleanMissing(context)
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .tintButtons()
                    .show()
            }
        }
}