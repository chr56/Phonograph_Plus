/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import player.phonograph.ui.components.explorer.FilesPageExplorer
import player.phonograph.ui.components.explorer.FilesPageViewModel

class FilesPage : AbsPage() {

    private lateinit var explorer: FilesPageExplorer
    private val model: FilesPageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootContainer = FrameLayout(requireContext())
        container?.addView(rootContainer, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        explorer = FilesPageExplorer(hostFragment.mainActivity, hostFragment)
        explorer.inflate(rootContainer, inflater)
        return container ?: rootContainer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        explorer.destroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        explorer.loadData(model)
    }

    override fun onBackPress(): Boolean {
        return explorer.gotoTopLevel(false)
    }
}
