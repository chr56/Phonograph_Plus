/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("unused")

package player.phonograph.ui.fragments.mainactivity.library

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.*
import com.afollestad.materialcab.MaterialCab
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.tabs.TabLayout
import player.phonograph.R
import player.phonograph.adapter.MusicLibraryPagerAdapter
import player.phonograph.database.mediastore.SongColumns
import player.phonograph.databinding.FragmentLibraryBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.dialogs.CreatePlaylistDialog
import player.phonograph.helper.SortOrder
import player.phonograph.interfaces.CabHolder
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
import java.util.regex.Pattern

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
//    private val isPlaylistPage: Boolean get() = currentFragment is PlaylistsFragment

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
        val primaryColor = ThemeColor.primaryColor(mainActivity)

        if (currentFragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
            val popup = menu.add(0, R.id.action_main_popup_window_menu, 0, R.string.action_settings)
            popup.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            popup.icon = TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_sort_variant_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor))
            )
        }

        val search = menu.add(0, R.id.action_search, 1, R.string.action_search)
        search.icon = TintHelper.createTintedDrawable(
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_search_white_24dp),
            MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor))
        )
        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
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
                configPopup(popupMenu, popup, fragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private lateinit var popupMenu: PopupWindow
    private var _bindingPopup: PopupWindowMainBinding? = null
    private val popup get() = _bindingPopup!!
    private var isPopupMenuInited: Boolean = false

    private fun initPopup() {
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

    private fun configPopup(popupWindow: PopupWindow, popup: PopupWindowMainBinding, fragment: Fragment) {
        if (fragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *> && fragment.isAdded) {
            initSortOrder(popupMenu, popup, fragment)
            initGridSize(popupMenu, popup, fragment)
            initColorFooter(popupMenu, popup, fragment)
            popupMenu.setOnDismissListener {
                handlePopupMenuDismiss(popup, popupMenu, fragment)
            }
        } else {
            disableSortOrder(popup)
            disableGridSize(popup)
            disableColorFooter(popup)
        }
    }

    private fun initSortOrder(popupWindow: PopupWindow, popup: PopupWindowMainBinding, fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
        popup.sortOrderBasic.visibility = View.VISIBLE
        popup.textSortOrderBasic.visibility = View.VISIBLE
        popup.sortOrderContent.visibility = View.VISIBLE
        popup.textSortOrderContent.visibility = View.VISIBLE

        for (i in 0 until popup.sortOrderContent.childCount) {
            val view: View? = popup.sortOrderContent.getChildAt(i)
            view?.visibility = View.GONE
        }

        val currentSortOrder = fragment.sortOrder
        Log.d(this::class.simpleName, "READ:$currentSortOrder")

        when (currentSortOrder) {
            SortOrder.SongSortOrder.SONG_Z_A, SortOrder.AlbumSortOrder.ALBUM_Z_A, SortOrder.ArtistSortOrder.ARTIST_Z_A,
            SortOrder.SongSortOrder.SONG_DURATION_REVERT, SortOrder.AlbumSortOrder.ALBUM_ARTIST_REVERT,
            SortOrder.SongSortOrder.SONG_YEAR_REVERT, SortOrder.SongSortOrder.SONG_DATE_REVERT, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT,
            -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
            else
            -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
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

                currentSortOrder?.let {
                    val p = it.split(Pattern.compile(" "), 2)
                    if (p.isNotEmpty() && p.size >= 2) {
                        Log.v("LibraryFragment", "${p[0]}    ${p[1]}")
                        when (p[0]) {

//                        SongColumns.ID ->
//                        SongColumns.PATH ->
//                        SongColumns.SIZE ->
//                        SongColumns.DISPLAY_NAME ->
                            SongColumns.DATE_ADDED -> popup.sortOrderContent.check(R.id.sort_order_date_added)
                            SongColumns.DATE_MODIFIED -> popup.sortOrderContent.check(R.id.sort_order_date_modified)
                            SongColumns.TITLE -> popup.sortOrderContent.check(R.id.sort_order_song)
//                        SongColumns.ALBUM_ID ->
                            SongColumns.ALBUM_NAME -> popup.sortOrderContent.check(R.id.sort_order_album)
//                        SongColumns.ARTIST_ID ->
                            SongColumns.ARTIST_NAME -> popup.sortOrderContent.check(R.id.sort_order_artist)
                            SongColumns.YEAR -> popup.sortOrderContent.check(R.id.sort_order_year)
                            SongColumns.DURATION -> popup.sortOrderContent.check(R.id.sort_order_duration)
//                        SongColumns.TRACK_NUMBER ->
                        }
                        when (p[1]) {
                            "ASC" -> popup.sortOrderContent.check(R.id.sort_order_a_z)
                            "DECS" -> popup.sortOrderContent.check(R.id.sort_order_z_a)
                        }
                    }
                }
                // todo migrate to new database
//                when (currentSortOrder) {
//                    SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> popup.sortOrderContent.check(R.id.sort_order_song)
//                    SortOrder.SongSortOrder.SONG_ALBUM, SortOrder.SongSortOrder.SONG_ALBUM_REVERT -> popup.sortOrderContent.check(R.id.sort_order_album)
//                    SortOrder.SongSortOrder.SONG_ARTIST, SortOrder.SongSortOrder.SONG_ARTIST_REVERT -> popup.sortOrderContent.check(R.id.sort_order_artist)
//                    SortOrder.SongSortOrder.SONG_YEAR, SortOrder.SongSortOrder.SONG_YEAR_REVERT -> popup.sortOrderContent.check(R.id.sort_order_year)
//                    SortOrder.SongSortOrder.SONG_DATE, SortOrder.SongSortOrder.SONG_DATE_REVERT -> popup.sortOrderContent.check(R.id.sort_order_date_added)
//                    SortOrder.SongSortOrder.SONG_DATE_MODIFIED, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT -> popup.sortOrderContent.check(R.id.sort_order_date_modified)
//                    SortOrder.SongSortOrder.SONG_DURATION, SortOrder.SongSortOrder.SONG_DURATION_REVERT -> popup.sortOrderContent.check(R.id.sort_order_duration)
//                    else -> { popup.sortOrderContent.clearCheck() }
//                }
            }
            is AlbumsFragment -> {
                popup.sortOrderAlbum.visibility = View.VISIBLE
                popup.sortOrderArtist.visibility = View.VISIBLE
                popup.sortOrderYear.visibility = View.VISIBLE
                when (currentSortOrder) {
                    SortOrder.AlbumSortOrder.ALBUM_Z_A, SortOrder.AlbumSortOrder.ALBUM_A_Z -> popup.sortOrderContent.check(R.id.sort_order_album)
                    SortOrder.AlbumSortOrder.ALBUM_YEAR, SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT -> popup.sortOrderContent.check(R.id.sort_order_year)
                    SortOrder.AlbumSortOrder.ALBUM_ARTIST, SortOrder.AlbumSortOrder.ALBUM_ARTIST_REVERT -> popup.sortOrderContent.check(R.id.sort_order_artist)
                    else -> { popup.sortOrderContent.clearCheck() }
                }
            }
            is ArtistsFragment -> {
                popup.sortOrderArtist.visibility = View.VISIBLE
                when (currentSortOrder) {
                    SortOrder.ArtistSortOrder.ARTIST_A_Z, SortOrder.ArtistSortOrder.ARTIST_Z_A -> popup.sortOrderContent.check(R.id.sort_order_artist)
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
        if (isLandscape(resources)) popup.textGridSize.text = resources.getText(R.string.action_grid_size_land)

        val current = fragment.gridSize
        val max = fragment.maxGridSize
        for (i in 0 until max) {
            popup.gridSize.getChildAt(i).visibility = View.VISIBLE
        }

        popup.gridSize.clearCheck()
        (popup.gridSize.getChildAt(current - 1) as RadioButton).isChecked = true
    }
    private fun disableGridSize(popup: PopupWindowMainBinding) {
        popup.textGridSize.visibility = View.GONE
        popup.gridSize.clearCheck()
        popup.gridSize.visibility = View.GONE
    }

    private fun initColorFooter(popupWindow: PopupWindow, popup: PopupWindowMainBinding, fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
        popup.actionColoredFooters.visibility = View.VISIBLE
        popup.actionColoredFooters.isChecked = fragment.usePalette()
        popup.actionColoredFooters.isEnabled = fragment.canUsePalette()
    }
    private fun disableColorFooter(popup: PopupWindowMainBinding) {
        popup.actionColoredFooters.visibility = View.GONE
    }

    private fun handlePopupMenuDismiss(popup: PopupWindowMainBinding, popupWindow: PopupWindow, fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
        // saving sort order

        val basicSelected = popup.sortOrderBasic.checkedRadioButtonId
        val contentSelected = popup.sortOrderContent.checkedRadioButtonId

        val sortOrderSelected: String =
            when (fragment) {
                //
                is SongsFragment -> {
                    // todo migrate to new database
                    val p = PreferenceUtil.getInstance(requireContext())
                    val c = when (contentSelected) {
                        R.id.sort_order_song -> /* /* p.sortOrderSongColumn =*/*/ SongColumns.TITLE
                        R.id.sort_order_album -> /* p.sortOrderSongColumn =*/ SongColumns.ALBUM_NAME
                        R.id.sort_order_artist -> /* p.sortOrderSongColumn =*/ SongColumns.ARTIST_NAME
                        R.id.sort_order_year -> /* p.sortOrderSongColumn =*/ SongColumns.YEAR
                        R.id.sort_order_date_added -> /* p.sortOrderSongColumn =*/ SongColumns.DATE_ADDED
                        R.id.sort_order_date_modified -> /* p.sortOrderSongColumn =*/ SongColumns.DATE_MODIFIED
                        R.id.sort_order_duration -> /* p.sortOrderSongColumn =*/ SongColumns.DURATION
                        else -> ""
                    }
                    if (c.isNotEmpty()) p.sortOrderSongColumn = c
                    val o = when (basicSelected) {
                        R.id.sort_order_a_z -> /* p.sortOrderSongOrientation =*/ true
                        R.id.sort_order_z_a -> /* p.sortOrderSongOrientation =*/ false
                        else -> { true }
                    }
                    p.sortOrderSongOrientation = o
                    fragment.setAndSaveSortOrder("$c $o")
                    Log.d(this::class.simpleName, "$c $o")
                    "" // empty

//                    when (contentSelected) {
//                        R.id.sort_order_song ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_A_Z
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_Z_A
//                                else -> ""
//                            }
//                        R.id.sort_order_album ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_ALBUM
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_ALBUM_REVERT
//                                else -> ""
//                            }
//                        R.id.sort_order_artist ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_ARTIST
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_ARTIST_REVERT
//                                else -> ""
//                            }
//                        R.id.sort_order_year ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_YEAR
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_YEAR_REVERT
//                                else -> ""
//                            }
//                        R.id.sort_order_date_added ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_DATE
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_DATE_REVERT
//                                else -> ""
//                            }
//                        R.id.sort_order_date_modified ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_DATE_MODIFIED
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT
//                                else -> ""
//                            }
//                        R.id.sort_order_duration ->
//                            when (basicSelected) {
//                                R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_DURATION
//                                R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_DURATION_REVERT
//                                else -> ""
//                            }
//                        else -> ""
//                    }
                }
                //
                is AlbumsFragment -> {
                    when (contentSelected) {
                        R.id.sort_order_album ->
                            when (basicSelected) {
                                R.id.sort_order_a_z -> SortOrder.AlbumSortOrder.ALBUM_A_Z
                                R.id.sort_order_z_a -> SortOrder.AlbumSortOrder.ALBUM_Z_A
                                else -> ""
                            }
                        R.id.sort_order_year ->
                            when (basicSelected) {
                                R.id.sort_order_a_z -> SortOrder.AlbumSortOrder.ALBUM_YEAR
                                R.id.sort_order_z_a -> SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT
                                else -> ""
                            }
                        R.id.sort_order_artist ->
                            when (basicSelected) {
                                R.id.sort_order_a_z -> SortOrder.AlbumSortOrder.ALBUM_ARTIST
                                R.id.sort_order_z_a -> SortOrder.AlbumSortOrder.ALBUM_ARTIST_REVERT
                                else -> ""
                            }
                        else -> ""
                    }
                }
                //
                is ArtistsFragment -> {
                    when (contentSelected) {
                        R.id.sort_order_artist ->
                            when (basicSelected) {
                                R.id.sort_order_a_z -> SortOrder.ArtistSortOrder.ARTIST_A_Z
                                R.id.sort_order_z_a -> SortOrder.ArtistSortOrder.ARTIST_Z_A
                                else -> ""
                            }
                        else -> ""
                    }
                }
                else -> ""
            } // end when(fragment)

        if (sortOrderSelected.isNotBlank()) {
            Log.d(this::class.simpleName, "WRITE:$sortOrderSelected")
            if (fragment.sortOrder != sortOrderSelected)
                fragment.setAndSaveSortOrder(sortOrderSelected)
        }

        //  Colored footers
        val coloredFooters = popup.actionColoredFooters.isChecked
        if (fragment.usePalette() != coloredFooters)
            fragment.setAndSaveUsePalette(coloredFooters)

        //  Grid Size
        var gridSize = 0
        for (i in 0 until fragment.maxGridSize) {
            if ((popup.gridSize.getChildAt(i) as RadioButton).isChecked) {
                gridSize = i + 1
                break
            }
        }
        if (gridSize > 0)
            fragment.setAndSaveGridSize(gridSize)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
    override fun onPageSelected(position: Int) {
        PreferenceUtil.getInstance(requireActivity()).lastPage = position
        if (currentFragment is PlaylistsFragment) {
            mainActivity.setFloatingActionButtonVisibility(View.VISIBLE)
        } else {
            mainActivity.setFloatingActionButtonVisibility(View.GONE)
        }
    }
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

    override fun handleFloatingActionButtonPress(): Boolean {
        CreatePlaylistDialog.createEmpty().show(childFragmentManager, "CREATE_PLAYLIST")
        return true
    }

    companion object {
        fun newInstance(): LibraryFragment = LibraryFragment()
    }
}
