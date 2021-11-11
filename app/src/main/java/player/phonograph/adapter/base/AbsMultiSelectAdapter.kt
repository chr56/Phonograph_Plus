package player.phonograph.adapter.base

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialcab.MaterialCab
import player.phonograph.R
import player.phonograph.interfaces.CabHolder
import java.util.ArrayList

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMultiSelectAdapter<VH : RecyclerView.ViewHolder, I>(
    private val context: Context,
    private val cabHolder: CabHolder?,
    @MenuRes var menuRes: Int
) : RecyclerView.Adapter<VH>(), MaterialCab.Callback {

    private var cab: MaterialCab? = null
    private var checked: MutableList<I> = ArrayList()

    protected fun setMultiSelectMenuRes(@MenuRes menuRes: Int) {
        this.menuRes = menuRes
    }

    protected fun toggleChecked(position: Int): Boolean {
        if (cabHolder != null) {
            val identifier = getIdentifier(position) ?: return false
            if (!checked.remove(identifier)) checked.add(identifier)
            notifyItemChanged(position)
            updateCab()
            return true
        }
        return false
    }

    protected fun checkAll() {
        if (cabHolder != null) {
            checked.clear()
            for (i in 0 until itemCount) {
                val identifier = getIdentifier(i)
                if (identifier != null) {
                    checked.add(identifier)
                }
            }
            notifyDataSetChanged()
            updateCab()
        }
    }

    private fun updateCab() {
        if (cabHolder != null) {
            if (cab == null || !cab!!.isActive) {
                cab = cabHolder.openCab(menuRes, this)
            }
            val size = checked.size
            if (size <= 0) cab!!.finish()
            else cab!!.setTitle(
                context.getString(R.string.x_selected, size)
            )
        }
    }

    private fun clearChecked() {
        checked.clear()
        notifyDataSetChanged()
    }

    protected fun isChecked(identifier: I): Boolean {
        return checked.contains(identifier)
    }

    protected val isInQuickSelectMode: Boolean
        get() = cab != null && cab!!.isActive

    override fun onCabCreated(materialCab: MaterialCab, menu: Menu): Boolean {
        return true
    }

    override fun onCabItemClicked(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_multi_select_adapter_check_all) {
            checkAll()
        } else {
            onMultipleItemAction(menuItem, ArrayList(checked))
            cab!!.finish()
            clearChecked()
        }
        return true
    }

    override fun onCabFinished(materialCab: MaterialCab): Boolean {
        clearChecked()
        return true
    }

    protected open fun getName(obj: I): String {
        return obj.toString()
    }

    protected abstract fun getIdentifier(position: Int): I
    protected abstract fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>)
}
