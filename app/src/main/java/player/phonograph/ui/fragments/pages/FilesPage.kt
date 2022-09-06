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
import player.phonograph.ui.components.explorer.FilesViewModel

class FilesPage : AbsPage() {

    private lateinit var explorer: FilesPageExplorer
    private lateinit var root: ViewGroup
    private val model: FilesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        explorer = FilesPageExplorer(hostFragment.mainActivity, hostFragment)
        root = FrameLayout(requireContext())
        container?.addView(root, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        explorer.destroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        explorer.create(root, model)
    }

    override fun onBackPress(): Boolean {
        return explorer.gotoTopLevel(false)
    }
}
