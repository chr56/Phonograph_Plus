/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.adapter.HomePagerAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.databinding.FragmentHomeBinding
import lib.phonograph.misc.menuProvider
import player.phonograph.model.pages.PageConfig
import player.phonograph.model.pages.Pages
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.SearchActivity
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.AbsPage
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.settings.SettingFlowStore
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.viewpager2.widget.ViewPager2
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : AbsMainActivityFragment(), MainActivity.MainActivityFragmentCallbacks {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingFlowStore(requireContext())
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                store.homeTabConfigJsonString.distinctUntilChanged().collect {
                    whenStarted { reloadPages() }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                store.fixedTabLayout.distinctUntilChanged().collect {
                    whenStarted { reloadPages() }
                }
            }
        }
    }

    private var _viewBinding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (DEBUG) Log.v(
            "Metrics",
            "${System.currentTimeMillis().mod(10000000)} HomeFragment.onCreateView()"
        )
        _viewBinding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setUpViewPager()

        if (DEBUG) Log.v(
            "Metrics",
            "${System.currentTimeMillis().mod(10000000)} HomeFragment.onViewCreated()"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.pager.unregisterOnPageChangeCallback(pageChangeListener)
        _viewBinding = null
    }

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
            tabMode = if (Setting.instance.fixedTabLayout) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
            setTabTextColors(secondaryTextColor, primaryTextColor)
            setSelectedTabIndicatorColor(accentColor)
        }

        requireActivity().addMenuProvider(
            menuProvider(this::setupMenu),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        cab = createToolbarCab(mainActivity, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)
    }

    lateinit var cab: ToolbarCab
    lateinit var cabController: MultiSelectionCabController

    private fun readConfig(): PageConfig = HomeTabConfig.homeTabConfig

    private val cfg: PageConfig get() = readConfig()

    private lateinit var pagerAdapter: HomePagerAdapter

    private fun setUpViewPager() {
        pagerAdapter = HomePagerAdapter(this, cfg)

        binding.pager.apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            adapter = pagerAdapter
            offscreenPageLimit = if (pagerAdapter.itemCount > 1) pagerAdapter.itemCount - 1 else 1
            if (Setting.instance.rememberLastTab) {
                val last = Setting.instance.lastPage
                currentItem = last
                mainActivity.switchPageChooserTo(last)
            }
            registerOnPageChangeCallback(pageChangeListener)
        }

        TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            tab.text = Pages.getDisplayName(cfg.get(i), requireContext())
        }.attach()
        updateTabVisibility()
    }

    private val currentPage: AbsPage?
        get() = pagerAdapter.map[binding.pager.currentItem]?.get()

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            Setting.instance.lastPage = position
            mainActivity.switchPageChooserTo(position)
            super.onPageSelected(position)
        }
    }

    override fun handleBackPress(): Boolean {
        return if (cabController.dismiss()) true else {
            currentPage?.onBackPress() ?: false
        }
    }

    override fun requestSelectPage(page: Int) {
        try {
            binding.pager.currentItem = page
        } catch (e: Exception) {
            ErrorNotification.postErrorNotification(e, "Failed to select page $page")
        }
    }

    /**
     *     the popup window for [AbsDisplayPage]
     */

    val popup: ListOptionsPopup by lazy { ListOptionsPopup(mainActivity) }

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

    fun addOnAppBarOffsetChangedListener(
        onOffsetChangedListener: AppBarLayout.OnOffsetChangedListener,
    ) {
        binding.appbar.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    fun removeOnAppBarOffsetChangedListener(
        onOffsetChangedListener: AppBarLayout.OnOffsetChangedListener,
    ) {
        binding.appbar.removeOnOffsetChangedListener(onOffsetChangedListener)
    }


    private suspend fun reloadPages() = withContext(Dispatchers.Main) {
        var oldPosition = binding.pager.currentItem
        if (oldPosition < 0) oldPosition = 0

        val current = pagerAdapter.cfg.get(oldPosition)

        var newPosition = -1

        readConfig().tabList.forEachIndexed { index, page ->
            if (page == current) {
                newPosition = index
            }
        }
        if (newPosition < 0) newPosition = 0

        setUpViewPager()
        binding.pager.currentItem = newPosition
    }

    private suspend fun reloadTabsLayout() = withContext(Dispatchers.Main) {
        binding.tabs.tabMode =
            if (Setting.instance.fixedTabLayout) {
                TabLayout.MODE_FIXED
            } else {
                TabLayout.MODE_SCROLLABLE
            }
    }

    val totalAppBarScrollingRange: Int get() = binding.appbar.totalScrollRange

    val totalHeaderHeight: Int
        get() =
            totalAppBarScrollingRange + if (binding.tabs.visibility == View.VISIBLE) binding.tabs.height else 0

    private fun updateTabVisibility() {
        // hide the tab bar when only a single tab is visible
        binding.tabs.visibility = if (pagerAdapter.itemCount == 1) View.GONE else View.VISIBLE
    }

    private fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(mainActivity, resId)?.also {
            it.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(primaryTextColor, BlendModeCompat.SRC_IN)
        }
    }

    private val primaryColor by lazy(LazyThreadSafetyMode.NONE) { ThemeColor.primaryColor(requireActivity()) }
    private val accentColor by lazy(LazyThreadSafetyMode.NONE) { ThemeColor.accentColor(requireActivity()) }
    private val primaryTextColor by lazy(LazyThreadSafetyMode.NONE) {
        mainActivity.primaryTextColor(primaryColor)
    }
    private val secondaryTextColor by lazy(LazyThreadSafetyMode.NONE) {
        mainActivity.primaryTextColor(primaryColor)
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }
}
