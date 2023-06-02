/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import lib.phonograph.dialog.LargeDialog
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.file.Location
import player.phonograph.ui.fragments.explorer.FilesChooserExplorerFragment
import player.phonograph.ui.fragments.explorer.FilesChooserViewModel
import player.phonograph.ui.components.viewcreater.buttonPanel
import player.phonograph.ui.components.viewcreater.contentPanel
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.core.view.setMargins
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout

abstract class FileChooserDialog : LargeDialog() {

    private lateinit var explorer: FilesChooserExplorerFragment
    private val model: FilesChooserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        explorer = FilesChooserExplorerFragment()
        explorer.initModel(model)
        return setupView(inflater, explorer)
    }

    protected open fun setupView(inflater: LayoutInflater, explorer: FilesChooserExplorerFragment): ViewGroup {
        val activity = requireActivity()

        val contentPanel = contentPanel(activity) {
            id = R.id.container
            setPadding(0, 0, 0, 24 + 128)
        }

        val buttonPanel = buttonPanel(activity) {
            button(0, getString(R.string.grant_permission), accentColor) {
                navigateToStorageSetting(activity)
            }
            space(1)
            button(2, getString(android.R.string.selectAll), accentColor) {
                affirmative(it, model.currentLocation.value)
            }
        }

        val rootContainer = FrameLayout(activity).apply {
            addView(contentPanel.panel, 0, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
            addView(buttonPanel.panel, 1, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM).apply { setMargins(8) })
            childFragmentManager.commit {
                replace(R.id.container, explorer, "FilesChooserExplorer")
            }
        }
        return rootContainer
    }

    protected abstract fun affirmative(view: View, currentLocation: Location)

    val accentColor by lazy { ThemeColor.accentColor(requireContext()) }
}
