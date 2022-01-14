/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.interfaces

import lib.phonograph.cab.ShowCallback
import lib.phonograph.cab.DestroyCallback
import lib.phonograph.cab.MultiSelectionCab
import lib.phonograph.cab.SelectCallback

interface MultiSelectionCabProvider {
    fun createCab(menuRes: Int, showCallback: ShowCallback, selectCallback: SelectCallback, destroyCallback: DestroyCallback): MultiSelectionCab
    fun getCab(): MultiSelectionCab?
    fun showCab()
    fun dismissCab()
}
