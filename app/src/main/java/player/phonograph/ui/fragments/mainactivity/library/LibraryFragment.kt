/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("unused")

package player.phonograph.ui.fragments.mainactivity.library

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import chr_56.MDthemer.color.MaterialColor
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.*
import com.afollestad.materialcab.MaterialCab
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.tabs.TabLayout
import player.phonograph.R
import player.phonograph.adapter.MusicLibraryPagerAdapter
import player.phonograph.databinding.FragmentLibraryBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.dialogs.CreatePlaylistDialog
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.SortOrder
import player.phonograph.interfaces.CabHolder
import player.phonograph.loader.SongLoader
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.SearchActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.ui.fragments.mainactivity.library.pager.AbsLibraryPagerRecyclerViewCustomGridSizeFragment
import player.phonograph.ui.fragments.mainactivity.library.pager.AlbumsFragment
import player.phonograph.ui.fragments.mainactivity.library.pager.ArtistsFragment
import player.phonograph.ui.fragments.mainactivity.library.pager.PlaylistsFragment
import player.phonograph.ui.fragments.mainactivity.library.pager.SongsFragment
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PreferenceUtil
import player.phonograph.util.Util.isLandscape

class LibraryFragment :
    AbsMainActivityFragment(), CabHolder, MainActivity.MainActivityFragmentCallbacks, SharedPreferences.OnSharedPreferenceChangeListener, ViewPager.OnPageChangeListener {

    // viewBinding
    private var _viewBinding: FragmentLibraryBinding? = null
    private val binding: FragmentLibraryBinding
        get() = _viewBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        PreferenceUtil.getInstance(requireActivity()).unregisterOnSharedPreferenceChangedListener(this)
        super.onDestroyView()
        binding.pager.removeOnPageChangeListener(this)
        isPopupMenuInited = false
        _viewBinding = null
    }

    private lateinit var pagerAdapter: MusicLibraryPagerAdapter // [setUpViewPager()]

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceUtil.getInstance(requireActivity()).registerOnSharedPreferenceChangedListener(this)

        setupTheme(requireActivity() as MainActivity)
        setupToolbar()
        setUpViewPager()
    }

    private fun setupToolbar() {
        // Todo improve theme engine
        val primaryColor = ThemeColor.primaryColor(requireActivity())
        binding.appbar.setBackgroundColor(primaryColor)
        binding.toolbar.setBackgroundColor(primaryColor)
        binding.toolbar.navigationIcon =
            TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_menu_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor))
            )

        binding.toolbar.setTitleTextColor(
            ToolbarColorUtil.toolbarTitleColor(requireActivity(), primaryColor)
        )
        requireActivity().setTitle(R.string.app_name)

        if (PreferenceUtil.getInstance(requireContext()).fixedTabLayout()) {
            binding.tabs.tabMode = TabLayout.MODE_FIXED
        } else {
            binding.tabs.tabMode = TabLayout.MODE_SCROLLABLE
        }

        (requireActivity() as MainActivity).setSupportActionBar(binding.toolbar)
    }

    private fun setUpViewPager() {
        pagerAdapter = MusicLibraryPagerAdapter(requireActivity(), childFragmentManager)
        binding.pager.adapter = pagerAdapter
        binding.pager.offscreenPageLimit = pagerAdapter.count - 1
        binding.tabs.setupWithViewPager(binding.pager)

        val primaryColor = ThemeColor.primaryColor(requireActivity())
        val normalColor = ToolbarColorUtil.toolbarSubtitleColor(requireActivity(), primaryColor)
        val selectedColor = ToolbarColorUtil.toolbarTitleColor(requireActivity(), primaryColor)

        TabLayoutUtil.setTabIconColors(binding.tabs, normalColor, selectedColor)
        binding.tabs.setTabTextColors(normalColor, selectedColor)
        binding.tabs.setSelectedTabIndicatorColor(ThemeColor.accentColor(requireActivity()))

        updateTabVisibility()

        if (PreferenceUtil.getInstance(requireContext()).rememberLastTab()) {
            binding.pager.currentItem = PreferenceUtil.getInstance(requireContext()).lastPage
        }

        binding.pager.addOnPageChangeListener(this)
    }

    private fun updateTabVisibility() {
        // hide the tab bar when only a single tab is visible
        binding.tabs.visibility = if (pagerAdapter.count == 1) View.GONE else View.VISIBLE
    }

    private val currentFragment: Fragment? get() = pagerAdapter.getFragment(binding.pager.currentItem)
    private val isPlaylistPage: Boolean get() = currentFragment is PlaylistsFragment

    private fun setupTheme(activity: MainActivity) {
        activity.setStatusbarColorAuto()
        activity.setNavigationbarColorAuto()
        activity.setTaskDescriptionColorAuto()
    }

    private var cab: MaterialCab? = null
    override fun openCab(menuRes: Int, callback: MaterialCab.Callback?): MaterialCab {

        cab?.let {
            if (it.isActive) it.finish()
        }

        cab = MaterialCab(mainActivity, R.id.cab_stub)
            .setMenu(menuRes)
            .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
            .setBackgroundColor(
                PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(requireActivity()))
            )
            .start(callback)

        return cab as MaterialCab
    }

    val totalAppBarScrollingRange: Int
        get() = binding.appbar.totalScrollRange

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
        if (isPlaylistPage) { menu.add(0, R.id.action_new_playlist, 0, R.string.new_playlist_title) }

        val currentFragment = currentFragment

        if (currentFragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *> && currentFragment.isAdded()) {
            val gridSizeItem = menu.findItem(R.id.action_grid_size)
            if (isLandscape(resources)) {
                gridSizeItem.setTitle(R.string.action_grid_size_land)
            }
            setUpGridSizeMenu(currentFragment, gridSizeItem.subMenu)
            menu.findItem(R.id.action_colored_footers).isChecked =
                currentFragment.usePalette()
            menu.findItem(R.id.action_colored_footers).isEnabled =
                currentFragment.canUsePalette()
            setUpSortOrderMenu(currentFragment, menu.findItem(R.id.action_sort_order).subMenu)
        } else {
            menu.removeItem(R.id.action_grid_size)
            menu.removeItem(R.id.action_colored_footers)
            menu.removeItem(R.id.action_sort_order)
        }

        MenuTinter.setMenuColor(
            mainActivity, binding.toolbar, menu, MaterialColor.White._1000.asColor
        )
    }
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        MenuTinter.handleOnPrepareOptionsMenu(mainActivity, binding.toolbar)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // TODO: clean up & extract
        if (PreferenceUtil.LIBRARY_CATEGORIES == key) {
            val current = currentFragment
            pagerAdapter.setCategoryInfos(PreferenceUtil.getInstance(requireActivity()).libraryCategoryInfos!!)
            binding.pager.offscreenPageLimit = pagerAdapter.count - 1
            var position = pagerAdapter.getItemPosition(current!!)
            if (position < 0) position = 0
            binding.pager.currentItem = position
            PreferenceUtil.getInstance(requireContext()).lastPage = position
            updateTabVisibility()
        } else if (PreferenceUtil.FIXED_TAB_LAYOUT == key) {
            if (PreferenceUtil.getInstance(requireContext()).fixedTabLayout()) {
                binding.tabs.tabMode = TabLayout.MODE_FIXED
            } else {
                binding.tabs.tabMode = TabLayout.MODE_SCROLLABLE
            }
        }
    }

    private fun setUpGridSizeMenu(fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>, gridSizeMenu: SubMenu) {
        when (fragment.gridSize) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked = true
            2 -> gridSizeMenu.findItem(R.id.action_grid_size_2).isChecked = true
            3 -> gridSizeMenu.findItem(R.id.action_grid_size_3).isChecked = true
            4 -> gridSizeMenu.findItem(R.id.action_grid_size_4).isChecked = true
            5 -> gridSizeMenu.findItem(R.id.action_grid_size_5).isChecked = true
            6 -> gridSizeMenu.findItem(R.id.action_grid_size_6).isChecked = true
            7 -> gridSizeMenu.findItem(R.id.action_grid_size_7).isChecked = true
            8 -> gridSizeMenu.findItem(R.id.action_grid_size_8).isChecked = true
        }
        val maxGridSize = fragment.maxGridSize
        if (maxGridSize < 8) gridSizeMenu.findItem(R.id.action_grid_size_8).isVisible = false
        if (maxGridSize < 7) gridSizeMenu.findItem(R.id.action_grid_size_7).isVisible = false
        if (maxGridSize < 6) gridSizeMenu.findItem(R.id.action_grid_size_6).isVisible = false
        if (maxGridSize < 5) gridSizeMenu.findItem(R.id.action_grid_size_5).isVisible = false
        if (maxGridSize < 4) gridSizeMenu.findItem(R.id.action_grid_size_4).isVisible = false
        if (maxGridSize < 3) gridSizeMenu.findItem(R.id.action_grid_size_3).isVisible = false
    }

    private fun handleGridSizeMenuItem(fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>, item: MenuItem): Boolean {
        var gridSize = 0
        when (item.itemId) {
            R.id.action_grid_size_1 -> gridSize = 1
            R.id.action_grid_size_2 -> gridSize = 2
            R.id.action_grid_size_3 -> gridSize = 3
            R.id.action_grid_size_4 -> gridSize = 4
            R.id.action_grid_size_5 -> gridSize = 5
            R.id.action_grid_size_6 -> gridSize = 6
            R.id.action_grid_size_7 -> gridSize = 7
            R.id.action_grid_size_8 -> gridSize = 8
        }
        if (gridSize > 0) {
            item.isChecked = true
            fragment.setAndSaveGridSize(gridSize)
            binding.toolbar.menu.findItem(R.id.action_colored_footers).isEnabled = fragment.canUsePalette()
            return true
        }
        return false
    }

    private fun setUpSortOrderMenu(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        sortOrderMenu: SubMenu
    ) {
        val currentSortOrder = fragment.sortOrder
        sortOrderMenu.clear()
        when (fragment) {
            is AlbumsFragment -> {
                sortOrderMenu.add(0, R.id.action_album_sort_order_asc, 0, R.string.sort_order_a_z).isChecked =
                    currentSortOrder == SortOrder.AlbumSortOrder.ALBUM_A_Z
                sortOrderMenu.add(0, R.id.action_album_sort_order_desc, 1, R.string.sort_order_z_a).isChecked =
                    currentSortOrder == SortOrder.AlbumSortOrder.ALBUM_Z_A
                sortOrderMenu.add(0, R.id.action_album_sort_order_artist, 2, R.string.sort_order_artist).isChecked =
                    currentSortOrder == SortOrder.AlbumSortOrder.ALBUM_ARTIST
                sortOrderMenu.add(0, R.id.action_album_sort_order_year, 3, R.string.sort_order_year).isChecked =
                    currentSortOrder == SortOrder.AlbumSortOrder.ALBUM_YEAR
            }
            is ArtistsFragment -> {
                sortOrderMenu.add(0, R.id.action_artist_sort_order_asc, 0, R.string.sort_order_a_z).isChecked =
                    currentSortOrder == SortOrder.ArtistSortOrder.ARTIST_A_Z
                sortOrderMenu.add(0, R.id.action_artist_sort_order_desc, 1, R.string.sort_order_z_a).isChecked =
                    currentSortOrder == SortOrder.ArtistSortOrder.ARTIST_Z_A
            }
            is SongsFragment -> {
                sortOrderMenu.add(0, R.id.action_song_sort_order_asc, 0, R.string.sort_order_a_z).isChecked =
                    currentSortOrder == SortOrder.SongSortOrder.SONG_A_Z
                sortOrderMenu.add(0, R.id.action_song_sort_order_desc, 1, R.string.sort_order_z_a).isChecked =
                    currentSortOrder == SortOrder.SongSortOrder.SONG_Z_A
                sortOrderMenu.add(0, R.id.action_song_sort_order_artist, 2, R.string.sort_order_artist).isChecked =
                    currentSortOrder == SortOrder.SongSortOrder.SONG_ARTIST
                sortOrderMenu.add(0, R.id.action_song_sort_order_album, 3, R.string.sort_order_album).isChecked =
                    currentSortOrder == SortOrder.SongSortOrder.SONG_ALBUM
                sortOrderMenu.add(0, R.id.action_song_sort_order_year, 4, R.string.sort_order_year).isChecked =
                    currentSortOrder == SortOrder.SongSortOrder.SONG_YEAR
                sortOrderMenu.add(0, R.id.action_song_sort_order_date_added, 5, R.string.sort_order_date_added).isChecked =
                    currentSortOrder == SortOrder.SongSortOrder.SONG_DATE
            }
        }
        sortOrderMenu.setGroupCheckable(0, true, true)
    }

    private fun handleSortOrderMenuItem(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        item: MenuItem
    ): Boolean {
        var sortOrder: String? = null
        when (fragment) {
            is AlbumsFragment -> {
                when (item.itemId) {
                    R.id.action_album_sort_order_asc -> sortOrder = SortOrder.AlbumSortOrder.ALBUM_A_Z
                    R.id.action_album_sort_order_desc -> sortOrder = SortOrder.AlbumSortOrder.ALBUM_Z_A
                    R.id.action_album_sort_order_artist -> sortOrder = SortOrder.AlbumSortOrder.ALBUM_ARTIST
                    R.id.action_album_sort_order_year -> sortOrder = SortOrder.AlbumSortOrder.ALBUM_YEAR
                }
            }
            is ArtistsFragment -> {
                when (item.itemId) {
                    R.id.action_artist_sort_order_asc -> sortOrder = SortOrder.ArtistSortOrder.ARTIST_A_Z
                    R.id.action_artist_sort_order_desc -> sortOrder = SortOrder.ArtistSortOrder.ARTIST_Z_A
                }
            }
            is SongsFragment -> {
                when (item.itemId) {
                    R.id.action_song_sort_order_asc -> sortOrder = SortOrder.SongSortOrder.SONG_A_Z
                    R.id.action_song_sort_order_desc -> sortOrder = SortOrder.SongSortOrder.SONG_Z_A
                    R.id.action_song_sort_order_artist -> sortOrder = SortOrder.SongSortOrder.SONG_ARTIST
                    R.id.action_song_sort_order_album -> sortOrder = SortOrder.SongSortOrder.SONG_ALBUM
                    R.id.action_song_sort_order_year -> sortOrder = SortOrder.SongSortOrder.SONG_YEAR
                    R.id.action_song_sort_order_date_added -> sortOrder = SortOrder.SongSortOrder.SONG_DATE
                }
            }
        }
        if (sortOrder != null) {
            item.isChecked = true
            fragment.setAndSaveSortOrder(sortOrder)
            return true
        }
        return false
    }

    private lateinit var popupMenu: PopupWindow
