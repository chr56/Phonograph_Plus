package player.phonograph.interfaces

import com.afollestad.materialcab.CreateCallback
import com.afollestad.materialcab.DestroyCallback
import com.afollestad.materialcab.SelectCallback
import com.afollestad.materialcab.attached.AttachedCab

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
interface CabHolder {
    fun showCab(menuRes: Int, createCallback: CreateCallback, selectCallback: SelectCallback, destroyCallback: DestroyCallback): AttachedCab
}
