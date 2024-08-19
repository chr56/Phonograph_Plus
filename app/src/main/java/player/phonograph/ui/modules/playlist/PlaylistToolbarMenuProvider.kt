/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.mechanism.actions.DetailToolbarMenuProviders
import player.phonograph.model.UIMode
import player.phonograph.model.playlist.PLAYLIST_TYPE_LAST_ADDED
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.ui.dialogs.LastAddedPlaylistIntervalDialog
import player.phonograph.util.fragmentActivity
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.view.Menu
import android.view.MenuItem

class PlaylistToolbarMenuProvider(
    private val onAction: (PlaylistAction) -> Boolean,
) : DetailToolbarMenuProviders.ToolbarMenuProvider<Playlist> {

    override fun inflateMenu(menu: Menu, context: ComponentActivity, item: Playlist, iconColor: Int): Boolean {

        DetailToolbarMenuProviders.PlaylistEntityToolbarMenuProvider
            .inflateMenu(menu, context, item, iconColor)
        with(context) {
            attach(menu) {
                menuItem {
                    title = getString(R.string.action_search)
                    icon = getTintedDrawable(R.drawable.ic_search_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        onAction(UpdateMode(UIMode.Search))
                    }
                }
                menuItem {
                    title = getString(R.string.refresh)
                    icon = getTintedDrawable(R.drawable.ic_refresh_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        onAction(Refresh(fetch = true))
                        true
                    }
                }
                if (!item.isVirtual()) menuItem {
                    title = getString(R.string.edit)
                    itemId = R.id.action_edit_playlist
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        onAction(UpdateMode(UIMode.Editor))
                        true
                    }
                } else {
                    val location = item.location
                    if (location is VirtualPlaylistLocation && location.type == PLAYLIST_TYPE_LAST_ADDED) {
                        menuItem {
                            itemId = R.id.action_setting_last_added_interval
                            title = getString(R.string.pref_title_last_added_interval)
                            icon = getTintedDrawable(R.drawable.ic_timer_white_24dp, iconColor)
                            onClick {
                                fragmentActivity(context) { activity ->
                                    val dialog = LastAddedPlaylistIntervalDialog()
                                    dialog.show(activity.supportFragmentManager, "LAST_ADDED")
                                    dialog.lifecycle.addObserver(object : DefaultLifecycleObserver {
                                        override fun onDestroy(owner: LifecycleOwner) {
                                            onAction(Refresh(fetch = true))
                                        }
                                    })
                                    true
                                }
                                true
                            }
                        }
                    }
                }
            }
        }
        return true
    }

}