/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.appbar.AppBarLayout
import player.phonograph.R
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.mechanism.actions.actionPlay
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.modules.main.pages.adapter.SongCollectionDisplayAdapter
import player.phonograph.ui.modules.main.pages.adapter.SongDisplayAdapter
import player.phonograph.ui.modules.popup.ListOptionsPopup
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
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
        viewModel.loadFolders(requireContext(), true)
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
                mainFragment.totalAppBarScrollingRange + verticalOffset
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
        binding.refreshContainer.apply {
            setColorSchemeColors(accentColor())
            setDistanceToTriggerSync(480)
            setProgressViewOffset(false, 10, 120)
            setOnRefreshListener {
                viewModel.loadSongs(requireContext(), false)
                viewModel.loadFolders(requireContext(), false)
                isRefreshing = false
            }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                navigateUpBackPressedCallback.remove()
            }

            override fun onResume(owner: LifecycleOwner) {
                updateNavigateUpBackPressedCallback(viewModel.mainViewMode.value)
            }
        })
    }


    private fun setupAppBar() {

        binding.panel.setExpanded(false)
        binding.panel.addOnOffsetChangedListener(innerAppbarOffsetListener)

        val context = mainActivity
        context.attach(binding.panelToolbar.menu) {
            menuItem(Menu.NONE, Menu.NONE, 999, getString(R.string.action_settings)) {
                icon = context.getTintedDrawable(
                    R.drawable.ic_sort_variant_white_24dp,
                    context.primaryTextColor(context.nightMode),
                )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    mainFragment.popup.onShow = ::configPopup
                    mainFragment.popup.onDismiss = ::dismissPopup
                    mainFragment.popup.showAtLocation(
                        binding.root, Gravity.TOP or Gravity.END, 0,
                        8 + mainFragment.totalHeaderHeight + binding.panel.height
                    )
                    true
                }
            }
            menuItem(getString(R.string.action_play)) {
                icon = context
                    .getTintedDrawable(
                        R.drawable.ic_play_arrow_white_24dp,
                        context.primaryTextColor(context.nightMode)
                    )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (!viewModel.mainViewMode.value) {
                            val allSongs = viewModel.currentSongs.value
                            if (allSongs.isNotEmpty()) {
                                allSongs.actionPlay(ShuffleMode.NONE, 0)
                            }
                        }
                    }
                    true
                }
            }
            menuItem(getString(R.string.action_shuffle_all)) {
                icon = context
                    .getTintedDrawable(
                        R.drawable.ic_shuffle_white_24dp,
                        context.primaryTextColor(context.nightMode)
                    )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (!viewModel.mainViewMode.value) {
                            val allSongs = viewModel.currentSongs.value
                            if (allSongs.isNotEmpty()) {
                                allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                            }
                        }
                    }
                    true
                }
            }
        }
        binding.panelToolbar.setTitleTextColor(requireContext().primaryTextColor(requireContext().nightMode))

        mainFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    private fun configPopup(popup: ListOptionsPopup) {
        val mode = viewModel.mainViewMode.value
        if (mode) configPopupFolder(popup) else configPopupSongs(popup)
    }

    private fun configPopupFolder(popup: ListOptionsPopup) {
        val currentSortMode = Setting(popup.contentView.context).Composites[Keys.collectionSortMode].data

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable =
            arrayOf(SortRef.DISPLAY_NAME)//, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)
    }

    private fun configPopupSongs(popup: ListOptionsPopup) {
        val displayConfig = SongPageDisplayConfig(requireContext())
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
        val preference = Setting(popup.contentView.context).Composites[Keys.collectionSortMode]
        if (preference.data != selected) {
            preference.data = selected
            viewModel.loadFolders(requireContext(), false)
        }
    }

    private fun dismissPopupSongs(popup: ListOptionsPopup) {
        val displayConfig = SongPageDisplayConfig(requireContext())

        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayConfig.sortMode != selected) {
            displayConfig.sortMode = selected
            viewModel.loadFolders(requireContext(), false)
            if (!viewModel.mainViewMode.value) {
                viewModel.loadSongs(requireContext(), true)
            }
        }
    }

    private lateinit var songCollectionDisplayAdapter: SongCollectionDisplayAdapter
    private lateinit var songAdapter: SongDisplayAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private fun setupRecyclerView() {

        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        songCollectionDisplayAdapter = SongCollectionDisplayAdapter(
            mainFragment.mainActivity,
            ::onFolderClick
        )

        songAdapter = SongDisplayAdapter(
            mainFragment.mainActivity,
            ConstDisplayConfig(ItemLayoutStyle.LIST, false)
        )

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            mainFragment.mainActivity,
            accentColor()
        )
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = songCollectionDisplayAdapter

    }


    private fun onFolderClick(position: Int) = viewModel.browseFolder(
        requireContext(), position, true, linearLayoutManager.findLastVisibleItemPosition()
    )

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
                updateNavigateUpBackPressedCallback(mode)
                binding.recyclerView.adapter = if (mode) songCollectionDisplayAdapter else songAdapter
                linearLayoutManager.scrollToPosition(if (mode) viewModel.historyFolderPosition else viewModel.historyPosition)
            }
        }
        lifecycleScope.launch {
            viewModel.bannerText.collect { text ->
                binding.panelText.text = text
            }
        }
    }


    private fun updateNavigateUpBackPressedCallback(mainViewMode: Boolean) {
        if (mainViewMode || !isVisible) {
            navigateUpBackPressedCallback.remove()
        } else {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                navigateUpBackPressedCallback
            )
        }
    }

    private val navigateUpBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateUp()
        }
    }

    private fun navigateUp(): Boolean =
        viewModel.navigateUp(requireContext(), linearLayoutManager.findFirstVisibleItemPosition())

    override fun onDestroyView() {
        super.onDestroyView()
        binding.panel.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        mainFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }
}