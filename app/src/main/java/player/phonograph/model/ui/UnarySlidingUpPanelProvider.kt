/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.ui

import android.view.View

interface UnarySlidingUpPanelProvider {
    fun requestToSetAntiDragView(view: View?): Boolean
    fun requestToSetScrollableView(view: View?): Boolean
}