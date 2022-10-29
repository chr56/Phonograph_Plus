/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.misc

import androidx.core.view.MenuProvider
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

fun menuProvider(block: (Menu) -> Unit): MenuProvider {
    return object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = block.invoke(menu)

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }
}

fun menuProvider(block: (Menu) -> Unit, callback: (MenuItem) -> Boolean): MenuProvider {
    return object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = block.invoke(menu)

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = callback(menuItem)
    }
}