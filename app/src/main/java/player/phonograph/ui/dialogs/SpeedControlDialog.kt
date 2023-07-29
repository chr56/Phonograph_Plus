/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.databinding.DialogSpeedControlBinding
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicService
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import kotlin.math.roundToInt

class SpeedControlDialog : DialogFragment() {

    private var _binding: DialogSpeedControlBinding? = null
    private val binding: DialogSpeedControlBinding get() = _binding!!

    private var targetSpeed: Float = -1f
    private fun applySpeed() {
        val service = MusicPlayerRemote.musicService ?: return
        val currentSpeed = service.speed
        if (targetSpeed > 0f && targetSpeed != currentSpeed)
            service.speed = targetSpeed
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val service: MusicService? = MusicPlayerRemote.musicService

        if (service == null) {
            Log.e(TAG, "Service unavailable!")
            return AlertDialog.Builder(requireContext()).setMessage(R.string.not_available_now).create()
        }

        _binding = DialogSpeedControlBinding.inflate(layoutInflater)

        val currentSpeed = service.speed
        binding.speed.setText(currentSpeed.toString())
        binding.speed.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            val value = text.toFloatOrNull()
            if (value != null) targetSpeed = value
        }

        binding.speedSeeker.max = 2 * RATIO
        binding.speedSeeker.progress = (currentSpeed * RATIO).roundToInt()
        binding.speedSeeker.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    binding.speed.setText((progress.toFloat() / RATIO).toString())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_speed)
            .setView(binding.root)
            .setPositiveButton(R.string.action_set) { _, _: Int ->
                applySpeed()
            }
            .setNegativeButton(R.string.reset_action) { _, _: Int ->
                targetSpeed = 1.0f
                applySpeed()
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _binding = null
    }

    companion object {
        private const val TAG = "SpeedControlDialog"

        private const val RATIO = 1000
    }

}