/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package lib.phonograph.misc

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