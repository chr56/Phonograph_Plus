/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.main.pages

import player.phonograph.R
import player.phonograph.databinding.FragmentDisplayPageStatusBinding
import player.phonograph.util.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.util.theme.getTintedDrawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class StatusFragment : Fragment() {

    companion object {
        private const val TAG = "StatusFragment"
    }

    private var _binding: FragmentDisplayPageStatusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDisplayPageStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        show(R.string.state_loading)
    }

    fun hide(): Boolean {
        if (_binding == null) {
            report()
            return false
        }
        with(binding) {
            statusContainer.isVisible = false
        }
        return true
    }

    fun show(
        @StringRes titleRes: Int,
        @StringRes descriptionRes: Int? = null,
        @DrawableRes imageRes: Int? = null,
        @StringRes buttonTextRes: Int? = null,
        buttonClickListener: View.OnClickListener? = null,
    ): Boolean {
        if (_binding == null) {
            report()
            return false
        }
        with(binding) {

            statusContainer.isVisible = true

            title.setText(titleRes)

            image.apply {
                isVisible = imageRes != null
                imageRes?.let { setImageDrawable(getTintedDrawable(imageRes, primaryColor())) }
            }

            description.apply {
                isVisible = descriptionRes != null
                descriptionRes?.let { setText(it) }
            }

            button.apply {
                isVisible = buttonTextRes != null
                buttonTextRes?.let { setText(it) }
                setTextColor(primaryColor())
                setOnClickListener(buttonClickListener)
            }
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun report() {
        Log.e(TAG, "Fragment is not ready!")
    }
}