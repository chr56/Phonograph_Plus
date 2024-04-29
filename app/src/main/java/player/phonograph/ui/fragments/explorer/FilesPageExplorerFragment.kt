/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.ui.fragments.explorer

import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.actions.ClickActionProviders
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.MainFragment
import player.phonograph.ui.views.StatusBarView
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import java.lang.ref.SoftReference
import com.afollestad.materialdialogs.R as MDR

class FilesPageExplorerFragment : AbsFilesExplorerFragment<FilesPageViewModel>() {

    override val model: FilesPageViewModel by viewModels({ requireActivity() })

    private lateinit var adapter: FilesPageAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun updateFilesDisplayed() {
        adapter.dataSet = model.currentFiles.value.toMutableList()
    }

    private var _mainFragment: SoftReference<MainFragment?> = SoftReference(null)
    var mainFragment: MainFragment?
        get() = _mainFragment.get()
        set(value) {
            _mainFragment = SoftReference(value)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val accentColor = ThemeColor.accentColor(requireContext())
        super.onViewCreated(view, savedInstanceState)
        // header
        binding.buttonPageHeader.setImageDrawable(requireContext().getThemedDrawable(R.drawable.ic_sort_variant_white_24dp))
        binding.buttonPageHeader.setOnClickListener {
            popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0, calculateHeight()
            )
        }
        binding.buttonBack.setImageDrawable(requireContext().getThemedDrawable(MDR.drawable.md_nav_back))
        binding.buttonBack.setOnClickListener { gotoTopLevel(true) }
        binding.buttonBack.setOnLongClickListener {
            model.changeLocation(requireContext(), Location.HOME)
            true
        }
        // bread crumb
        binding.header.apply {
            location = model.currentLocation.value
            callBack = {
                model.changeLocation(context, it)
            }
        }

        binding.container.apply {
            setColorSchemeColors(accentColor)
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                refreshFiles()
            }
        }

        // recycle view
        layoutManager = LinearLayoutManager(activity)
        adapter = FilesPageAdapter(requireActivity(), model.currentFiles.value) { fileEntities, position ->
            when (val item = fileEntities[position]) {
                is FileEntity.Folder -> {
                    model.changeLocation(requireContext(), item.location)
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

        binding.recyclerView.setUpFastScrollRecyclerViewColor(requireContext(), accentColor)
        binding.recyclerView.apply {
            layoutManager = this@FilesPageExplorerFragment.layoutManager
            adapter = this@FilesPageExplorerFragment.adapter
        }
        model.refreshFiles(requireContext())
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
        val innerAppBarHeight = binding.innerAppBar.height
        return statusBarHeight + innerAppBarHeight + appbarHeight // + homeFragment.totalHeaderHeight //todo
    }
}