/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.util

import androidx.core.view.MenuProvider
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem


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