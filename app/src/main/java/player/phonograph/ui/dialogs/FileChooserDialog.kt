/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Space
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.ButtonBarLayout
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.viewModels
import lib.phonograph.dialog.LargeDialog
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.file.Location
import player.phonograph.ui.components.explorer.FilesChooserExplorer
import player.phonograph.ui.components.explorer.FilesChooserViewModel
import player.phonograph.util.Util

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

    @SuppressLint("RestrictedApi")
    protected open fun setupView(inflater: LayoutInflater, explorer: FilesChooserExplorer): ViewGroup {
        val activity = requireActivity()

        val buttonPanelHeight = 128
        val contentPanel = FrameLayout(activity)
        explorer.inflate(contentPanel, inflater)
        contentPanel.setPadding(0, 0, 0, 24 + buttonPanelHeight)

        val buttonPanel = ButtonBarLayout(activity, null).apply { orientation = LinearLayout.HORIZONTAL }
        with(buttonPanel) {
            val buttonGrantPermission =
                createButton(activity, getString(R.string.grant_permission)) {
                    Util.navigateToStorageSetting(activity)
                }
            val buttonPositive =
                createButton(activity, getString(android.R.string.selectAll)) {
                    affirmative(it, model.currentLocation)
                }
            val layoutParams =
                LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 0f).apply { setMargins(16) }
            addView(
                buttonGrantPermission,
                layoutParams
            )
            addView(
                Space(activity),
                LayoutParams(0, 0, 1f)
            )
            addView(
                buttonPositive,
                layoutParams
            )
        }

        val rootContainer = FrameLayout(activity)
        rootContainer.addView(contentPanel, 0, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
        rootContainer.addView(buttonPanel, 1, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        return rootContainer
    }

    protected abstract fun affirmative(view: View, currentLocation: Location)

    private fun createButton(context: Context, buttonText: String, onClick: (View) -> Unit): Button {
        return AppCompatButton(context).apply {
            text = buttonText
            textSize = 20f
            setTextColor(accentColor)
            gravity = Gravity.CENTER
            setOnClickListener { onClick(it) }
            background = ColorDrawable(Color.TRANSPARENT)
            setPadding(8)
        }
    }

    val accentColor by lazy { ThemeColor.accentColor(requireContext()) }
}
