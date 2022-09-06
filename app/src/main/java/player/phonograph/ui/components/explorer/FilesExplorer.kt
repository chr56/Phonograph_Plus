/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.appbar.AppBarLayout
import player.phonograph.databinding.FragmentFolderPageBinding
import player.phonograph.ui.components.ViewComponent

sealed class FilesExplorer<M>(protected val context: Context) : ViewComponent<ViewGroup, M> {

    private var _viewBinding: FragmentFolderPageBinding? = null
    protected val binding get() = _viewBinding!!

    override fun create(container: ViewGroup, model: M) {
        _viewBinding = FragmentFolderPageBinding.inflate(
            LayoutInflater.from(context),
            container,
            true
        )

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)

        initModel(model)
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

    abstract fun reload()
    abstract fun changeVolume(): Boolean
    abstract fun gotoTopLevel(allowToChangeVolume: Boolean): Boolean
}
