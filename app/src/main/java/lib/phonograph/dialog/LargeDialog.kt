/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.dialog

import androidx.fragment.app.DialogFragment

open class LargeDialog : DialogFragment() {
    override fun onStart() {
        // set up size
        requireDialog().window!!.attributes =
            requireDialog().window!!.let { window ->
                window.attributes.apply {
                    width = (requireActivity().window.decorView.width * 0.90).toInt()
                    height = (requireActivity().window.decorView.height * 0.90).toInt()
                }
            }

        super.onStart()
    }
}