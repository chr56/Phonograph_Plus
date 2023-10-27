/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.*
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.ui.components.popup.ComposePopup
import player.phonograph.ui.components.popup.SongActionMenuPopupContent
import player.phonograph.util.lifecycleScopeOrNewOne
import androidx.compose.runtime.Composable
import android.app.Activity
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
object ActionMenuProviders {

    abstract class ActionMenuProvider<I> {

        /**
         * prepare action menu of this [item]
         */
        fun prepareActionMenu(menuButtonView: View?, item: I) {
            menuButtonView?.setOnClickListener {
                showActionMenu(menuButtonView, item)
            }
        }


        /**
         * show action menu popup of this [item]
         */
        abstract fun showActionMenu(menuButtonView: View, item: I)


        protected fun showMenuPopup(menuButtonView: View, block: (PopupMenu) -> Unit) {
            PopupMenu(menuButtonView.context, menuButtonView).also { popupMenu ->
                block(popupMenu)
            }.show()
        }

        protected fun showComposePopup(menuButtonView: View, block: @Composable () -> Unit) {
            val activity = menuButtonView.context as Activity

            val location: IntArray = intArrayOf(0, 0).also { menuButtonView.getLocationInWindow(it) }

            val popup = ComposePopup.content(activity, content = block)

            val x = menuButtonView.width
            val y = menuButtonView.height + location[1]

            popup.showAtLocation(activity.window.decorView, Gravity.TOP or Gravity.END, -x, y)
        }
    }

    class SongActionMenuProvider(
        private val showPlay: Boolean,
        private val index: Int = Int.MIN_VALUE,
        private val transitionView: View? = null,
    ) : ActionMenuProvider<Song>() {
        override fun showActionMenu(menuButtonView: View, song: Song) {
            showComposePopup(menuButtonView) {
                SongActionMenuPopupContent(song, showPlay, index, transitionView)
            }
        }
    }

    object PlaylistActionMenuProvider : ActionMenuProvider<Playlist>() {

        override fun showActionMenu(menuButtonView: View, playlist: Playlist) {
            showMenuPopup(menuButtonView) { popupMenu ->
                menuButtonView.context.run {
                    attach(popupMenu.menu) {
                        menuItem {
                            title = getString(R.string.action_play)
                            onClick { playlist.actionPlay(context) }
                        }
                        menuItem {
                            title = getString(R.string.action_play_next)
                            onClick { playlist.actionPlayNext(context) }
                        }
                        menuItem {
                            title = getString(R.string.action_add_to_playing_queue)
                            onClick { playlist.actionAddToCurrentQueue(context) }
                        }
                        menuItem {
                            title = getString(R.string.add_playlist_title)
                            onClick {
                                fragmentActivity(context) {
                                    playlist.actionAddToPlaylist(it)
                                    true
                                }
                            }
                        }
                        if (playlist is FilePlaylist) {
                            menuItem {
                                title = getString(R.string.rename_action)
                                onClick {
                                    fragmentActivity(context) {
                                        playlist.actionRenamePlaylist(it)
                                        true
                                    }
                                }
                            }
                            menuItem {
                                val pined = FavoritesStore.get().containsPlaylist(playlist)
                                title =
                                    getString(if (!pined) R.string.action_pin else R.string.action_unpin)
                                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                                onClick {
                                    context.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
                                        val ins = FavoritesStore.get()
                                        if (pined) ins.removePlaylist(playlist) else ins.addPlaylist(playlist)
                                    }
                                    true
                                }
                            }
                        }
                        if (playlist is ResettablePlaylist) {
                            menuItem {
                                title =
                                    if (playlist is FilePlaylist) getString(R.string.delete_action)
                                    else getString(R.string.clear_action)
                                onClick {
                                    fragmentActivity(context) {
                                        playlist.actionDeletePlaylist(it)
                                        true
                                    }
                                }
                            }
                        }
                        menuItem {
                            title = getString(R.string.save_playlist_title)
                            onClick {
                                fragmentActivity(context) {
                                    playlist.actionSavePlaylist(it)
                                    true
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    object EmptyActionMenuProvider : ActionMenuProvider<Any>() {
        override fun showActionMenu(menuButtonView: View, item: Any) {}
    }
}