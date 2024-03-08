/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.activities

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import lib.phonograph.misc.Reboot
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.mechanism.setting.PageConfig
import player.phonograph.mechanism.setting.StyleConfig
import player.phonograph.model.pages.Pages
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.Scanner
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.dialogs.ScanMediaFolderDialog
import player.phonograph.ui.modules.setting.SettingsActivity
import player.phonograph.ui.modules.web.WebSearchLauncher
import player.phonograph.util.permissions.navigateToAppDetailSetting
import player.phonograph.util.permissions.navigateToStorageSetting
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Menu
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainDrawerViewModel : ViewModel() {

    private val _selectedPage: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedPage: StateFlow<Int> = _selectedPage.asStateFlow()

    fun switchPageTo(page: Int) {
        _selectedPage.value = page
        viewModelScope.launch(Dispatchers.IO) {
            Setting(App.instance)[Keys.lastPage].edit { page }
        }
    }
}


/**
 * inflate drawer menu
 */
fun setupDrawerMenu(
    activity: FragmentActivity,
    menu: Menu,
    switchPageTo: (Int) -> Unit,
    closeDrawer: () -> Unit,
    pageConfig: PageConfig?,
): Unit = with(activity) {
    menu.clear()
    attach(activity, menu) {

        val textColorPrimary: Int = primaryTextColor(nightMode)

        // page chooser
        if (pageConfig != null) {
            val mainGroupId = 999999
            for ((page, tab) in pageConfig.withIndex()) {
                menuItem {
                    groupId = mainGroupId
                    icon = activity.getTintedDrawable(Pages.getTintedIconRes(tab), textColorPrimary)
                    title = Pages.getDisplayName(tab, activity)
                    itemId = 1000 + page
                    onClick {
                        closeDrawer()
                        switchPageTo(page)
                        true
                    }
                }
            }
            rootMenu.setGroupCheckable(mainGroupId, true, true)
        }

        // normal items
        val groupIds = intArrayOf(0, 1, 2, 3)
        if (StyleConfig.generalTheme(activity) != StyleConfig.THEME_AUTO) {
            menuItem {
                groupId = groupIds[1]
                itemId = R.id.action_theme_toggle
                icon = getTintedDrawable(R.drawable.ic_theme_switch_white_24dp, textColorPrimary)
                titleRes(R.string.theme_switch)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            val result = StyleConfig.toggleTheme(activity)
                            if (result) recreate()
                        }, 150
                    )
                }
            }
        }

        menuItem {
            groupId = groupIds[2]
            itemId = R.id.action_shuffle_all
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, textColorPrimary)
            titleRes(R.string.action_shuffle_all)
            onClick {
                closeDrawer()
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        val songs = Songs.all(activity)
                        if (songs.isNotEmpty())
                            songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
                    }, 350
                )
            }
        }
        menuItem {
            groupId = groupIds[2]
            itemId = R.id.action_scan
            icon = getTintedDrawable(R.drawable.ic_scan_white_24dp, textColorPrimary)
            titleRes(R.string.scan_media)
            onClick {
                closeDrawer()
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        ScanMediaFolderDialog().show(supportFragmentManager, "scan_media")
                    }, 200
                )
            }
        }

        menuItem {
            groupId = groupIds[3]
            itemId = R.id.nav_settings
            icon = getTintedDrawable(R.drawable.ic_settings_white_24dp, textColorPrimary)
            titleRes(R.string.action_settings)
            onClick {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        startActivity(
                            Intent(activity, SettingsActivity::class.java)
                        )
                    }, 200
                )
            }
        }
        menuItem {
            groupId = groupIds[3]
            itemId = R.id.nav_about
            icon = getTintedDrawable(R.drawable.ic_help_white_24dp, textColorPrimary)
            titleRes(R.string.action_about)
            onClick {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        startActivity(
                            Intent(activity, AboutActivity::class.java)
                        )
                    }, 200
                )
            }
        }


        menuItem {
            groupId = groupIds[3]
            icon = getTintedDrawable(R.drawable.ic_more_vert_white_24dp, textColorPrimary)
            titleRes(R.string.more_actions)
            onClick {
                val items = listOf(
                    "Delete Databases" to {
                        MusicDatabase.songsDataBase.close()
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            MusicDatabase.songsDataBase.clearAllTables()
                            MusicDatabase.Metadata.lastUpdateTimestamp = 0
                        }
                    },
                    context.getString(R.string.refresh_database) to {
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            Scanner.refreshDatabase(context, true)
                        }
                    },
                    activity.getString(R.string.action_grant_storage_permission) to {
                        navigateToStorageSetting(activity)
                    },
                    activity.getString(R.string.app_info) to {
                        navigateToAppDetailSetting(activity)
                    },
                    activity.getString(R.string.action_reboot) to {
                        Reboot.reboot(activity)
                    },
                    activity.getString(R.string.search_online) to {
                        activity.startActivity(WebSearchLauncher.launchIntent(activity))
                    },
                )
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.more_actions)
                    .setItems(items.map { it.first }.toTypedArray()) { dialog, index ->
                        dialog.dismiss()
                        items[index].second.invoke()
                    }
                    .show()
                true
            }
        }

        for (id in groupIds) {
            rootMenu.setGroupEnabled(id, true)
            rootMenu.setGroupCheckable(id, false, false)
        }
    }

}