/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import lib.phonograph.storage.root
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.FileAdapter
import player.phonograph.databinding.FragmentFolderPageBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.mediastore.sort.FileRef
import player.phonograph.mediastore.sort.FileSortMode
import player.phonograph.model.FileEntity
import player.phonograph.model.Location
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import util.mdcolor.pref.ThemeColor

class FilesPage : AbsPage() {

    private val model: FilesPageViewModel by viewModels()

    private var _viewBinding: FragmentFolderPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentFolderPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }

    lateinit var adapter: FileAdapter
    lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)

        // header
        binding.buttonPageHeader.setImageDrawable(getDrawable(R.drawable.ic_sort_variant_white_24dp))
        binding.buttonPageHeader.setOnClickListener {
            popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0,
                (
                    hostFragment.mainActivity.findViewById<player.phonograph.views.StatusBarView>(R.id.status_bar)?.height ?: 8
                    ) +
                    hostFragment.totalHeaderHeight + binding.innerAppBar.height
            )
        }
        binding.buttonBack.setImageDrawable(getDrawable(R.drawable.icon_back_white))
        binding.buttonBack.setOnClickListener { gotoTopLevel() }
        binding.buttonBack.setOnLongClickListener {
            model.currentLocation = Location.HOME
            reload()
            true
        }
        binding.headerTitle.text = model.currentLocation.let { "${it.storageVolume.getDescription(requireContext())} : ${it.basePath}" }
        binding.header.smoothScrollBy((binding.headerTitle.width * 0.3).toInt(), 0)

        binding.container.apply {
            setColorSchemeColors(ThemeColor.accentColor(requireContext()))
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                reload()
            }
        }

        // recycle view
        layoutManager = LinearLayoutManager(hostFragment.mainActivity)
        adapter = FileAdapter(hostFragment.mainActivity, model.currentFileList.toMutableList(), {
            when (it) {
                is FileEntity.Folder -> {
                    model.currentLocation = it.location
                    reload()
                }
                is FileEntity.File -> {
                    MusicPlayerRemote.playNext(it.linkedSong)
                }
            }
        }, hostFragment)

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.apply {
            layoutManager = this@FilesPage.layoutManager
            adapter = this@FilesPage.adapter
        }
        model.loadFiles { reload() }
    }

    private var _popup: ListOptionsPopup? = null
    private val popup: ListOptionsPopup
        get() {
            if (_popup == null) _popup =
                ListOptionsPopup(hostFragment.mainActivity).apply {
                    onShow = this@FilesPage::configPopup
                    onDismiss = this@FilesPage::dismissPopup
                }
            return _popup!!
        }

    private fun configPopup(popup: PopupWindowMainBinding) {
        // clear existed
        popup.sortOrderBasic.visibility = View.VISIBLE
        popup.textSortOrderBasic.visibility = View.VISIBLE
        popup.sortOrderContent.visibility = View.VISIBLE
        popup.textSortOrderContent.visibility = View.VISIBLE
        for (i in 0 until popup.sortOrderContent.childCount) popup.sortOrderContent.getChildAt(i).visibility = View.GONE

        popup.sortOrderContent.clearCheck()
        popup.sortOrderNamePlain.visibility = View.VISIBLE
        popup.sortOrderDateAdded.visibility = View.VISIBLE
        popup.sortOrderDateModified.visibility = View.VISIBLE
        popup.sortOrderSize.visibility = View.VISIBLE

        val currentSortMode = Setting.instance.fileSortMode
        when (currentSortMode.sortRef) {
            FileRef.DISPLAY_NAME -> popup.sortOrderContent.check(R.id.sort_order_name_plain)
            FileRef.ADDED_DATE -> popup.sortOrderContent.check(R.id.sort_order_date_added)
            FileRef.MODIFIED_DATE -> popup.sortOrderContent.check(R.id.sort_order_date_modified)
            FileRef.SIZE -> popup.sortOrderContent.check(R.id.sort_order_size)
            else -> popup.sortOrderContent.clearCheck()
        }

        when (currentSortMode.revert) {
            false -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
            true -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
        }
    }

    private fun dismissPopup(popUpView: PopupWindowMainBinding) {
        val revert = when (popUpView.sortOrderBasic.checkedRadioButtonId) {
            R.id.sort_order_z_a -> true
            R.id.sort_order_a_z -> false
            else -> false
        }
        val sortRef = when (popUpView.sortOrderContent.checkedRadioButtonId) {
            R.id.sort_order_name_plain -> FileRef.DISPLAY_NAME
            R.id.sort_order_date_added -> FileRef.ADDED_DATE
            R.id.sort_order_date_modified -> FileRef.MODIFIED_DATE
            R.id.sort_order_size -> FileRef.SIZE
            else -> FileRef.ID
        }
        Setting.instance.fileSortMode = FileSortMode(sortRef, revert)
        reload()
    }

    /**
     * @param allowToChangeVolume false if do not intend to change volume
     * @return success or not
     */
    private fun gotoTopLevel(allowToChangeVolume: Boolean = true): Boolean {
        val parent = model.currentLocation.parent
        return if (parent != null) {
            model.currentLocation = parent
            reload()
            true
        } else {
            Snackbar.make(binding.root, getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
            if (!allowToChangeVolume) {
                false
            } else {
                changeVolume()
            }
        }
    }

    private fun changeVolume(): Boolean {
        val storageManager = hostFragment.mainActivity.getSystemService<StorageManager>()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            ?: emptyList()
        if (volumes.isEmpty()) {
            ErrorNotification.postErrorNotification("No volumes found! Your system might be not supported!")
            return false
        }
        MaterialDialog(hostFragment.mainActivity)
            .listItemsSingleChoice(
                items = volumes.map { "${it.getDescription(requireContext())}\n(${it.root()?.path ?: "N/A"})" },
                initialSelection = volumes.indexOf(model.currentLocation.storageVolume),
                waitForPositiveButton = true,
            ) { materialDialog: MaterialDialog, i: Int, _: CharSequence ->
                materialDialog.dismiss()
                val path = volumes[i].root()?.absolutePath
                if (path == null) {
                    Toast.makeText(hostFragment.mainActivity, "Unmounted volume", Toast.LENGTH_SHORT).show()
                } else { // todo
                    model.currentLocation = Location.fromAbsolutePath("$path/")
                    reload()
                }
            }
            .title(R.string.folders)
            .positiveButton(android.R.string.ok)
            .show()
        return true
    }

    private fun reload() {
        binding.container.isRefreshing = true
        model.loadFiles {
            adapter.dataSet = model.currentFileList.toMutableList()
            binding.headerTitle.text = model.currentLocation.let { "${it.storageVolume.getDescription(requireContext())} : ${it.basePath}" }
            binding.header.smoothScrollBy((binding.headerTitle.width * 0.6).toInt(), 0)
            binding.buttonBack.setImageDrawable(
                if (model.currentLocation.parent == null)
                    getDrawable(R.drawable.ic_library_music_white_24dp)
                else
                    getDrawable(R.drawable.icon_back_white)
            )
            binding.container.isRefreshing = false
        }
    }

    private fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(hostFragment.mainActivity, resId)?.also {
            it.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(
                    binding.headerTitle.currentTextColor,
                    BlendModeCompat.SRC_IN
                )
        }
    }

    private var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.innerAppBar.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom

            )
        }

    override fun onBackPress(): Boolean {
        return gotoTopLevel(false)
    }
}
