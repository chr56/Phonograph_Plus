/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.R
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import player.phonograph.ui.components.explorer.FilesPageExplorer2
import player.phonograph.ui.components.explorer.FilesPageViewModel
import androidx.fragment.app.commitNow
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout

class FilesPage : AbsPage() {

    private lateinit var explorer: FilesPageExplorer2
    private val model: FilesPageViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext()).also { frameLayout ->
            frameLayout.id = R.id.container
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        explorer = FilesPageExplorer2()
        explorer.controller = hostFragment.cabController
        explorer.initModel(model)

        childFragmentManager.commitNow {
            replace(R.id.container, explorer, "FilesPageExplorer")
        }

    }

    override fun onBackPress(): Boolean {
        return explorer.gotoTopLevel(false)
    }
}
