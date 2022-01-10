/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.viewpager2.widget.ViewPager2
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import player.phonograph.R
import player.phonograph.adapter.HomePagerAdapter
import player.phonograph.adapter.PAGERS
import player.phonograph.adapter.PagerConfig
import player.phonograph.databinding.FragmentHomeBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.SearchActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.util.PreferenceUtil

class HomeFragment : AbsMainActivityFragment(), MainActivity.MainActivityFragmentCallbacks {

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
        _viewBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo move these to main activity
        mainActivity.setStatusbarColorAuto()
        mainActivity.setNavigationbarColorAuto()
        mainActivity.setTaskDescriptionColorAuto()

        setupToolbar()
        setUpViewPager()
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

        binding.tabs.tabMode = if (PreferenceUtil.getInstance(requireContext()).fixedTabLayout()) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
        binding.tabs.setTabTextColors(secondaryTextColor, primaryTextColor)
        binding.tabs.setSelectedTabIndicatorColor(accentColor)
    }

    private fun readConfig(): PagerConfig { // todo
        return PagerConfig(
            HashMap<Int, String>(1)
                .also {
                    it[0] = PAGERS.EMPTY
                    it[1] = PAGERS.SONG
                }
        )
    }

    private val cfg: PagerConfig get() = readConfig()

    private lateinit var pagerAdapter: HomePagerAdapter

    private fun setUpViewPager() {
        pagerAdapter = HomePagerAdapter(this, cfg)

        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.pager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            when (cfg.get(i)) {
                PAGERS.SONG -> tab.text = getString(R.string.songs)
                PAGERS.EMPTY -> tab.text = "TODO"
            }
        }.attach()
    }

    override fun handleBackPress(): Boolean {
        // todo cab
        return false
    }

    override fun handleFloatingActionButtonPress(): Boolean {
        return false
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }

    // all pages share one popup this all can be re-used
    lateinit var popupMenu: PopupWindow
        private set

    private var _bindingPopup: PopupWindowMainBinding? = null
    val popup get() = _bindingPopup!!

    var isPopupMenuInited: Boolean = false
        private set

    fun initPopup() {
        _bindingPopup = PopupWindowMainBinding.inflate(LayoutInflater.from(mainActivity))

        val accentColor = ThemeColor.accentColor(mainActivity)
        popup.textGridSize.setTextColor(accentColor)
        popup.textSortOrderBasic.setTextColor(accentColor)
        popup.textSortOrderContent.setTextColor(accentColor)

        popup.actionColoredFooters.buttonTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled), intArrayOf()
            ),
            intArrayOf(
                accentColor, ThemeColor.textColorSecondary(mainActivity)
            )
        )

        val csl = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                accentColor, ThemeColor.textColorSecondary(mainActivity)
            )
        )

        for (i in 0 until popup.gridSize.childCount) {
            (popup.gridSize.getChildAt(i) as RadioButton).buttonTintList = csl
        }
        for (i in 0 until popup.sortOrderContent.childCount) {
            (popup.sortOrderContent.getChildAt(i) as RadioButton).buttonTintList = csl
        }
        for (i in 0 until popup.sortOrderBasic.childCount) {
            (popup.sortOrderBasic.getChildAt(i) as RadioButton).buttonTintList = csl
        }

        popupMenu = PopupWindow(popup.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        val backgroundColor = mainActivity.getColor(
            when (PreferenceUtil.getInstance(mainActivity).generalTheme) {
                R.style.Theme_Phonograph_Auto -> R.color.cardBackgroundColor
                R.style.Theme_Phonograph_Light -> R.color.md_white_1000
                R.style.Theme_Phonograph_Black -> R.color.md_black_1000
                R.style.Theme_Phonograph_Dark -> R.color.md_grey_800
                else -> R.color.md_grey_700
            }
        )
        popupMenu.setBackgroundDrawable(ColorDrawable(backgroundColor))

        popupMenu.animationStyle = android.R.style.Animation_Dialog

        isPopupMenuInited = true
    }

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
}

//    SharedPreferences.OnSharedPreferenceChangeListener, ViewPager.OnPageChangeListener
