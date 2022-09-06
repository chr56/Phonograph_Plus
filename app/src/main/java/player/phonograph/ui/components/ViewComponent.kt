/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components

import android.view.LayoutInflater

interface ViewComponent<C, M> {
    /**
     * create a blank view component
     */
    fun inflate(rootContainer: C, layoutInflater: LayoutInflater?)

    /**
     * load view model
     */
    fun loadData(model: M)

    /**
     * destroy
     */
    fun destroy()
}
