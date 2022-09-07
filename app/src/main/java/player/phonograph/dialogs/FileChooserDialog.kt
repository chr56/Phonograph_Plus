/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.setPadding
import androidx.fragment.app.viewModels
import lib.phonograph.dialog.LargeDialog
import player.phonograph.model.file.Location
import player.phonograph.ui.components.explorer.FilesChooserExplorer
import player.phonograph.ui.components.explorer.FilesChooserViewModel

abstract class FileChooserDialog : LargeDialog() {

    private lateinit var explorer: FilesChooserExplorer
    private val model: FilesChooserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        explorer = FilesChooserExplorer(requireActivity())
        return setupView(inflater, explorer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        explorer.loadData(model)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        explorer.destroy()
    }

    protected open fun setupView(inflater: LayoutInflater, explorer: FilesChooserExplorer): ViewGroup {
        val activity = requireActivity()

        val buttonPanelHeight = 128
        val content = FrameLayout(activity)
        explorer.inflate(content, inflater)
        content.setPadding(0, 0, 0, 24 + buttonPanelHeight)

        val buttonPanel = FrameLayout(activity)
        with(buttonPanel) {
            val button = AppCompatButton(activity).apply {
                text = activity.getText(android.R.string.selectAll)
                gravity = Gravity.CENTER
                setPadding(16)
                setOnClickListener {
                    affirmative(it, model.currentLocation)
                }
                background = ColorDrawable(Color.TRANSPARENT)
            }
            addView(
                button,
                LayoutParams(MATCH_PARENT, buttonPanelHeight, Gravity.CENTER)
            )
        }

        val rootContainer = FrameLayout(activity)
        rootContainer.addView(content, 0, LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
        rootContainer.addView(
            buttonPanel,
            1,
            LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM)
        )
        return rootContainer
    }

    protected abstract fun affirmative(view: View, currentLocation: Location)

    companion object {
        class TestDialog : FileChooserDialog() {
            override fun affirmative(view: View, currentLocation: Location) {
                Toast.makeText(requireContext(), currentLocation.absolutePath, Toast.LENGTH_SHORT).show()
            }
        }

    }
}
