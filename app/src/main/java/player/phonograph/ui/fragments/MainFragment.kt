/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import lib.phonograph.misc.menuProvider
import mt.pref.accentColor
import mt.pref.primaryColor
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.FragmentHomeBinding
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.mechanism.setting.PageConfig
import player.phonograph.model.pages.Pages
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.AbsPage
import player.phonograph.ui.modules.search.SearchActivity
import player.phonograph.util.debug
import player.phonograph.util.logMetrics
import player.phonograph.util.reportError
import player.phonograph.util.theme.getTintedDrawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View
import android.view.ViewGroup
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainFragment : Fragment(), MainActivity.MainActivityFragmentCallbacks {

    val mainActivity: MainActivity get() = requireActivity() as MainActivity

    private var _viewBinding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        debug { logMetrics("MainFragment.onCreateView()") }
        _viewBinding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        binding.pager.registerOnPageChangeCallback(pageChangeListener)

        debug { logMetrics("MainFragment.onViewCreated()") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.pager.unregisterOnPageChangeCallback(pageChangeListener)
        _viewBinding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readSettings()
    }

    //region Settings
    private fun readSettings() {
        val store = Setting(requireContext())
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {

                val homeTabConfigFlow = store[Keys.homeTabConfigJsonString].flow.distinctUntilChanged()

                homeTabConfigFlow.collect { raw ->
                    val pageConfig: PageConfig = HomeTabConfig.parseHomeTabConfig(raw)

                    val rememberLastTab = lifecycleScope.async(Dispatchers.IO) {
                        Setting(requireContext())[Keys.rememberLastTab].flowData()
                    }.await()
                    val lastPage = lifecycleScope.async(Dispatchers.IO) {
                        Setting(requireContext())[Keys.lastPage].flowData()
                    }.await()

                    withStarted {
                        loadPages(pageConfig, if (rememberLastTab) lastPage else -1)
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                store[Keys.fixedTabLayout].flow.distinctUntilChanged().collect { fixedTabLayout ->
                    withStarted {
                        binding.tabs.tabMode = if (fixedTabLayout) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
                    }
                }
            }
        }
    }
    //endregion

    //region Toolbar

    private fun setupToolbar() {
        binding.appbar.setBackgroundColor(primaryColor)
        with(binding.toolbar) {
            setBackgroundColor(primaryColor)
            navigationIcon = getDrawable(R.drawable.ic_menu_white_24dp)!!
            setTitleTextColor(primaryTextColor)
            title = requireActivity().getString(R.string.app_name)
        }

        mainActivity.setSupportActionBar(binding.toolbar)
        with(binding.tabs) {
            setTabTextColors(secondaryTextColor, primaryTextColor)
            setSelectedTabIndicatorColor(accentColor)
        }

        requireActivity().addMenuProvider(
            menuProvider(this::setupMenu),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun setupMenu(menu: Menu) {
        attach(requireContext(), menu) {
            menuItem {
                itemId = R.id.action_search
                titleRes(R.string.action_search)
                icon = mainActivity.getTintedDrawable(R.drawable.ic_search_white_24dp, primaryTextColor)
                showAsActionFlag = SHOW_AS_ACTION_ALWAYS
                onClick {
                    startActivity(Intent(mainActivity, SearchActivity::class.java))
                    true
                }
            }
        }
    }
    //endregion

    //region Pages
    private var pagerAdapter: HomePagerAdapter? = null

    private fun loadPages(pageConfig: PageConfig, preferredPosition: Int) {

        val oldAdapter: HomePagerAdapter? = pagerAdapter

        val targetPosition = if (oldAdapter != null) {
            // from old adapter
            val oldPosition = binding.pager.currentItem.coerceAtLeast(0)
            val currentPage = oldAdapter.pageConfig[oldPosition]
            val newPosition = pageConfig.tabs.indexOf(currentPage)
            newPosition.coerceIn(0, pageConfig.size - 1)
        } else if (preferredPosition > -1) {
            // from Argument
            preferredPosition.coerceIn(0, pageConfig.size - 1)
        } else {
            // first page by default
            0
        }

        setupViewPager(pageConfig)
        binding.pager.setCurrentItem(targetPosition, false)
        mainActivity.switchPageChooserTo(targetPosition)
    }

    private fun setupViewPager(homeTabConfig: PageConfig) {
        // Adapter
        val homePagerAdapter = HomePagerAdapter(this, homeTabConfig)
        binding.pager.apply {
            adapter = homePagerAdapter
            offscreenPageLimit = min(homePagerAdapter.itemCount, 3)
        }
        pagerAdapter = homePagerAdapter

        // TabLayout
        TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, index: Int ->
            tab.text = Pages.getDisplayName(homeTabConfig[index], requireContext())
        }.attach()
        binding.tabs.visibility = if (homePagerAdapter.itemCount == 1) View.GONE else View.VISIBLE
    }


    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            mainActivity.switchPageChooserTo(position)
            mainActivity.lifecycleScope.launch(Dispatchers.IO) {
                Setting(App.instance)[Keys.lastPage].edit { position }
            }
            super.onPageSelected(position)
        }
    }
    //endregion

    //region Popup & AppBar

    /**
     *  the popup window for [AbsPage]
     */
    val popup: ListOptionsPopup by lazy { ListOptionsPopup(mainActivity) }

    fun addOnAppBarOffsetChangedListener(onOffsetChangedListener: OnOffsetChangedListener) {
        binding.appbar.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    fun removeOnAppBarOffsetChangedListener(onOffsetChangedListener: OnOffsetChangedListener) {
        binding.appbar.removeOnOffsetChangedListener(onOffsetChangedListener)
    }

    val totalAppBarScrollingRange: Int get() = binding.appbar.totalScrollRange

    val totalHeaderHeight: Int
        get() = totalAppBarScrollingRange + if (binding.tabs.visibility == View.VISIBLE) binding.tabs.height else 0
    //endregion

    //region Interactivity
    override fun requestSelectPage(page: Int) {
        try {
            binding.pager.currentItem = page
        } catch (e: Exception) {
            reportError(e, "HomeFragment", "Failed to select page $page")
        }
    }

    override fun handleBackPress(): Boolean = pagerAdapter?.fetch(binding.pager.currentItem)?.onBackPress() ?: false
    //endregion

    //region Utils
    private fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(mainActivity, resId)?.also {
            it.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(primaryTextColor, BlendModeCompat.SRC_IN)
        }
    }

    private val primaryColor by lazy(LazyThreadSafetyMode.NONE) { mainActivity.primaryColor() }
    private val accentColor by lazy(LazyThreadSafetyMode.NONE) { mainActivity.accentColor() }
    private val primaryTextColor by lazy(LazyThreadSafetyMode.NONE) { mainActivity.primaryTextColor(primaryColor) }
    private val secondaryTextColor by lazy(LazyThreadSafetyMode.NONE) { mainActivity.primaryTextColor(primaryColor) }
    //endregion

    companion object {
        fun newInstance(): MainFragment = MainFragment()
    }

    private class HomePagerAdapter(fragment: Fragment, var pageConfig: PageConfig) : FragmentStateAdapter(fragment) {

        private val current: MutableMap<Int, WeakReference<AbsPage>> = ArrayMap(pageConfig.size)

        override fun getItemCount(): Int = pageConfig.size

        override fun createFragment(position: Int): Fragment =
            pageConfig.initiate(position).also { fragment -> current[position] = WeakReference(fragment) } // registry

        fun fetch(index: Int): AbsPage? = current[index]?.get()
    }
}