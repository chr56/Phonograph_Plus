/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * a ComposeView dialog
 */
abstract class BridgeDialogFragment : DialogFragment() {

    private lateinit var ui: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = ComposeView(requireContext())
        return ui
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ui.setContent {
            Content()
        }
    }

    @Composable
    protected abstract fun Content()
}