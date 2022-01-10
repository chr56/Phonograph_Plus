/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import player.phonograph.ui.fragments.mainactivity.library.pager.AbsLibraryPagerFragment
import java.lang.Exception
import java.lang.ref.WeakReference

class HomePagerAdapter(fragment: Fragment, var cfg: PagerConfig) : FragmentStateAdapter(fragment) {

    private val parentFragmentReference: WeakReference<Fragment> = WeakReference(fragment)

    override fun getItemCount(): Int {
        return cfg.getSize()
    }

    override fun createFragment(position: Int): Fragment {
        val fragmentClass = cfg.get(position).fragmentClass
        var fragment: Fragment? = null
        try {
            fragment = fragmentClass.getConstructor().newInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fragment ?: Fragment()
    }
}

class PagerConfig(var list: MutableList<TabPair>) {
    init {
        list.sortedBy { it.index }
    }

    fun getSize(): Int = list.size

    fun get(index: Int): TabPair {
        list.forEach { tabPair ->
            if (tabPair.index == index) return@get tabPair
        }
        return list[0]
    }

    data class TabPair(
        var index: Int,
        var tab: TabLayout.Tab,
        var fragmentClass: Class<out AbsLibraryPagerFragment>
    )
}
