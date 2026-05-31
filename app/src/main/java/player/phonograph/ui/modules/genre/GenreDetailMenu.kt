/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.genre

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Genres
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun inflateGenreDetailMenu(
    menu: Menu,
    context: ComponentActivity,
    item: Genre,
    @ColorInt iconColor: Int,
): Boolean =
    with(context) {
        attach(menu) {
            menuItem(getString(R.string.action_play)) {
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    context.lifecycleScope.launch {
                        val allSongs = item.allSongs(context)
                        allSongs.actionPlay(ShuffleMode.NONE, 0)
                    }
                    true
                }
            }
            menuItem(getString(R.string.action_shuffle_playlist)) {
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    context.lifecycleScope.launch {
                        val allSongs = item.allSongs(context)
                        allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                    }
                    true
                }
            }
            menuItem(getString(R.string.action_play_next)) { //id = R.id.action_play_next
                icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        val allSongs = item.allSongs(context)
                        allSongs.actionPlayNext()
                    }
                    true
                }
            }
        }
        true
    }

private suspend fun Genre.allSongs(context: Context) = withContext(Dispatchers.IO) {
    Genres.songs(context, id)
}