/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import androidx.fragment.app.Fragment

abstract class AbsPage : Fragment() {

    protected val hostFragment: HomeFragment
        get() = parentFragment?.let { it as HomeFragment } ?: throw IllegalStateException("${this::class.simpleName} hasn't attach to HomeFragment")

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }
}
