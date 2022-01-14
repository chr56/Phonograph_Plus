/*
 * Copyright (c) 2022 chr_56 & Karim Abou Zeid (kabouzeid)
 */
package player.phonograph.adapter.base

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import lib.phonograph.cab.CabStatus
import lib.phonograph.cab.MultiSelectionCab
import player.phonograph.R
import player.phonograph.interfaces.MultiSelectionCabProvider
import java.util.*

/**
 * @author chr_56 & Karim Abou Zeid (kabouzeid)
 */
abstract class MultiSelectAdapter<VH : RecyclerView.ViewHolder, I>(
    protected val context: Context,
    private val cabProvider: MultiSelectionCabProvider?,
) : RecyclerView.Adapter<VH>() {

    abstract var multiSelectMenuRes: Int

    private var cab: MultiSelectionCab? = null
    private var checkedList: MutableList<I> = ArrayList()

    private fun updateCab() {
        if (cabProvider != null) {
            cab = cabProvider.getCab() ?: cabProvider.createCab(multiSelectMenuRes, this::onCabCreated, this::onCabItemClicked, this::onCabFinished)
            when (cab!!.status) {
                CabStatus.STATUS_DESTROYING -> return
                CabStatus.STATUS_DESTROYED -> {
                    cab = cabProvider.createCab(multiSelectMenuRes, this::onCabCreated, this::onCabItemClicked, this::onCabFinished)
                }
                else -> {}
            }

            if (checkedList.size <= 0) cabProvider.dismissCab()
            else {
                cab!!.titleText = context.getString(R.string.x_selected, checkedList.size)
                cab!!.titleTextColor = ThemeColor.textColorPrimary(context)
                cabProvider.showCab()
            }
        }
    }

    /** must return a real item **/
    protected abstract fun getItem(datasetPosition: Int): I

    abstract fun updateItemCheckStatusForAll()
    abstract fun updateItemCheckStatus(datasetPosition: Int)

    protected fun toggleChecked(datasetPosition: Int): Boolean {
        if (cabProvider != null) {
            val item = getItem(datasetPosition) ?: return false
            if (!checkedList.remove(item)) checkedList.add(item)
            updateItemCheckStatus(datasetPosition)
            updateCab()
            return true
        }
        return false
    }

    protected fun checkAll() {
        if (cabProvider != null) {
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
        get() = cab != null && cab!!.status == CabStatus.STATUS_ACTIVE

    protected open fun getName(obj: I): String = obj.toString()

    private fun onCabCreated(cab: MultiSelectionCab, menu: Menu): Boolean {
        return true
    }

    private fun onCabItemClicked(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_multi_select_adapter_check_all) {
            checkAll()
        } else {
            onMultipleItemAction(menuItem, ArrayList(checkedList))
            cabProvider?.dismissCab()
        }
        return true
    }

    private fun onCabFinished(cab: MultiSelectionCab): Boolean {
        clearChecked()
        return true
    }
    protected abstract fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>)
}
