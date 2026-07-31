/*
 *  Copyright (c) 2026 chr_56
 */

package player.phonograph.ui.theme

import player.phonograph.R
import android.view.View

/**
 * Holder for System Bars stub views.
 */
class SystemBarsStubs {
    var statusbar: View? = null
    var statusbarOverlay: View? = null
    var navigationbar: View? = null

    var statusbarViewId: Int = R.id.status_bar
    var statusbarOverlayViewId: Int = R.id.status_bar_overlay
    var navigationbarViewId: Int = R.id.navigation_bar

    fun registerViews(
        statusbar: View? = null, statusbarViewId: Int = 0,
        statusbarOverlay: View? = null, statusbarOverlayViewId: Int = 0,
        navigationbar: View? = null, navigationbarViewId: Int = 0,
    ) {
        this.statusbar = statusbar
        this.statusbarOverlay = statusbarOverlay
        this.navigationbar = navigationbar

        if (statusbarViewId != 0) this.statusbarViewId = statusbarViewId
        if (statusbarOverlayViewId != 0) this.statusbarOverlayViewId = statusbarOverlayViewId
        if (navigationbarViewId != 0) this.navigationbarViewId = navigationbarViewId
    }

    fun unregisterViews() {
        this.statusbar = null
        this.statusbarOverlay = null
        this.navigationbar = null
    }

    fun statusbar(view: View? = null): View? = statusbar ?: view?.findViewById(statusbarViewId)
    fun statusbarOverlay(view: View? = null): View? = statusbarOverlay ?: view?.findViewById(statusbarOverlayViewId)
    fun navigationbar(view: View? = null): View? = navigationbar ?: view?.findViewById(navigationbarViewId)

}