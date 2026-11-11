/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.player.dialogs

import player.phonograph.R
import player.phonograph.databinding.DialogSpeedControlBinding
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicService
import player.phonograph.ui.theme.tintButtons
import player.phonograph.ui.util.observe
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.SeekBar
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow

class SpeedControlDialog : DialogFragment() {

    private var _binding: DialogSpeedControlBinding? = null
    private val binding: DialogSpeedControlBinding get() = _binding!!

    private val _speed: MutableStateFlow<Float> = MutableStateFlow(-1f)
    private fun applySpeed() {
        val current = MusicPlayerRemote.speed
        val target = _speed.value
        if (target > 0f && target != current)
            MusicPlayerRemote.setSpeed(target)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (!MusicPlayerRemote.isServiceConnected) {
            Log.e(TAG, "Service unavailable!")
            return AlertDialog.Builder(requireContext())
                .setMessage(R.string.tips_service_disconnected)
                .create().tintButtons()
        }

        _binding = DialogSpeedControlBinding.inflate(layoutInflater)

        setup()


        _speed.value = MusicPlayerRemote.speed // init
        observe(lifecycle, _speed) { speed ->
            binding.speedSeeker.progress = calculateProcess(speed)
            binding.speed.setText(String.format(null, "%.2f", speed))
        }


        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.label_speed)
            .setView(binding.root)
            .setPositiveButton(R.string.action_set) { _, _: Int ->
                applySpeed()
            }
            .setNegativeButton(R.string.action_reset) { _, _: Int ->
                _speed.value = 1.0f
                applySpeed()
            }
            .create().tintButtons()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _binding = null
    }

    private fun setup() {

        binding.speedSeeker.max = length()
        binding.speedSeeker.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var currentProcess = -1
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentProcess = progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (currentProcess > -1) _speed.value = calculateSpeed(currentProcess)
                }
            }
        )

        binding.speed.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = binding.speed.text?.toString()
                if (text != null) {
                    val newValue = text.toFloatOrNull()
                    if (newValue != null) _speed.value = newValue
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
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