//    private lateinit var popupView: View
    private var _bindingPopup: PopupWindowMainBinding? = null
    private val popup get() = _bindingPopup!!
    private var isPopupMenuInited: Boolean = false

    private fun initPopup() {
//        popupView = LayoutInflater.from(mainActivity).inflate(R.layout.popup_window_main, null, false)
        _bindingPopup = PopupWindowMainBinding.inflate(LayoutInflater.from(mainActivity))

        val width = mainActivity.window.decorView.width / 2
//        popupMenu = PopupWindow(popupView, width, AppBarLayout.LayoutParams.WRAP_CONTENT, true)
        popupMenu = PopupWindow(popup.root, width, AppBarLayout.LayoutParams.WRAP_CONTENT, true)
        popupMenu.setBackgroundDrawable(ColorDrawable(mainActivity.resources.getColor(R.color.md_white_1000)))

        isPopupMenuInited = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentFragment = currentFragment
        if (currentFragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
            if (item.itemId == R.id.action_colored_footers) {
                item.isChecked = !item.isChecked
                currentFragment.setAndSaveUsePalette(item.isChecked)
                return true
            }
            if (handleGridSizeMenuItem(currentFragment, item)) { return true }
            if (handleSortOrderMenuItem(currentFragment, item)) { return true }
        }
        when (item.itemId) {
            R.id.action_shuffle_all -> {
                MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(requireActivity()), true)
                return true
            }
            R.id.action_new_playlist -> {
                CreatePlaylistDialog.createEmpty().show(childFragmentManager, "CREATE_PLAYLIST")
                return true
            }
            R.id.action_search -> {
                startActivity(Intent(mainActivity, SearchActivity::class.java))
                return true
            }
            R.id.action_main_popup_window_menu -> {
                if (!isPopupMenuInited) initPopup()

                val yOffset = (mainActivity.supportActionBar?.height ?: binding.toolbar.height) +
                    (mainActivity.findViewById<player.phonograph.views.StatusBarView>(R.id.status_bar)?.height ?: 8)

                popupMenu.showAtLocation(binding.toolbar.rootView, Gravity.TOP or Gravity.END, 0, yOffset)

                val fragment = currentFragment ?: return true

                if (fragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *> && fragment.isAdded) {
                    initSortOrder(popupMenu, popup, fragment)
                    initGridSize(popupMenu, popup, fragment)
                } else {
                    disableSortOrder(popup)
                    disableGridSize(popup)
                }

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initSortOrder(popupWindow: PopupWindow, popup: PopupWindowMainBinding, fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
        popup.sortOrderBasic.visibility = View.VISIBLE
        popup.textSortOrderBasic.visibility = View.VISIBLE
        popup.sortOrderContent.visibility = View.VISIBLE
        popup.textSortOrderContent.visibility = View.VISIBLE

        val currentSortOrder = fragment.sortOrder
        when (currentSortOrder) {
            SortOrder.SongSortOrder.SONG_Z_A, SortOrder.SongSortOrder.SONG_YEAR_REVERT, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT, SortOrder.SongSortOrder.SONG_DATE_REVERT,
            SortOrder.AlbumSortOrder.ALBUM_Z_A, SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS_REVERT, /*SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT,*/
            SortOrder.ArtistSortOrder.ARTIST_Z_A, SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS_REVERT, SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS_REVERT ->
                popup.sortOrderBasic.check(R.id.sort_order_z_a)
            else -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
        }

        popup.sortOrderContent.clearCheck()

        // TODO implement content Sort Order
        when (fragment) {
            is SongsFragment -> {
                popup.sortOrderSong.visibility = View.VISIBLE
                popup.sortOrderAlbum.visibility = View.VISIBLE
                popup.sortOrderArtist.visibility = View.VISIBLE
                popup.sortOrderYear.visibility = View.VISIBLE
                popup.sortOrderDateAdded.visibility = View.VISIBLE
                popup.sortOrderDateModified.visibility = View.VISIBLE
                popup.sortOrderDuration.visibility = View.VISIBLE
                when (currentSortOrder) {
                    SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> popup.sortOrderContent.check(R.id.sort_order_song)
                    SortOrder.SongSortOrder.SONG_ALBUM -> popup.sortOrderContent.check(R.id.sort_order_album)
                    SortOrder.SongSortOrder.SONG_ARTIST -> popup.sortOrderContent.check(R.id.sort_order_artist)
                    SortOrder.SongSortOrder.SONG_YEAR, SortOrder.SongSortOrder.SONG_YEAR_REVERT -> popup.sortOrderContent.check(R.id.sort_order_year)
                    SortOrder.SongSortOrder.SONG_DATE, SortOrder.SongSortOrder.SONG_DATE_REVERT -> popup.sortOrderContent.check(R.id.sort_order_date_added)
                    SortOrder.SongSortOrder.SONG_DATE_MODIFIED, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT -> popup.sortOrderContent.check(R.id.sort_order_date_modified)
                    SortOrder.SongSortOrder.SONG_DURATION -> popup.sortOrderContent.check(R.id.sort_order_duration)
                    else -> { popup.sortOrderContent.clearCheck() }
                }
            }
            is AlbumsFragment -> {
                popup.sortOrderAlbum.visibility = View.VISIBLE
                popup.sortOrderArtist.visibility = View.VISIBLE
                popup.sortOrderYear.visibility = View.VISIBLE
//                popup.sortOrder_.visibility = View.VISIBLE
                when (currentSortOrder) {
                    SortOrder.AlbumSortOrder.ALBUM_Z_A, SortOrder.AlbumSortOrder.ALBUM_A_Z -> popup.sortOrderContent.check(R.id.sort_order_song)
                    SortOrder.AlbumSortOrder.ALBUM_YEAR, SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT -> popup.sortOrderContent.check(R.id.sort_order_year)
                    SortOrder.AlbumSortOrder.ALBUM_ARTIST -> popup.sortOrderContent.check(R.id.sort_order_artist)
                    SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS, SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS_REVERT -> { /* todo */ }
                    else -> { popup.sortOrderContent.clearCheck() }
                }
            }
            is ArtistsFragment -> {
                popup.sortOrderArtist.visibility = View.VISIBLE
//                popup.sortOrder_.visibility = View.GONE
//                popup.sortOrder_.visibility = View.GONE
                when (currentSortOrder) {
                    SortOrder.ArtistSortOrder.ARTIST_A_Z, SortOrder.ArtistSortOrder.ARTIST_Z_A -> popup.sortOrderContent.check(R.id.sort_order_artist)
                    SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS, SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS_REVERT -> { /* todo */ }
                    SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS, SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS_REVERT -> { /* todo */ }
                    else -> { popup.sortOrderContent.clearCheck() }
                }
            }
        }
    }
    private fun disableSortOrder(popup: PopupWindowMainBinding) {
        popup.sortOrderBasic.visibility = View.GONE
        popup.sortOrderBasic.clearCheck()
        popup.textSortOrderBasic.visibility = View.GONE

        popup.sortOrderContent.visibility = View.GONE
        popup.sortOrderContent.clearCheck()
        popup.textSortOrderContent.visibility = View.GONE
    }

    private fun initGridSize(popupWindow: PopupWindow, popup: PopupWindowMainBinding, fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
        popup.textGridSize.visibility = View.VISIBLE
        popup.gridSize.visibility = View.VISIBLE
    }
    private fun disableGridSize(popup: PopupWindowMainBinding) {
        popup.textGridSize.visibility = View.GONE
        popup.gridSize.clearCheck()
        popup.gridSize.visibility = View.GONE
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
    override fun onPageSelected(position: Int) { PreferenceUtil.getInstance(requireActivity()).lastPage = position }
    override fun onPageScrollStateChanged(state: Int) { }

    fun addOnAppBarOffsetChangedListener(onOffsetChangedListener: OnOffsetChangedListener) {
        binding.appbar.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    fun removeOnAppBarOffsetChangedListener(onOffsetChangedListener: OnOffsetChangedListener) {
        binding.appbar.removeOnOffsetChangedListener(onOffsetChangedListener)
    }
    override fun handleBackPress(): Boolean {
        if (cab != null && cab!!.isActive) {
            cab!!.finish()
            return true
        }
        return false
    }

    companion object {
        fun newInstance(): LibraryFragment = LibraryFragment()
    }
}
