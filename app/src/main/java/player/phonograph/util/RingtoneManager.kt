package player.phonograph.util

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import util.mdcolor.pref.ThemeColor

object RingtoneManager {

    fun setRingtone(context: Context, id: Long) {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM, MusicUtil.getSongFileUri(id))
    }

    fun requiresDialog(context: Context): Boolean = !Settings.System.canWrite(context)

    fun showDialog(context: Context): MaterialDialog {
        val dialog = MaterialDialog(context)
            .show {
                title(R.string.dialog_ringtone_title)
                message(R.string.dialog_ringtone_message)
                negativeButton(android.R.string.cancel)
                positiveButton(android.R.string.ok) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:" + context.packageName)
                        }
                    )
                }
                getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(context))
                getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(context))
            }
        return dialog
    }
}
