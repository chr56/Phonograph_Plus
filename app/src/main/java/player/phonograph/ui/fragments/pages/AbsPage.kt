/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.fragments.MainFragment

// todo no more AbsMusicServiceFragment
abstract class AbsPage : AbsMusicServiceFragment() {

    protected val hostFragment: MainFragment
        get() = parentFragment?.let { it as MainFragment } ?: throw IllegalStateException("${this::class.simpleName} hasn't attach to MainFragment")
    open fun onBackPress(): Boolean = false
}
