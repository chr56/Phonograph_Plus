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
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import com.google.android.material.tabs.TabLayout
import player.phonograph.R
import player.phonograph.adapter.MusicLibraryPagerAdapter
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
        val primaryTextColor = MaterialColorHelper.getPrimaryTextColor(
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

        binding.tabs.tabMode = if (PreferenceUtil.getInstance(requireContext())
                .fixedTabLayout()
        ) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
        mainActivity.setSupportActionBar(binding.toolbar)
    }

    private fun setUpViewPager() {
        TODO("Not yet implemented")
    }


    private lateinit var pagerAdapter: MusicLibraryPagerAdapter // [setUpViewPager()]

    override fun handleBackPress(): Boolean {
        // todo cab
        return false
    }

    override fun handleFloatingActionButtonPress(): Boolean {
        return false
    }
}

//    SharedPreferences.OnSharedPreferenceChangeListener, ViewPager.OnPageChangeListener
