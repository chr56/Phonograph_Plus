/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.util

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import player.phonograph.foundation.concurrent.lifecycleScopeOrNewOne
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.resource.interpreter
import player.phonograph.ui.theme.withColorFilter
import androidx.annotation.ColorInt
import androidx.core.view.MenuProvider
import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun inflateMenuEntries(context: Context, menu: Menu, entries: List<ActionEntry>): Unit =
    context.attach(menu) {
        for (entry in entries) {
            val title = entry.semantic.interpreter().text(context.resources)
            when (entry) {
                is ActionEntry.ToolbarEntry   -> menuItem(title = title) {
                    onClick {
                        context.lifecycleScopeOrNewOne().launch(Dispatchers.Main.immediate) {
                            entry.action()
                        }
                        true
                    }
                }

                is ActionEntry.GroupedEntries -> {
                    val subMenu = submenu(title) {}
                    inflateMenuEntries(context, subMenu, entry.entries)
                }
            }
        }
    }

fun inflateToolbarEntries(
    context: Context,
    menu: Menu,
    @ColorInt iconTint: Int,
    entries: List<ActionEntry>,
): Unit =
    context.attach(menu) {
        for (entry in entries) {
            val title = entry.semantic.interpreter().text(context.resources)
            when (entry) {
                is ActionEntry.ToolbarEntry   -> menuItem(title = title) {
                    icon = entry.semantic.interpreter().icon(context.resources)?.withColorFilter(iconTint)
                    when (entry.visibility) {
                        ActionEntry.ToolbarEntry.NEVER   -> showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        ActionEntry.ToolbarEntry.ALWAYS  -> showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                        ActionEntry.ToolbarEntry.IF_ROOM -> showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    }
                    onClick {
                        context.lifecycleScopeOrNewOne().launch(Dispatchers.Main.immediate) {
                            entry.action()
                        }
                        true
                    }
                }

                is ActionEntry.GroupedEntries -> {
                    val subMenu = submenu(title) {}
                    inflateToolbarEntries(context, subMenu, iconTint, entry.entries)
                }
            }
        }
    }

fun menuProvider(block: (Menu) -> Unit, callback: (MenuItem) -> Boolean): MenuProvider {
    return object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = block.invoke(menu)

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = callback(menuItem)
    }
}

fun menuProvider(block: (Menu) -> Unit): MenuProvider {
    return object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = block.invoke(menu)

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }
}