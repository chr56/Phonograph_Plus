/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

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
abstract class ComposeViewDialogFragment : DialogFragment() {

    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).also {
        composeView = it
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView.setContent {
            Content()
        }
    }

    /**
     * Composable content of dialog
     */
    @Composable
    protected abstract fun Content()
}