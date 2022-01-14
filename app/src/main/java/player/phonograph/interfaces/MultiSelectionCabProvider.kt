/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.interfaces

import lib.phonograph.cab.*

interface MultiSelectionCabProvider {
    fun deployCab(menuRes: Int, initCallback: InitCallback?, showCallback: ShowCallback?, selectCallback: SelectCallback?, hideCallback: HideCallback?, destroyCallback: DestroyCallback?): MultiSelectionCab
    fun getCab(): MultiSelectionCab?
    fun showCab()
    fun dismissCab()
}
