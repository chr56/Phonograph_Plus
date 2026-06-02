/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.debug
import player.phonograph.logMetrics
import player.phonograph.ui.modules.main.MainFragment
import androidx.fragment.app.Fragment

abstract class AbsPage : Fragment() {

    protected val mainFragment: MainFragment
        get() = (parentFragment as? MainFragment)
            ?: throw IllegalStateException("${this::class.simpleName} hasn't attach to MainFragment")

    override fun onResume() {
        super.onResume()
        debug { logMetrics("AbsDisplayPage.onResume()") }
    }

}
