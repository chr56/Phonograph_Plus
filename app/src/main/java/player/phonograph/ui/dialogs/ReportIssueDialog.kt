/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import mt.pref.ThemeColor
import mt.tint.viewtint.tint
import player.phonograph.ISSUE_TRACKER_LINK
import player.phonograph.R
import player.phonograph.databinding.DialogReportIssueBinding
import player.phonograph.util.DeviceInfoUtil
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode

class ReportIssueDialog : DialogFragment() {

    private var _binding: DialogReportIssueBinding? = null
    private val binding get() = _binding!!

    private val deviceInfo by lazy { DeviceInfoUtil.getDeviceInfo(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogReportIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.deviceInfoText.text = deviceInfo

        binding.buttonSend.tint(primaryColor, true, resources.nightMode)
        binding.buttonSend.setImageDrawable(
            requireContext().getTintedDrawable(R.drawable.ic_send_white_24dp, Color.WHITE)
        )
        binding.buttonSend.setOnClickListener {
            requireContext().copyDeviceInfoToClipBoard()
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    this.data = Uri.parse(ISSUE_TRACKER_LINK)
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }

    private val primaryColor get() = ThemeColor.primaryColor(requireContext())

    private fun Context.copyDeviceInfoToClipBoard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager
            .setPrimaryClip(ClipData.newPlainText(getString(R.string.device_info), deviceInfo))
        Toast.makeText(this, R.string.copied_device_info_to_clipboard, Toast.LENGTH_LONG).show()
    }

}
