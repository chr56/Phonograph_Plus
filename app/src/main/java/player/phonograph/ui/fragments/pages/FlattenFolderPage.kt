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
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.adapter.display.SongCollectionAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.util.Log
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

        setupAppBar()
        setupRecyclerView()

        observeRecyclerView()

        binding.recyclerView.also {
            it.adapter = songCollectionAdapter
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
                    context.primaryTextColor(context.resources.nightMode),
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
        binding.panelToolbar.setTitleTextColor(requireContext().primaryTextColor(resources.nightMode))

        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    private fun configPopup(popup: ListOptionsPopup) {
        val mode = viewModel.mainViewMode.value
        if (mode) configPopupFolder(popup) else configPopupSongs(popup)
    }

    private fun configPopupFolder(popup: ListOptionsPopup) {
        val currentSortMode = viewModel.sortMode.value
        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable =
            arrayOf(SortRef.DISPLAY_NAME)//, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)
    }

    private fun configPopupSongs(popup: ListOptionsPopup) {
        popup.dismiss()
        // todo
    }

    private fun dismissPopup(popup: ListOptionsPopup) {
        val mode = viewModel.mainViewMode.value
        if (mode) dismissPopupFolder(popup) else dismissPopupSongs(popup)
    }

    private fun dismissPopupFolder(popup: ListOptionsPopup) {
        val selected = SortMode(popup.sortRef, popup.revert)
        if (viewModel.sortMode.value != selected) {
            viewModel.sortMode.value = selected
            viewModel.loadFolders(requireContext())
        }
    }

    private fun dismissPopupSongs(popup: ListOptionsPopup) {
        // todo
    }

    private lateinit var songCollectionAdapter: SongCollectionAdapter
    private lateinit var songAdapter: SongDisplayAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private fun setupRecyclerView() {

        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        songCollectionAdapter = SongCollectionAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            emptyList(),
            R.layout.item_list,
        ) {}
        songCollectionAdapter.onClick = ::onFolderClick

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
        binding.recyclerView.adapter = songCollectionAdapter

    }


    private fun onFolderClick(position: Int) = viewModel.browseFolder(position)

    private fun observeRecyclerView() {
        lifecycleScope.launch {
            viewModel.folders.collect { data ->
                binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                songCollectionAdapter.dataset = data
            }
        }
        lifecycleScope.launch {
            viewModel.currentSongs.collect { data ->
                songAdapter.dataset = data
            }
        }
        lifecycleScope.launch {
            viewModel.mainViewMode.collect { mode ->
                binding.recyclerView.adapter = if (mode) songCollectionAdapter else songAdapter

                val size =
                    if (mode) songCollectionAdapter.dataset.size else songAdapter.dataset.size
                binding.panelText.text = if (mode)
                    requireContext().resources.getQuantityString(
                        R.plurals.item_files, size, size
                    ) else
                    requireContext().resources.getQuantityString(
                        R.plurals.x_songs, size, size
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
}