/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import lib.phonograph.cab.*
import player.phonograph.R
import player.phonograph.adapter.HomePagerAdapter
import player.phonograph.adapter.PAGERS
import player.phonograph.adapter.PageConfig
import player.phonograph.databinding.FragmentHomeBinding
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.SearchActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.MaterialColorHelper

class HomeFragment : AbsMainActivityFragment(), MainActivity.MainActivityFragmentCallbacks, SharedPreferences.OnSharedPreferenceChangeListener, MultiSelectionCabProvider {

    private var _viewBinding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setUpViewPager()

        Setting.instance.registerOnSharedPreferenceChangedListener(this)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        binding.pager.unregisterOnPageChangeCallback(pageChangeListener)
        Setting.instance.unregisterOnSharedPreferenceChangedListener(this)
        multiSelectionCab?.destroy()
        multiSelectionCab = null
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
    }

    private fun readConfig(): PageConfig = Setting.instance.homeTabConfig

    private val cfg: PageConfig get() = readConfig()

    private lateinit var pagerAdapter: HomePagerAdapter

    private fun setUpViewPager() {
        pagerAdapter = HomePagerAdapter(this, cfg)

        binding.pager.apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            adapter = pagerAdapter
            offscreenPageLimit = if (pagerAdapter.itemCount > 1) pagerAdapter.itemCount - 1 else 1
            if (Setting.instance.rememberLastTab) {
                currentItem = Setting.instance.lastPage
            }
            registerOnPageChangeCallback(pageChangeListener)
        }

        TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            tab.text = PAGERS.getDisplayName(cfg.get(i), requireContext())
        }.attach()
        updateTabVisibility()
    }

    private val currentPage: AbsPage?
        get() = pagerAdapter.map[binding.pager.currentItem]?.get()

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            Setting.instance.lastPage = position
            super.onPageSelected(position)
        }
    }

    override fun handleBackPress(): Boolean {
        if (multiSelectionCab != null && multiSelectionCab!!.status == CabStatus.STATUS_ACTIVE) {
            dismissCab()
            return true
        } else if (multiSelectionCab != null) {
            multiSelectionCab!!.destroy()
            multiSelectionCab = null
        }
        return currentPage?.onBackPress() ?: false
    }

    /**
     *     the popup window for [AbsDisplayPage]
     */

    val popup: ListOptionsPopup by lazy { ListOptionsPopup(mainActivity) }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.add(0, R.id.action_search, 0, R.string.action_search).also {
            it.icon = getDrawable(R.drawable.ic_search_white_24dp)
            it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(mainActivity, SearchActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

    val totalAppBarScrollingRange: Int get() = binding.appbar.totalScrollRange

    val totalHeaderHeight: Int
        get() =
            totalAppBarScrollingRange + if (binding.tabs.visibility == View.VISIBLE) binding.tabs.height else 0

    private var multiSelectionCab: MultiSelectionCab? = null
    override fun getCab(): MultiSelectionCab? = multiSelectionCab

    override fun deployCab(
        menuRes: Int,
        initCallback: InitCallback?,
        showCallback: ShowCallback?,
        selectCallback: SelectCallback?,
        hideCallback: HideCallback?,
        destroyCallback: DestroyCallback?,
    ): MultiSelectionCab {

        val cfg: CabCfg = {

            backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(primaryColor)
            titleTextColor = primaryTextColor

            closeDrawable = mainActivity.getTintedDrawable(R.drawable.ic_close_white_24dp, primaryTextColor)!!

            this.menuRes = menuRes

            onInit(initCallback)
            onShow(showCallback)
            onSelection(selectCallback)
            onHide(hideCallback)
            onClose { dismissCab() }
            onDestroy(destroyCallback)
        }

        if (multiSelectionCab == null) multiSelectionCab =
            createMultiSelectionCab(mainActivity, R.id.cab_stub, R.id.multi_selection_cab, cfg)
        else {
            multiSelectionCab!!.applyCfg = cfg
            multiSelectionCab!!.refresh()
        }

        return multiSelectionCab!!
    }

    override fun showCab() {
        multiSelectionCab?.let { cab ->
            binding.toolbar.visibility = View.INVISIBLE
            binding.tabs.visibility = View.GONE
            binding.pager.isUserInputEnabled = false
            cab.refresh()
            cab.show()
        }
    }

    override fun dismissCab() {
        multiSelectionCab?.hide()
        binding.toolbar.visibility = View.VISIBLE
        binding.tabs.visibility = View.VISIBLE
        binding.pager.isUserInputEnabled = true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Setting.HOME_TAB_CONFIG -> {
                var oldPosition = binding.pager.currentItem
                if (oldPosition < 0) oldPosition = 0

                val current = pagerAdapter.cfg.get(oldPosition)

                var newPosition = -1

                readConfig().tabMap.forEach { entry: Map.Entry<Int, String> ->
                    if (entry.value == current) {
                        newPosition = entry.key
                        return@forEach
                    }
                }
                if (newPosition < 0) newPosition = 0

                setUpViewPager()
                binding.pager.currentItem = newPosition
            }
            Setting.FIXED_TAB_LAYOUT -> {
                binding.tabs.tabMode =
                    if (Setting.instance.fixedTabLayout)
                        TabLayout.MODE_FIXED
                    else
                        TabLayout.MODE_SCROLLABLE
            }
        }
    }

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
        MaterialColorHelper.getPrimaryTextColor(
            mainActivity, ColorUtil.isColorLight(primaryColor)
        )
    }
    private val secondaryTextColor by lazy(LazyThreadSafetyMode.NONE) {
        MaterialColorHelper.getSecondaryTextColor(
            mainActivity, ColorUtil.isColorLight(primaryColor)
        )
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }
}
