/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.theme.ThemeColor
import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.actions.actionPlayNext
import player.phonograph.model.Genre
import player.phonograph.repo.loader.Songs
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.lifecycleScopeOrNewOne
import player.phonograph.util.theme.getTintedDrawable
import util.theme.color.primaryTextColor
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlin.random.Random
import kotlinx.coroutines.launch

fun genreDetailToolbar(
    menu: Menu,
    context: Context,
    genre: Genre,
): Boolean = with(context) {
    val iconColor = primaryTextColor(ThemeColor.primaryColor(context))
    attach(menu) {
        menuItem(getString(R.string.action_play)) {
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                context.lifecycleScopeOrNewOne().launch {
                    val allSongs = genre.allSongs(context)
                    allSongs.actionPlay(ShuffleMode.NONE, 0)
                }
                true
            }
        }
        menuItem(getString(R.string.action_shuffle_playlist)) {
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                context.lifecycleScopeOrNewOne().launch {
                    val allSongs = genre.allSongs(context)
                    allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                }
                true
            }
        }
        menuItem(getString(R.string.action_play_next)) { //id = R.id.action_play_next
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                context.lifecycleScopeOrNewOne().launch {
                    val allSongs = genre.allSongs(context)
                    allSongs.actionPlayNext()
                }
                true
            }
        }
    }
    true
}

private suspend fun Genre.allSongs(context: Context) = Songs.genres(context, id)