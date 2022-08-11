/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import android.util.ArrayMap
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference
import player.phonograph.model.pages.PageConfig
import player.phonograph.ui.fragments.home.*

class HomePagerAdapter(fragment: Fragment, var cfg: PageConfig) : FragmentStateAdapter(fragment) {
    val map: MutableMap<Int, WeakReference<AbsPage>> = ArrayMap(cfg.getSize())

    override fun getItemCount(): Int = cfg.getSize()

    override fun createFragment(position: Int): Fragment =
        cfg.getAsPage(position)
            .also { fragment -> map[position] = WeakReference(fragment) } // registry
}

