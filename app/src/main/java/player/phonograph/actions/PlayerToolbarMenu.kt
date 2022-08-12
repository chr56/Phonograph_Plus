/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import com.github.chr56.android.menu_dsl.add
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.dialogs.CreatePlaylistDialog
import player.phonograph.dialogs.LyricsDialog
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.preferences.NowPlayingScreenPreferenceDialog
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.PlayerFragmentViewModel
import player.phonograph.util.FavoriteUtil.isFavorite
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil.openEqualizer

fun injectPlayerToolbar(
    menu: Menu,
    playerFragment: AbsPlayerFragment,
    viewModel: PlayerFragmentViewModel
) = playerFragment.requireActivity().run {
    attach(menu) {
        rootMenu.add(this) {
            order = 0
            title = getString(R.string.lyrics)
            icon = getTintedDrawable(R.drawable.ic_comment_text_outline_white_24dp, Color.WHITE)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            visible = false
            itemId = R.id.action_show_lyrics
            onClick {
                val lyricsPack = viewModel.lyricsList
                if (lyricsPack != null) {
                    LyricsDialog.create(
                        lyricsPack,
                        viewModel.currentSong,
                        viewModel.currentLyrics ?: lyricsPack.getAvailableLyrics()!!
                    ).show(playerFragment.childFragmentManager, "LYRICS")
                }
                true
            }
        }.apply {
            viewModel.lyricsMenuItem = this
        }

        // todo
        rootMenu.add(this) {
            order = 1
            title = getString(R.string.action_add_to_favorites)
            icon = getTintedDrawable(R.drawable.ic_favorite_border_white_24dp, Color.WHITE) // default state
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            itemId = R.id.action_toggle_favorite
            onClick {
                playerFragment.requireContext().run {
                    viewModel.toggleFavorite(this, viewModel.currentSong)
                    viewModel.updateFavoriteIcon(isFavorite(this, viewModel.currentSong))
                }
                true
            }
        }.apply {
            viewModel.favoriteMenuItem = this
        }

        menuItem {
            order = 2
            title = getString(R.string.action_clear_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                MusicPlayerRemote.clearQueue()
            }
        }
        menuItem {
            title = getString(R.string.action_save_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                CreatePlaylistDialog.create(MusicPlayerRemote.playingQueue)
                    .show(playerFragment.childFragmentManager, "ADD_TO_PLAYLIST")
                true
            }
        }
        menuItem {
            title = getString(R.string.action_sleep_timer)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                SleepTimerDialog()
                    .show(playerFragment.childFragmentManager, "SET_SLEEP_TIMER")
                true
            }
        }
        menuItem {
            title = getString(R.string.equalizer)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                openEqualizer(playerFragment.requireActivity())
                true
            }
        }
        menuItem {
            title = getString(R.string.change_now_playing_screen)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                NowPlayingScreenPreferenceDialog()
                    .show(playerFragment.childFragmentManager, "NOW_PLAYING_SCREEN")
                true
            }
        }
    }
}
