/*
 * Copyright (c) 2022 chr_56 & Karim Abou Zeid (kabouzeid)
 */
package player.phonograph.adapter.base

import android.content.Context
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import lib.phonograph.cab.ToolbarCabStatus2
import player.phonograph.R

/**
 * @author chr_56 & Karim Abou Zeid (kabouzeid)
 */
abstract class MultiSelectAdapter<VH : RecyclerView.ViewHolder, I>(
    protected val context: Context,
    private val cabController: MultiSelectionCabController?,
) : RecyclerView.Adapter<VH>() {

    abstract var multiSelectMenuRes: Int

    private var checkedList: MutableList<I> = ArrayList()

    private fun updateCab() {
        cabController?.showContent(context, checkedList.size, multiSelectMenuRes)
    }

    /** must return a real item **/
    protected abstract fun getItem(datasetPosition: Int): I

    abstract fun updateItemCheckStatusForAll()
    abstract fun updateItemCheckStatus(datasetPosition: Int)

    protected fun toggleChecked(datasetPosition: Int): Boolean {
        if (cabController != null) {
            val item = getItem(datasetPosition) ?: return false
            if (!checkedList.remove(item)) checkedList.add(item)
            updateItemCheckStatus(datasetPosition)
            updateCab()
            return true
        }
        return false
    }

    protected fun checkAll() {
        if (cabController != null) {
            checkedList.clear()
            for (i in 0 until itemCount) {
                val item = getItem(i)
                if (item != null) { checkedList.add(item) }
            }
            updateItemCheckStatusForAll()
            updateCab()
        }
    }

    private fun clearChecked() {
        checkedList.clear()
        updateItemCheckStatusForAll()
    }

    protected fun isChecked(identifier: I): Boolean = checkedList.contains(identifier)

    protected val isInQuickSelectMode: Boolean
        get() = cabController?.cab != null && cabController.cab.status == ToolbarCabStatus2.STATUS_ACTIVE

    protected open fun getName(obj: I): String = obj.toString()

    private fun onCabItemClicked(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_multi_select_adapter_check_all) {
            checkAll()
        } else {
            onMultipleItemAction(menuItem, ArrayList(checkedList))
            cabController?.dismiss()
        }
        return true
    }

    protected abstract fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>)
}
