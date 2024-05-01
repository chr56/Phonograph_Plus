/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.pages

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_model.MenuContext
import com.google.android.material.appbar.AppBarLayout
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.databinding.FragmentFilePageBinding
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.loader.Songs
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.explorer.FilesPageExplorerFragment
import player.phonograph.ui.fragments.explorer.FilesPageViewModel
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu.NONE
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilesPage : AbsPage() {

    private var _viewBinding: FragmentFilePageBinding? = null
    private val binding get() = _viewBinding!!

    private lateinit var explorer: FilesPageExplorerFragment
    private val model: FilesPageViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        explorer = FilesPageExplorerFragment()
        _viewBinding = FragmentFilePageBinding.inflate(inflater, container, false)

        childFragmentManager.commitNow {
            replace(R.id.file_explore_container, explorer, "FilesPageExplorer")
        }

        return binding.root
    }

    private var outerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.fileExploreContainer.setPadding(
                binding.fileExploreContainer.paddingLeft,
                binding.fileExploreContainer.paddingTop,
                binding.fileExploreContainer.paddingRight,
                mainFragment.totalAppBarScrollingRange + verticalOffset
            )
        }

    private var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.fileExploreContainer.setPadding(
                binding.fileExploreContainer.paddingLeft,
                binding.panel.totalScrollRange + verticalOffset,
                binding.fileExploreContainer.paddingRight,
                binding.fileExploreContainer.paddingBottom
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.panel.setExpanded(false)
        binding.panel.addOnOffsetChangedListener(innerAppbarOffsetListener)

        val context = mainActivity
        context.attach(binding.panelToolbar.menu) {
            menuItem(NONE, NONE, 999, getString(R.string.action_settings)) {
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
            configAppBarActionButton(this)
        }

        binding.panelText.setTextColor(requireContext().primaryTextColor(requireContext().nightMode))
        binding.panelToolbar.setTitleTextColor(requireContext().primaryTextColor(requireContext().nightMode))

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.currentFiles.collect {
                    binding.panelText.text = headerText(resources, it.size)
                }
            }
        }

        mainFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    override fun onDestroyView() {

        binding.panel.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        mainFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        super.onDestroyView()
    }

    private fun configAppBarActionButton(menuContext: MenuContext) = with(menuContext) {
        menuItem(getString(R.string.action_play)) {
            icon = context
                .getTintedDrawable(
                    R.drawable.ic_play_arrow_white_24dp,
                    context.primaryTextColor(context.nightMode)
                )
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                lifecycleScope.launch(Dispatchers.IO) {
                    val allSongs = currentSongs(menuContext.context)
                    allSongs.actionPlay(ShuffleMode.NONE, 0)
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
                    val allSongs = currentSongs(menuContext.context)
                    allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                }
                true
            }
        }
        Unit
    }

    private suspend fun currentSongs(context: Context): List<Song> {
        val entities = model.currentFiles.value.filterIsInstance<FileEntity.File>()
        return entities.map { Songs.searchByFileEntity(context, it) }
    }

    private fun headerText(resources: Resources, size: Int): CharSequence =
        resources.getQuantityString(R.plurals.item_files, size, size)

    //region Popup
    private fun configPopup(popup: ListOptionsPopup) {
        val currentSortMode = Setting(popup.contentView.context).Composites[Keys.fileSortMode].data
        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable =
            arrayOf(SortRef.DISPLAY_NAME, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE, SortRef.SIZE)

        popup.showFileOption = true
        popup.useLegacyListFiles = model.useLegacyListFile
        popup.showFilesImages = model.showFilesImages
    }

    private fun dismissPopup(popup: ListOptionsPopup) {
        val context = popup.contentView.context
        Setting(context).Composites[Keys.fileSortMode].data =
            SortMode(popup.sortRef, popup.revert)
        model.useLegacyListFile = popup.useLegacyListFiles
        @SuppressLint("NotifyDataSetChanged")
        if (model.showFilesImages != popup.showFilesImages) {
            model.showFilesImages = popup.showFilesImages
            Setting(context)[Keys.showFileImages].data = model.showFilesImages
        }
        model.refreshFiles(context)
    }
    //endregion
}
