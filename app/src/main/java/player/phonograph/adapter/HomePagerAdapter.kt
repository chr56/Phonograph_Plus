/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import player.phonograph.ui.fragments.mainactivity.library.new_ui.EmptyPage
import java.lang.ref.WeakReference

class HomePagerAdapter(fragment: Fragment, var cfg: PagerConfig) : FragmentStateAdapter(fragment) {

    private val parentFragmentReference: WeakReference<Fragment> = WeakReference(fragment)

    override fun getItemCount(): Int {
        return cfg.getSize()
    }

    override fun createFragment(position: Int): Fragment {
        val fragmentClass =
            when (cfg.get(position)) {
//                PAGERS.SONG -> SongPage::class.java
                else -> EmptyPage::class.java
            }
        var fragment: Fragment? = null

        try {
            fragment = fragmentClass.getConstructor().newInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return fragment ?: EmptyPage()
    }
}

class PagerConfig(var tabMap: MutableMap<Int, String>) {

    fun getSize(): Int = tabMap.size

    fun get(index: Int): String {
        return tabMap[index] ?: "EMPTY"
    }
}
interface PAGERS {
    companion object {
        const val EMPTY = "EMPTY"
        const val SONG = "SONG"
    }
}
