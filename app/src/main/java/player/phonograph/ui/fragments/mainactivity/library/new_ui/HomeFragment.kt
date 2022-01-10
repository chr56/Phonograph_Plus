/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.viewpager2.widget.ViewPager2
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import player.phonograph.R
import player.phonograph.adapter.HomePagerAdapter
import player.phonograph.adapter.PAGERS
import player.phonograph.adapter.PagerConfig
import player.phonograph.databinding.FragmentHomeBinding
import player.phonograph.ui.activities.MainActivity
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
        binding.toolbar.setTitleTextColor(primaryColor)
        binding.toolbar.title = requireActivity().getString(R.string.app_name)
        mainActivity.setSupportActionBar(binding.toolbar)

        binding.tabs.tabMode = if (PreferenceUtil.getInstance(requireContext()).fixedTabLayout()) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
        binding.tabs.setTabTextColors(secondaryTextColor, primaryTextColor)
        binding.tabs.setSelectedTabIndicatorColor(accentColor)
    }

    private val cfg: PagerConfig = PagerConfig(
        HashMap<Int, String>(1)
            .also { it[0] = PAGERS.SONG }
    )

    private lateinit var pagerAdapter: HomePagerAdapter

    private fun setUpViewPager() {
        pagerAdapter = HomePagerAdapter(this, cfg)

        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.pager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            when (cfg.get(i)) {
                PAGERS.SONG -> tab.text = getString(R.string.songs)
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
}

//    SharedPreferences.OnSharedPreferenceChangeListener, ViewPager.OnPageChangeListener
