/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.actions.actionPlayNext
import player.phonograph.mediastore.loaders.GenreLoader
import player.phonograph.model.Genre
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.theme.getTintedDrawable
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlin.random.Random

fun genreDetailToolbar(
    menu: Menu,
    context: Context,
    genre: Genre,
): Boolean = with(context) {
    val iconColor = primaryTextColor(ThemeColor.primaryColor(context))
    attach(menu) {
        val allSongs = GenreLoader.genreSongs(context, genre.id)
        menuItem(getString(R.string.action_play)) {
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                allSongs.actionPlay(ShuffleMode.NONE, 0)
                true
            }
        }
        menuItem(getString(R.string.action_shuffle_playlist)) {
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
            }
        }
        menuItem(getString(R.string.action_play_next)) { //id = R.id.action_play_next
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick { allSongs.actionPlayNext() }
        }
    }
    true
}