/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
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
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.SearchActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper
import java.lang.ref.WeakReference

class HomeFragment : AbsMainActivityFragment(), MainActivity.MainActivityFragmentCallbacks, SharedPreferences.OnSharedPreferenceChangeListener, MultiSelectionCabProvider {

    private var _viewBinding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pager.unregisterOnPageChangeCallback(pageChangeListener)
        _viewBinding = null
        multiSelectionCab?.destroy()
        multiSelectionCab = null

        Setting.instance.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo move these to main activity
        mainActivity.setStatusbarColorAuto()
        mainActivity.setNavigationbarColorAuto()
        mainActivity.setTaskDescriptionColorAuto()

        setupToolbar()
        setUpViewPager()

        Setting.instance.registerOnSharedPreferenceChangedListener(this)
    }
    private fun setupToolbar() {
        val primaryColor = ThemeColor.primaryColor(requireActivity())
        val accentColor = ThemeColor.accentColor(requireActivity())
        val primaryTextColor = MaterialColorHelper.getPrimaryTextColor(
            requireActivity(),
            ColorUtil.isColorLight(primaryColor)
        )
        val secondaryTextColor = MaterialColorHelper.getSecondaryTextColor(
            requireActivity(),
            ColorUtil.isColorLight(primaryColor)
        )

        val navigationIcon =
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_menu_white_24dp)!!
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            primaryTextColor,
            BlendModeCompat.SRC_IN
        )
        navigationIcon.colorFilter = colorFilter

        binding.appbar.setBackgroundColor(primaryColor)
        binding.toolbar.navigationIcon = navigationIcon
        binding.toolbar.setBackgroundColor(primaryColor)
        binding.toolbar.setTitleTextColor(primaryTextColor)
        binding.toolbar.title = requireActivity().getString(R.string.app_name)
        mainActivity.setSupportActionBar(binding.toolbar)

        binding.tabs.tabMode = if (Setting.instance.fixedTabLayout) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
        binding.tabs.setTabTextColors(secondaryTextColor, primaryTextColor)
        binding.tabs.setSelectedTabIndicatorColor(accentColor)
    }

    private fun readConfig(): PageConfig = Setting.instance.homeTabConfig

    private val cfg: PageConfig get() = readConfig()

    private lateinit var pagerAdapter: HomePagerAdapter

    private fun setUpViewPager() {
        pagerAdapter = HomePagerAdapter(this, cfg)

        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.pager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            tab.text = PAGERS.getDisplayName(cfg.get(i), requireContext())
        }.attach()
        binding.pager.offscreenPageLimit = if (pagerAdapter.itemCount> 1) pagerAdapter.itemCount - 1 else 1
        updateTabVisibility()
        if (Setting.instance.rememberLastTab) {
            binding.pager.currentItem = Setting.instance.lastPage
        }
        binding.pager.registerOnPageChangeCallback(pageChangeListener)
    }

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
        return false
    }

    /**
     *     the popup window for [AbsDisplayPage]
     */
    var displayPopup: WeakReference<PopupWindow?> = WeakReference(null)
    var displayPopupView: WeakReference<PopupWindowMainBinding?> = WeakReference(null)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        val primaryColor = ThemeColor.primaryColor(mainActivity)
        val primaryTextColor = MaterialColorHelper.getPrimaryTextColor(mainActivity, ColorUtil.isColorLight(primaryColor))

        val f = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(primaryTextColor, BlendModeCompat.SRC_IN)

        val search = menu.add(0, R.id.action_search, 0, R.string.action_search)
        search.icon =
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_search_white_24dp)
                .also { it?.colorFilter = f }

        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
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

    fun addOnAppBarOffsetChangedListener(onOffsetChangedListener: AppBarLayout.OnOffsetChangedListener) {
        binding.appbar.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    fun removeOnAppBarOffsetChangedListener(onOffsetChangedListener: AppBarLayout.OnOffsetChangedListener) {
        binding.appbar.removeOnOffsetChangedListener(onOffsetChangedListener)
    }

    val totalAppBarScrollingRange: Int get() = binding.appbar.totalScrollRange

    val totalHeaderHeight: Int get() =
        totalAppBarScrollingRange + if (binding.tabs.visibility == View.VISIBLE) binding.tabs.height else 0

    private var multiSelectionCab: MultiSelectionCab? = null
    override fun getCab(): MultiSelectionCab? = multiSelectionCab

    override fun deployCab(
        menuRes: Int,
        initCallback: InitCallback?,
        showCallback: ShowCallback?,
        selectCallback: SelectCallback?,
        hideCallback: HideCallback?,
        destroyCallback: DestroyCallback?
    ): MultiSelectionCab {

        val cfg: CabCfg = {
            val primaryColor = ThemeColor.primaryColor(requireActivity())
            val textColor = Color.WHITE

            backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(primaryColor)
            titleTextColor = textColor

            closeDrawable = AppCompatResources.getDrawable(mainActivity, R.drawable.ic_close_white_24dp)!!.also {
                it.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(textColor, BlendModeCompat.SRC_IN)
            }

            this.menuRes = menuRes

            onInit(initCallback)
            onShow(showCallback)
            onSelection(selectCallback)
            onHide(hideCallback)
            onClose { dismissCab() }
            onDestroy(destroyCallback)
        }

        if (multiSelectionCab == null) multiSelectionCab = createMultiSelectionCab(mainActivity, R.id.cab_stub, R.id.multi_selection_cab, cfg)
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

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }
}
