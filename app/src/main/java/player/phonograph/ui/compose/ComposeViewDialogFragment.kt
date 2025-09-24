/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * a ComposeView dialog
 */
abstract class ComposeViewDialogFragment : DialogFragment() {

    private var _composeView: ComposeView? = null
    private val composeView: ComposeView get() = _composeView!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).also {
        _composeView = it
        it.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView.setContent {
            Content()
        }
    }

    override fun onDestroyView() {
        _composeView = null
        super.onDestroyView()
    }

    /**
     * Composable content of dialog
     */
    @Composable
    protected abstract fun Content()
}