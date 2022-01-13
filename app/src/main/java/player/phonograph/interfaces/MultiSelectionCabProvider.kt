/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.interfaces

import com.afollestad.materialcab.CreateCallback
import com.afollestad.materialcab.DestroyCallback
import com.afollestad.materialcab.SelectCallback
import lib.phonograph.cab.MultiSelectionCab

interface MultiSelectionCabProvider {
    fun createCab(menuRes: Int, createCallback: CreateCallback, selectCallback: SelectCallback, destroyCallback: DestroyCallback): MultiSelectionCab
    fun getCab(): MultiSelectionCab?
    fun dismissCab()
}