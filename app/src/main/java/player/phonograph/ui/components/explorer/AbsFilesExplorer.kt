/*
 * Copyright (c) 2022 chr_56
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
import player.phonograph.notification.ErrorNotification
import player.phonograph.ui.components.ViewComponent
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.annotation.DrawableRes
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Environment
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class AbsFilesExplorer<M : AbsFileViewModel>(protected val context: Context) : ViewComponent<ViewGroup, M> {

    // view
    private var _viewBinding: FragmentFolderPageBinding? = null
    protected val binding get() = _viewBinding!!

    /**
     * view model [AbsFileViewModel]
     */
    protected lateinit var model: M

    override fun inflate(rootContainer: ViewGroup, layoutInflater: LayoutInflater?) {
        _viewBinding = FragmentFolderPageBinding.inflate(
            layoutInflater ?: LayoutInflater.from(context),
            rootContainer,
            true
        )
    }

    override fun loadData(model: M) {
        this.model = model
        initModel(model)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        val lifecycle = (context as LifecycleOwner).lifecycle
        val scope = (context as LifecycleOwner).lifecycleScope
        scope.launch {
            model.currentLocation.collect { newLocation ->
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    scope.launch(Dispatchers.Main) {
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
                            context.getThemedDrawable(
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
        scope.launch {
            model.currentFiles.collect {
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    scope.launch(Dispatchers.Main) {
                        updateFilesDisplayed()
                    }
                }
            }
        }
        scope.launch {
            model.loading.collect {
                lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                    scope.launch(Dispatchers.Main) {
                        binding.container.isRefreshing = it
                    }
                }
            }
        }
    }

    protected abstract fun initModel(model: M)

    override fun destroy() {
        binding.innerAppBar.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        _viewBinding = null
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
     * reload all files (determined by [AbsFileViewModel.currentLocation])
     */
    protected fun reload() {
        model.refreshFiles(context)
    }

    abstract fun updateFilesDisplayed()

    /**
     * open a dialog to ask change storage volume (disk)
     */
    internal fun requireChangeVolume(): Boolean {
        val storageManager = context.getSystemService<StorageManager>()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            ?: emptyList()
        if (volumes.isEmpty()) {
            ErrorNotification.postErrorNotification("No volumes found! Your system might be not supported!")
            return false
        }
        MaterialDialog(context)
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
                    model.changeLocation(context, Location.fromAbsolutePath("$path/"))
                    reload()
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
        val parent = model.currentLocation.value.parent
        return if (parent != null) {
            model.changeLocation(context, parent)
            reload()
            true
        } else {
            Snackbar.make(binding.root, context.getString(R.string.reached_to_root), Snackbar.LENGTH_SHORT).show()
            if (!allowToChangeVolume) {
                false
            } else {
                requireChangeVolume()
            }
        }
    }

    companion object {
        internal fun Context.getThemedDrawable(@DrawableRes resId: Int): Drawable? =
            getTintedDrawable(resId, primaryTextColor(nightMode))
    }
}
