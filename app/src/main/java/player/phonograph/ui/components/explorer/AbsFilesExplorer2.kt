/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.components.explorer

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import lib.phonograph.storage.root
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.databinding.FragmentFolderPageBinding
import player.phonograph.model.file.Location
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.warning
import androidx.annotation.DrawableRes
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class AbsFilesExplorer2<M : AbsFileViewModel> : Fragment() {

    // view binding
    private var _viewBinding: FragmentFolderPageBinding? = null
    protected val binding get() = _viewBinding!!
    // view model
    protected lateinit var model: M

    fun initModel(model: M) {
        this.model = model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewBinding = FragmentFolderPageBinding.inflate(
            layoutInflater,
            container,
            false
        )
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.innerAppBar.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        _viewBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    open fun setupObservers() {
        lifecycleScope.launch {
            model.currentLocation.collect { newLocation ->
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        // header
                        binding.header.apply {
                            location = newLocation
                            layoutManager.scrollHorizontallyBy(
                                binding.header.width / 4,
                                recyclerView.Recycler(),
                                RecyclerView.State()
                            )
                        }
                        binding.buttonBack.setImageDrawable(
                            requireContext().getThemedDrawable(
                                if (newLocation.parent == null) {
                                    R.drawable.ic_library_music_white_24dp
                                } else {
                                    com.afollestad.materialdialogs.color.R.drawable.icon_back_white
                                }
                            )
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            model.currentFiles.collect {
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateFilesDisplayed()
                    }
                }
            }
        }
        lifecycleScope.launch {
            model.loading.collect {
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.container.isRefreshing = it
                    }
                }
            }
        }
    }

    abstract fun updateFilesDisplayed()


    /**
     * reload all files (determined by [AbsFileViewModel.currentLocation])
     */
    protected fun refreshFiles() {
        context?.let { model.refreshFiles(it) }
    }


    private val innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.innerAppBar.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom
            )
        }

    /**
     * open a dialog to ask change storage volume (disk)
     */
    internal fun requireChangeVolume(): Boolean {
        if (context == null) return false
        val storageManager = requireContext().getSystemService<StorageManager>()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            ?: emptyList()
        if (volumes.isEmpty()) {
            warning(TAG, "No volumes found! Your system might be not supported!")
            return false
        }
        MaterialDialog(requireContext())
            .listItemsSingleChoice(
                items = volumes.map { "${it.getDescription(context)}\n(${it.root()?.path ?: "N/A"})" },
                initialSelection = volumes.indexOf(model.currentLocation.value.storageVolume),
                waitForPositiveButton = true,
            ) { materialDialog: MaterialDialog, i: Int, _: CharSequence ->
                materialDialog.dismiss()
                val path = volumes[i].root()?.absolutePath
                if (path == null) {
                    Toast.makeText(context, "Unmounted volume", Toast.LENGTH_SHORT).show()
                } else { // todo
                    model.changeLocation(requireContext(), Location.fromAbsolutePath("$path/"))
                }
            }
            .title(R.string.folders)
            .positiveButton(android.R.string.ok)
            .show()
        return true
    }

    /**
     * @param allowToChangeVolume false if do not intend to change volume
     * @return success or not
     */
    internal fun gotoTopLevel(allowToChangeVolume: Boolean): Boolean {
        if (context == null) return false
        val parent = model.currentLocation.value.parent
        return if (parent != null) {
            model.changeLocation(requireContext(), parent)
            true
        } else {
            Snackbar.make(binding.root, getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
            if (!allowToChangeVolume) {
                false
            } else {
                requireChangeVolume()
            }
        }
    }


    companion object {
        private const val TAG = "FilesExplorer"
        internal fun Context.getThemedDrawable(@DrawableRes resId: Int): Drawable? =
            getTintedDrawable(resId, primaryTextColor(nightMode))
    }
}