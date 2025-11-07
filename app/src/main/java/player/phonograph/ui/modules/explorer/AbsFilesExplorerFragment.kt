/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import com.google.android.material.snackbar.Snackbar
import player.phonograph.R
import player.phonograph.databinding.FragmentFileExploreBinding
import player.phonograph.mechanism.explorer.MediaPaths
import player.phonograph.model.file.FileItem
import player.phonograph.model.file.MediaPath
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.util.observe
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.withStateAtLeast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

sealed class AbsFilesExplorerFragment : Fragment() {

    // view binding
    private var _viewBinding: FragmentFileExploreBinding? = null
    protected val binding get() = _viewBinding!!
    // view model
    protected val model: FileExplorerViewModel by viewModels({ requireActivity() })
    // adapter
    protected lateinit var adapter: DisplayAdapter<FileItem>
    protected lateinit var presenter: DisplayPresenter<FileItem>
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

        // Back press
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                navigateUpBackPressedCallback.remove()
            }

            override fun onResume(owner: LifecycleOwner) {
                updateBackPressedDispatcher(model.currentPath.value)
            }
        })

        // Back Button
        binding.buttonBack.apply {
            setImageDrawable(requireContext().getThemedDrawable(R.drawable.ic_nav_back_white_24dp))
            setOnClickListener { navigateUp(true) }
            setOnLongClickListener {
                onSwitch(MediaPaths.startDirectory(it.context))
                true
            }
        }

        // Bread Crumb
        binding.breadCrumb.apply {
            val current = model.currentPath.value
            setCrumbs(current.volume.name, current.basePathSegments)
            setOnCrumbClick { crumbs: List<String> ->
                onSwitch(MediaPaths.from(current.volumeRoot, crumbs, context))
            }
        }

        binding.container.apply {
            setColorSchemeColors(accentColor)
            setProgressViewOffset(false, 0, 180)
            setOnRefreshListener {
                refreshFiles()
            }
        }

        // Recycle View
        presenter = FileItemPresenter(
            clickActionProvider = createClickActionProvider(),
            menuProvider = createMenuProvider(),
        )
        adapter = DisplayAdapter(
            activity = requireActivity(),
            presenter = presenter,
            allowMultiSelection = allowMultiSelection,
            stableId = false,
        )
        layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.setUpFastScrollRecyclerViewColor(requireContext(), accentColor)

        binding.recyclerView.apply {
            layoutManager = this@AbsFilesExplorerFragment.layoutManager
            adapter = this@AbsFilesExplorerFragment.adapter
        }

        // Data
        setupObservers()
        model.refreshFiles(requireContext())
    }

    abstract val allowMultiSelection: Boolean
    abstract fun createClickActionProvider(): ClickActionProviders.ClickActionProvider<FileItem>
    abstract fun createMenuProvider(): ActionMenuProviders.ActionMenuProvider<FileItem>?

    protected open fun setupObservers() {
        observe(model.currentPath) { newPath ->
            lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                // Bread Crumb
                binding.breadCrumb.apply {
                    setCrumbs(newPath.volume.name, newPath.basePathSegments)
                    layoutManager.scrollHorizontallyBy(
                        binding.breadCrumb.width / 4,
                        recyclerView.Recycler(),
                        RecyclerView.State()
                    )
                }
                binding.buttonBack.setImageDrawable(
                    requireContext().getThemedDrawable(
                        if (newPath.isRoot) {
                            R.drawable.ic_sdcard_white_24dp
                        } else {
                            R.drawable.ic_nav_back_white_24dp
                        }
                    )
                )
                updateBackPressedDispatcher(newPath)
            }
        }
        observe(model.currentFiles) { items ->
            adapter.dataset = items
            layoutManager.scrollToPosition(model.historyPosition)
        }
        observe(model.loading) { loading ->
            binding.container.isRefreshing = loading
        }
    }

    /**
     * reload all files (determined by [FileExplorerViewModel.currentPath])
     */
    protected fun refreshFiles() {
        model.refreshFiles(requireContext())
    }

    /**
     * open a dialog to ask change storage volume (disk)
     * @return true if dialog created
     */
    private fun requireChangeVolume(): Boolean {
        val volumes = model.volumes(requireContext())
        if (volumes.names.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.err_no_volume_found), Snackbar.LENGTH_SHORT).show()
            return false
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.label_storage_volumes)
            .setSingleChoiceItems(volumes.names.toTypedArray(), volumes.current) { dialog, choice ->
                dialog.dismiss()
                val volumeRoot = volumes.paths[choice]
                if (volumeRoot != null) {
                    onSwitch(volumeRoot)
                } else {
                    Toast.makeText(context, R.string.tips_not_available_now, Toast.LENGTH_SHORT).show()
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
        val current = model.currentPath.value
        val parent = MediaPaths.parent(current, requireContext())
        return if (parent != null) {
            onSwitch(parent)
            true
        } else {
            Snackbar.make(binding.root, getString(R.string.tips_reached_to_root), Snackbar.LENGTH_SHORT).show()
            if (!allowToChangeVolume) {
                false
            } else {
                requireChangeVolume()
            }
        }
    }

    protected fun onSwitch(item: FileItem) = onSwitch(item.mediaPath)

    protected fun onSwitch(target: MediaPath) {
        val position = layoutManager.findLastVisibleItemPosition()
        model.changeDirectory(requireContext(), position, target)
    }

    private fun updateBackPressedDispatcher(path: MediaPath) {
        if (!path.isRoot && isResumed) {
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