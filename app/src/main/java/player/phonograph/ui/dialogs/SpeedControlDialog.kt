/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.databinding.DialogSpeedControlBinding
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicService
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SpeedControlDialog : DialogFragment() {

    private var _binding: DialogSpeedControlBinding? = null
    private val binding: DialogSpeedControlBinding get() = _binding!!

    private val speedData: MutableStateFlow<Float> = MutableStateFlow(-1f)
    private fun applySpeed() {
        val service = MusicPlayerRemote.musicService ?: return
        val currentSpeed = service.speed
        val targetSpeed = speedData.value ?: 1.0f
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

        speedData.value = currentSpeed

        binding.speedSeeker.max = length()
        binding.speedSeeker.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var currentProcess = -1
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentProcess = progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    speedData.value = calculateSpeed(currentProcess)
                }
            }
        )

        binding.speed.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val editable = binding.speed.text
                if (editable != null) {
                    val newValue = editable.toString().toFloatOrNull()
                    if (newValue != null) speedData.value = newValue
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        lifecycleScope.launch {
            speedData.collect { speed ->
                binding.speedSeeker.progress = calculateProcess(speed)
                binding.speed.setText(String.format("%.2f", speed))
            }
        }


        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_speed)
            .setView(binding.root)
            .setPositiveButton(R.string.action_set) { _, _: Int ->
                applySpeed()
            }
            .setNegativeButton(R.string.reset_action) { _, _: Int ->
                speedData.value = 1.0f
                applySpeed()
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _binding = null
    }

    private fun length() = ((MAX - MIN) * RATIO).roundToInt()

    private fun calculateSpeed(process: Int): Float =
        MIN + (process.toFloat() / RATIO)

    private fun calculateProcess(speed: Float): Int =
        when {
            speed > MAX -> length()
            speed < MIN -> 0
            else        -> (RATIO * (speed - MIN)).roundToInt()
        }


    companion object {
        private const val TAG = "SpeedControlDialog"

        private const val MAX = 2.0f
        private const val MIN = 0.5f
        private const val RATIO = 1000
    }

}