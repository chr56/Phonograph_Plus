/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.dialog

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log

inline fun alertDialog(context: Context, buildBlock: DialogContext.() -> Unit): AlertDialog {
    val builder = AlertDialog.Builder(context)
    val dialogContext = DialogContext(context, builder)
    dialogContext.apply(buildBlock)
    return dialogContext.build()
}

open class DialogContext(val context: Context, val builder: AlertDialog.Builder) {

    fun title(@StringRes res: Int) = title(context.getText(res))
    fun title(text: CharSequence): DialogContext {
        builder.setTitle(text)
        return this
    }

    fun message(@StringRes res: Int) = message(context.getText(res))
    fun message(text: CharSequence): DialogContext {
        builder.setMessage(text)
        return this
    }

    fun positiveButton(
        @StringRes res: Int,
        callback: ((DialogInterface) -> Unit)? = null,
    ) = positiveButton(context.getText(res), callback)

    fun positiveButton(
        text: CharSequence,
        callback: ((DialogInterface) -> Unit)? = null,
    ): DialogContext {
        builder.setPositiveButton(text) { dialog, _ ->
            callback?.invoke(dialog)
        }
        return this
    }

    fun negativeButton(
        @StringRes res: Int,
        callback: ((DialogInterface) -> Unit)? = null,
    ) = negativeButton(context.getText(res), callback)

    fun negativeButton(
        text: CharSequence,
        callback: ((DialogInterface) -> Unit)? = null,
    ): DialogContext {
        builder.setNegativeButton(text) { dialog, _ ->
            callback?.invoke(dialog)
        }
        return this
    }

    fun neutralButton(
        @StringRes res: Int,
        callback: ((DialogInterface) -> Unit)? = null,
    ) = neutralButton(context.getText(res), callback)

    fun neutralButton(
        text: CharSequence,
        callback: ((DialogInterface) -> Unit)? = null,
    ): DialogContext {
        builder.setNeutralButton(text) { dialog, _ ->
            callback?.invoke(dialog)
        }
        return this
    }

    fun singleChoiceItems(
        map: SingleChoiceItemMap,
        selected: Int = -1,
        dismiss: Boolean,
    ): DialogContext {
        val labels = map.map { it.first }.toTypedArray()
        val callbacks = map.map { it.second }
        builder.setSingleChoiceItems(labels, selected) { dialog, id ->
            if (id >= 0 && id < map.size) {
                callbacks[id].invoke(dialog)
            } else {
                Log.v("Dialog", "$id outbound")
            }
            if (dismiss) dialog.dismiss()
        }
        return this
    }

    fun build(): AlertDialog = builder.create()

}

typealias SingleChoiceItemMap = List<Pair<CharSequence, (DialogInterface) -> Unit>>