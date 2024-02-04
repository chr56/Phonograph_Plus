/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_model.MenuContext
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.fragments.pages.adapter.SongDisplayAdapter
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.fragment.app.viewModels
import android.content.Context
import android.view.MenuItem
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope

class SongPage : AbsDisplayPage<Song, DisplayAdapter<Song>>() {

    override val viewModel: AbsDisplayPageViewModel<Song> get() = _viewModel

    private val _viewModel: SongPageViewModel by viewModels()

    class SongPageViewModel : AbsDisplayPageViewModel<Song>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Song> {
            return Songs.all(App.instance)
        }

        override val headerTextRes: Int get() = R.plurals.item_songs
    }


    override fun displayConfig(): PageDisplayConfig = SongPageDisplayConfig(requireContext())

    override fun initAdapter(): DisplayAdapter<Song> {
        val displayConfig = displayConfig()
        return SongDisplayAdapter(
            mainActivity,
            adapterDisplayConfig.copy(layoutStyle = displayConfig.layout, usePalette = displayConfig.colorFooter),
        )
    }

    override fun configAppBarActionButton(menuContext: MenuContext) = with(menuContext) {
        menuItem(getString(R.string.action_play)) {
            icon = context
                .getTintedDrawable(
                    R.drawable.ic_play_arrow_white_24dp,
                    context.primaryTextColor(context.nightMode)
                )
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                val allSongs = Songs.all(context)
                allSongs.actionPlay(ShuffleMode.NONE, 0)
                true
            }
        }
        menuItem(getString(R.string.action_shuffle_all)) {
            icon = context
                .getTintedDrawable(
                    R.drawable.ic_shuffle_white_24dp,
                    context.primaryTextColor(context.nightMode)
                )
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                val allSongs = Songs.all(context)
                allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                true
            }
        }
        Unit
    }

    companion object {
        const val TAG = "SongPage"
    }
}
