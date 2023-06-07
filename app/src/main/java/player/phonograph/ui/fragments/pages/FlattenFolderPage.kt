/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.appbar.AppBarLayout
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.display.SongCollectionDisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.launch

class FlattenFolderPage : AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    private val viewModel: FlattenFolderPageViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel.loadFolders(requireContext())
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    // for mini bar
    private var outerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.container.paddingTop,
                binding.container.paddingRight,
                hostFragment.totalAppBarScrollingRange + verticalOffset
            )
        }

    private var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.panel.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.empty.text = resources.getText(R.string.loading)

        setupRecyclerView()
        setupAppBar()

        observeRecyclerView()

        binding.recyclerView.also {
            it.adapter = songCollectionDisplayAdapter
            it.layoutManager = linearLayoutManager
        }
    }


    private fun setupAppBar() {

        binding.panel.setExpanded(false)
        binding.panel.addOnOffsetChangedListener(innerAppbarOffsetListener)

        val context = hostFragment.mainActivity
        context.attach(binding.panelToolbar.menu) {
            menuItem(Menu.NONE, Menu.NONE, 999, getString(R.string.action_settings)) {
                icon = context.getTintedDrawable(
                    R.drawable.ic_sort_variant_white_24dp,
                    context.primaryTextColor(context.nightMode),
                )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    hostFragment.popup.onShow = ::configPopup
                    hostFragment.popup.onDismiss = ::dismissPopup
                    hostFragment.popup.showAtLocation(
                        binding.root, Gravity.TOP or Gravity.END, 0,
                        8 + hostFragment.totalHeaderHeight + binding.panel.height
                    )
                    true
                }
            }
        }
        binding.panelToolbar.setTitleTextColor(requireContext().primaryTextColor(requireContext().nightMode))

        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    private fun configPopup(popup: ListOptionsPopup) {
        val mode = viewModel.mainViewMode.value
        if (mode) configPopupFolder(popup) else configPopupSongs(popup)
    }

    private fun configPopupFolder(popup: ListOptionsPopup) {
        val currentSortMode = Setting.instance.collectionSortMode

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable =
            arrayOf(SortRef.DISPLAY_NAME)//, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)
    }

    private fun configPopupSongs(popup: ListOptionsPopup) {
        val displayConfig = DisplayConfig(DisplayConfigTarget.SongPage)
        val currentSortMode = displayConfig.sortMode

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable =
            arrayOf(
                SortRef.SONG_NAME,
                SortRef.ALBUM_NAME,
                SortRef.ARTIST_NAME, SortRef.YEAR, SortRef.ADDED_DATE,
                SortRef.MODIFIED_DATE,
                SortRef.DURATION,
            )
    }

    private fun dismissPopup(popup: ListOptionsPopup) {
        val mode = viewModel.mainViewMode.value
        if (mode) dismissPopupFolder(popup) else dismissPopupSongs(popup)
    }

    private fun dismissPopupFolder(popup: ListOptionsPopup) {

        val selected = SortMode(popup.sortRef, popup.revert)
        if (Setting.instance.collectionSortMode != selected) {
            Setting.instance.collectionSortMode = selected
            viewModel.loadFolders(requireContext())
        }
    }

    private fun dismissPopupSongs(popup: ListOptionsPopup) {
        val displayConfig = DisplayConfig(DisplayConfigTarget.SongPage)

        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayConfig.sortMode != selected) {
            displayConfig.sortMode = selected
            viewModel.loadFolders(requireContext())
            viewModel.loadSongs()
        }
    }

    private lateinit var songCollectionDisplayAdapter: SongCollectionDisplayAdapter
    private lateinit var songAdapter: SongDisplayAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private fun setupRecyclerView() {

        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        songCollectionDisplayAdapter = SongCollectionDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            emptyList(),
            R.layout.item_list,
        ) {}
        songCollectionDisplayAdapter.onClick = ::onFolderClick

        songAdapter = SongDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            emptyList(),
            R.layout.item_list,
        ) {}

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = songCollectionDisplayAdapter

    }


    private fun onFolderClick(position: Int) = viewModel.browseFolder(position)

    private fun observeRecyclerView() {
        lifecycleScope.launch {
            viewModel.folders.collect { data ->
                binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                songCollectionDisplayAdapter.dataset = data
            }
        }
        lifecycleScope.launch {
            viewModel.currentSongs.collect { data ->
                songAdapter.dataset = data
            }
        }
        lifecycleScope.launch {
            viewModel.mainViewMode.collect { mode ->
                binding.recyclerView.adapter = if (mode) songCollectionDisplayAdapter else songAdapter

                val size =
                    if (mode) songCollectionDisplayAdapter.dataset.size else songAdapter.dataset.size
                binding.panelText.text = if (mode)
                    requireContext().resources.getQuantityString(
                        R.plurals.item_files, size, size
                    ) else
                    requireContext().resources.getQuantityString(
                        R.plurals.item_songs, size, size
                    )
            }
        }
    }

    override fun onBackPress(): Boolean {
        with(viewModel.mainViewMode) {
            if (value) {
                return false
            } else {
                value = true
                return true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.panel.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        hostFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }
}