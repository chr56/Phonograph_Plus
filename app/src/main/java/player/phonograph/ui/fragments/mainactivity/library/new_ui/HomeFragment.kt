/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
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
import lib.phonograph.cab.*
import player.phonograph.R
import player.phonograph.adapter.HomePagerAdapter
import player.phonograph.adapter.PAGERS
import player.phonograph.adapter.PageConfig
import player.phonograph.databinding.FragmentHomeBinding
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.SearchActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PreferenceUtil
import java.lang.ref.WeakReference

class HomeFragment : AbsMainActivityFragment(), MainActivity.MainActivityFragmentCallbacks, MultiSelectionCabProvider {

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

    private fun readConfig(): PageConfig { // todo
        return PageConfig(
            HashMap<Int, String>(1)
                .also {
                    it[0] = PAGERS.EMPTY
                    it[1] = PAGERS.SONG
                }
        )
    }

    private val cfg: PageConfig get() = readConfig()

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
        if (multiSelectionCab != null && multiSelectionCab!!.status == CabStatus.STATUS_AVAILABLE) {
            multiSelectionCab!!.destroy()
        }
        return false
    }

    override fun handleFloatingActionButtonPress(): Boolean {
        return false
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }

    /**
     *     the popup window for [AbsDisplayPage]
     */
    var displayPopup: WeakReference<PopupWindow?> = WeakReference(null)

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

    override fun createCab(
        menuRes: Int,
        showCallback: ShowCallback,
        selectCallback: SelectCallback,
        destroyCallback: DestroyCallback
    ): MultiSelectionCab {

        multiSelectionCab?.let {
            if (it.status == CabStatus.STATUS_AVAILABLE || it.status == CabStatus.STATUS_ACTIVE) it.destroy()
        }

        multiSelectionCab = createMultiSelectionCab(mainActivity, R.id.cab_stub, R.id.multi_selection_cab) {

            val primaryColor = ThemeColor.primaryColor(requireActivity())
            backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(primaryColor)

            val textColor = MaterialColorHelper.getSecondaryTextColor(mainActivity, ColorUtil.isColorLight(primaryColor))

            inflateMenuRes(menuRes)

            titleTextColor = textColor

            closeDrawable = AppCompatResources.getDrawable(mainActivity, R.drawable.ic_close_white_24dp)!!.also {
                it.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(textColor, BlendModeCompat.SRC_IN)
            }

            onCreate(showCallback)
            onSelection(selectCallback)
            onDestroy(destroyCallback)
        }

        return multiSelectionCab!!
    }
    override fun showCab() {
        multiSelectionCab?.let { cab ->
            binding.toolbar.visibility = View.INVISIBLE
            cab.refresh()
            cab.show()
        }
    }
    override fun dismissCab() {
        multiSelectionCab?.destroy()
        binding.toolbar.visibility = View.VISIBLE
    }
}
