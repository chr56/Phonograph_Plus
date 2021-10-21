package player.phonograph.preferences.basic.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.ListPreference
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.preferences.basic.ListPreferenceX
// Todo Completed

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class ListPreferenceDialogFragmentCompatX : PreferenceDialogFragmentX() {
    private var ClickedEntryIndex: Int = 0
    private val listPreference: ListPreferenceX
        get() = preference as ListPreferenceX

    @SuppressLint("CheckResult")
    override fun onPrepareDialog(dialog: MaterialDialog) {
        super.onPrepareDialog(dialog)
        val preference: ListPreference = listPreference
        check(!(preference.entries == null || preference.entryValues == null)) { "ListPreference requires an entries array and an entryValues array." }
        ClickedEntryIndex = preference.findIndexOfValue(preference.value)
// TODO
        dialog.noAutoDismiss()
            .listItemsSingleChoice(
                items = List<CharSequence>(preference.entries.size) { preference.entries[it] },
                checkedColor = ThemeColor.accentColor(requireContext()),
                initialSelection = ClickedEntryIndex,
                waitForPositiveButton = false
            ) { _, index, _ -> ClickedEntryIndex = index }
            .negativeButton() { dismiss() }
            .positiveButton() {
                preference.value =
                    preference.entryValues[ClickedEntryIndex].toString()
                dismiss()
                if (preference.key == "general_theme") {
                    this.requireActivity().recreate()
                }
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String?): ListPreferenceDialogFragmentCompatX {
            val fragment = ListPreferenceDialogFragmentCompatX()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
