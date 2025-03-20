/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import com.google.android.material.snackbar.Snackbar
import lib.storage.extension.rootDirectory
import player.phonograph.R
import player.phonograph.databinding.FragmentFileExploreBinding
import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.file.Location
import player.phonograph.model.file.defaultStartDirectory
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.recyclerview.widget.LinearLayoutManager
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

sealed class AbsFilesExplorerFragment<M : AbsFileViewModel, A : AbsFilesAdapter<*>> : Fragment() {

    // view binding
    private var _viewBinding: FragmentFileExploreBinding? = null
    protected val binding get() = _viewBinding!!
    // view model
    protected abstract val model: M
    // adapter
    protected lateinit var adapter: A
    protected lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewBinding = FragmentFileExploreBinding.inflate(
            layoutInflater,
            container,
            false
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val accentColor = accentColor()
        setupObservers()

        // Back press
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                navigateUpBackPressedCallback.remove()
            }

            override fun onResume(owner: LifecycleOwner) {
                updateBackPressedDispatcher(model.currentLocation.value)
            }
        })

        // Back Button
        binding.buttonBack.apply {
            setImageDrawable(requireContext().getThemedDrawable(R.drawable.ic_nav_back_white_24dp))
            setOnClickListener { navigateUp(true) }
            setOnLongClickListener {
                onSwitch(Locations.from(defaultStartDirectory.absolutePath, requireContext()))
                true
            }
        }

        // Bread Crumb
        binding.breadCrumb.apply {
            location = model.currentLocation.value
            callBack = ::onSwitch
        }

        binding.container.apply {
            setColorSchemeColors(accentColor)
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                refreshFiles()
            }
        }

        // Recycle View
        adapter = createAdapter()
        layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.setUpFastScrollRecyclerViewColor(requireContext(), accentColor)

        binding.recyclerView.apply {
            layoutManager = this@AbsFilesExplorerFragment.layoutManager
            adapter = this@AbsFilesExplorerFragment.adapter
        }

        // Data
        model.refreshFiles(requireContext())
    }

    abstract fun createAdapter(): A

    protected open fun setupObservers() {
        lifecycleScope.launch {
            model.currentLocation.collect { newLocation ->
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        // Bread Crumb
                        binding.breadCrumb.apply {
                            location = newLocation
                            layoutManager.scrollHorizontallyBy(
                                binding.breadCrumb.width / 4,
                                recyclerView.Recycler(),
                                RecyclerView.State()
                            )
                        }
                        binding.buttonBack.setImageDrawable(
                            requireContext().getThemedDrawable(
                                if (newLocation.isRoot) {
                                    R.drawable.ic_sdcard_white_24dp
                                } else {
                                    R.drawable.ic_nav_back_white_24dp
                                }
                            )
                        )
                    }
                    updateBackPressedDispatcher(newLocation)
                }
            }
        }
        lifecycleScope.launch {
            model.currentFiles.collect {
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateFilesDisplayed()
                        layoutManager.scrollToPosition(model.historyPosition)
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
        model.refreshFiles(requireContext())
    }

    /**
     * open a dialog to ask change storage volume (disk)
     * @return true if dialog created
     */
    private fun requireChangeVolume(): Boolean {
        if (context == null) return false
        val storageManager = requireContext().getSystemService<StorageManager>()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            ?: emptyList()
        if (volumes.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.no_volume_found), Snackbar.LENGTH_SHORT).show()
            return false
        }
        val volumesNames = volumes.map { "${it.getDescription(context)}\n(${it.rootDirectory()?.path ?: "N/A"})" }
        val currentLocation = model.currentLocation.value
        val currentVolume = volumes.find { it.uuid.orEmpty() == currentLocation.volumeUUID }
        val selected = volumes.indexOf(currentVolume)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.storage_volumes)
            .setSingleChoiceItems(volumesNames.toTypedArray(), selected) { dialog, choice ->
                dialog.dismiss()
                val rootDirectory = volumes[choice].rootDirectory()
                if (rootDirectory == null) {
                    Toast.makeText(context, R.string.not_available_now, Toast.LENGTH_SHORT).show()
                } else {
                    onSwitch(Locations.from(rootDirectory, requireContext())) // todo
                }
            }
            .show().tintButtons()
        return true
    }

    /**
     * @param allowToChangeVolume false if not intend to change volume
     * @return success or not
     */
    protected fun navigateUp(allowToChangeVolume: Boolean): Boolean {
        if (activity == null) return false
        val current = model.currentLocation.value
        val parent = Locations.parent(current, requireContext())
        return if (parent != null) {
            onSwitch(parent)
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

    protected fun onSwitch(location: Location) {
        val position = layoutManager.findLastVisibleItemPosition()
        model.changeLocation(requireContext(), position, location)
    }

    private fun updateBackPressedDispatcher(location: Location) {
        if (!location.isRoot && isResumed) {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner, navigateUpBackPressedCallback
            )
        } else {
            navigateUpBackPressedCallback.remove()
        }
    }

    private val navigateUpBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp(false)
            }
        }

    companion object {
        private const val TAG = "FilesExplorer"
        internal fun Context.getThemedDrawable(@DrawableRes resId: Int): Drawable? =
            getTintedDrawable(resId, primaryTextColor(nightMode))
    }
}