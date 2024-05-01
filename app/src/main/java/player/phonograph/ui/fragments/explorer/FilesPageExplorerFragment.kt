/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.fragments.explorer

import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.model.file.FileEntity
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.MainFragment
import player.phonograph.ui.views.StatusBarView
import androidx.fragment.app.viewModels
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import java.lang.ref.SoftReference

class FilesPageExplorerFragment : AbsFilesExplorerFragment<FilesPageViewModel, FilesPageAdapter>() {

    override val model: FilesPageViewModel by viewModels({ requireActivity() })

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }

    override fun onPrepareHeader() {
        binding.buttonOptions.setImageDrawable(requireContext().getThemedDrawable(R.drawable.ic_sort_variant_white_24dp))
        binding.buttonOptions.setOnClickListener {
            popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0, calculateHeight()
            )
        }
    }

    override fun createAdapter(): FilesPageAdapter =
        FilesPageAdapter(requireActivity(), model.currentFiles.value) { fileEntities, position ->
            when (val item = fileEntities[position]) {
                is FileEntity.Folder -> {
                    onSwitch(item.location)
                }

                is FileEntity.File   -> {
                    ClickActionProviders.FileEntityClickActionProvider().listClick(
                        fileEntities,
                        position,
                        requireContext(),
                        null
                    )
                }
            }
        }//todo

    // region Popup
    private var _mainFragment: SoftReference<MainFragment?> = SoftReference(null)
    var mainFragment: MainFragment?
        get() = _mainFragment.get()
        set(value) {
            _mainFragment = SoftReference(value)
        }

    private val popup: ListOptionsPopup by lazy(LazyThreadSafetyMode.NONE) {
        ListOptionsPopup(requireContext()).also {
            it.onShow = this::configPopup
            it.onDismiss = this::dismissPopup
        }
    }

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
        Setting(popup.contentView.context).Composites[Keys.fileSortMode].data =
            SortMode(popup.sortRef, popup.revert)
        model.useLegacyListFile = popup.useLegacyListFiles
        @SuppressLint("NotifyDataSetChanged")
        if (model.showFilesImages != popup.showFilesImages) {
            model.showFilesImages = popup.showFilesImages
            adapter.loadCover = model.showFilesImages
            adapter.notifyDataSetChanged()
        }
        refreshFiles()
    }

    private fun calculateHeight(): Int {
        val statusBarHeight = requireActivity().findViewById<StatusBarView>(R.id.status_bar)?.height ?: 8
        val appbarHeight = mainFragment?.totalHeaderHeight ?: 0
        val innerAppBarHeight = binding.navigationHeader.height
        return statusBarHeight + innerAppBarHeight + appbarHeight // + homeFragment.totalHeaderHeight //todo
    }
    //endregion
}