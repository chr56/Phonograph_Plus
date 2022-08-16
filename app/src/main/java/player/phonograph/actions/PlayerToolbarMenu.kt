/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.view.Menu
import android.view.MenuItem
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.dialogs.CreatePlaylistDialog
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.preferences.NowPlayingScreenPreferenceDialog
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.util.NavigationUtil.openEqualizer

fun injectPlayerToolbar(
    menu: Menu,
    playerFragment: AbsPlayerFragment
) = playerFragment.requireActivity().run {
    attach(menu) {
        menuItem {
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
