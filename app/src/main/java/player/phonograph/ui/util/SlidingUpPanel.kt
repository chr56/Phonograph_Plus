/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.util

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState

class SlidingUpPanelSwitchHelper {
    fun collapse(slidingUpPanel: SlidingUpPanelLayout?): Boolean {
        with(slidingUpPanel ?: return false) {
            if (panelState != PanelState.COLLAPSED) panelState = PanelState.COLLAPSED
        }
        return true
    }

    fun expand(slidingUpPanel: SlidingUpPanelLayout?): Boolean {
        with(slidingUpPanel ?: return false) {
            if (panelState != PanelState.EXPANDED) panelState = PanelState.EXPANDED
        }
        return true
    }

    fun toggle(slidingUpPanel: SlidingUpPanelLayout?) {
        with(slidingUpPanel ?: return) {
            if (panelState == PanelState.EXPANDED) {
                panelState = PanelState.COLLAPSED
            } else if (panelState == PanelState.COLLAPSED) {
                panelState = PanelState.EXPANDED
            }
        }
    }
}