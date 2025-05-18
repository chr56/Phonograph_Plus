/*
 * Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import player.phonograph.R
import player.phonograph.databinding.FragmentHomeBinding
import player.phonograph.foundation.reportError
import player.phonograph.model.pages.Pages
import player.phonograph.model.pages.PagesConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingObserver
import player.phonograph.ui.modules.main.pages.AbsPage
import player.phonograph.ui.modules.main.pages.AlbumPage
import player.phonograph.ui.modules.main.pages.ArtistPage
import player.phonograph.ui.modules.main.pages.EmptyPage
import player.phonograph.ui.modules.main.pages.FilesPage
import player.phonograph.ui.modules.main.pages.FoldersPage
import player.phonograph.ui.modules.main.pages.GenrePage
import player.phonograph.ui.modules.main.pages.PlaylistPage
import player.phonograph.ui.modules.main.pages.SongPage
import player.phonograph.ui.modules.popup.ListOptionsPopup
import player.phonograph.ui.modules.search.SearchActivity
import player.phonograph.util.debug
import player.phonograph.util.logMetrics
import player.phonograph.util.observe
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.ui.menuProvider
import util.theme.color.primaryTextColor
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainFragment : Fragment() {

    val mainActivity: MainActivity get() = requireActivity() as MainActivity

    private val drawerViewModel: MainDrawerViewModel by viewModels({ mainActivity })

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
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val settingObserver = SettingObserver(requireContext(), lifecycleScope)
                settingObserver.collect(Keys.homeTabConfig) { pagesConfig ->
                    val rememberLastTab = settingObserver.blocking(Keys.rememberLastTab)
                    val lastPage = settingObserver.blocking(Keys.lastPage)
                    withStarted {
                        loadPages(pagesConfig, if (rememberLastTab) lastPage else -1)
                    }
                }
                settingObserver.collect(Keys.fixedTabLayout) { fixedTabLayout ->
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
                icon = getTintedDrawable(R.drawable.ic_search_white_24dp, primaryTextColor)
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

    private fun loadPages(pagesConfig: PagesConfig, preferredPosition: Int) {

        val oldAdapter: HomePagerAdapter? = pagerAdapter

        val targetPosition = if (oldAdapter != null) {
            // from old adapter
            val oldPosition = binding.pager.currentItem.coerceAtLeast(0)
            val currentPage = oldAdapter.pagesConfig[oldPosition]
            val newPosition = pagesConfig.tabs.indexOf(currentPage)
            newPosition.coerceIn(0, pagesConfig.size - 1)
        } else if (preferredPosition > -1) {
            // from Argument
            preferredPosition.coerceIn(0, pagesConfig.size - 1)
        } else {
            // first page by default
            0
        }

        setupViewPager(pagesConfig)
        binding.pager.setCurrentItem(targetPosition, false)
        drawerViewModel.switchPageTo(targetPosition)
    }

    private fun setupViewPager(homeTabConfig: PagesConfig) {

        observe(drawerViewModel.selectedPage) { page ->
            try {
                binding.pager.currentItem = page
            } catch (e: Exception) {
                reportError(e, "MainFragment", "Failed to select page $page")
            }
        }

        // Adapter
        val homePagerAdapter = HomePagerAdapter(this, homeTabConfig)
        binding.pager.apply {
            adapter = homePagerAdapter
            offscreenPageLimit = 1
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
            drawerViewModel.switchPageTo(position)
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

    private class HomePagerAdapter(fragment: Fragment, var pagesConfig: PagesConfig) : FragmentStateAdapter(fragment) {

        private val current: MutableMap<Int, WeakReference<AbsPage>> = ArrayMap(pagesConfig.size)

        override fun getItemCount(): Int = pagesConfig.size

        override fun createFragment(position: Int): Fragment =
            createPage(pagesConfig[position]).also { fragment ->
                current[position] = WeakReference(fragment)
            } // registry

        private fun createPage(type: String): AbsPage {
            return when (type) {
                Pages.SONG     -> SongPage()
                Pages.ALBUM    -> AlbumPage()
                Pages.ARTIST   -> ArtistPage()
                Pages.PLAYLIST -> PlaylistPage()
                Pages.GENRE    -> GenrePage()
                Pages.FOLDER   -> FoldersPage()
                Pages.FILES    -> FilesPage()
                else           -> EmptyPage()
            }
        }
    }
}