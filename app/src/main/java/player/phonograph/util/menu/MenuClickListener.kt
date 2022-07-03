/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util.menu

import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.FragmentActivity
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification

abstract class MenuClickListener(
    activity: FragmentActivity,
    @MenuRes menuRes: Int?
) : View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private val activityWeakReference: WeakReference<FragmentActivity> = WeakReference(activity)
    private val activity: FragmentActivity? get() = activityWeakReference.get()

    abstract val song: Song
    protected open var realRes = menuRes ?: menuResDefault

    // show menu
    override fun onClick(v: View) {
        onShowMenu(v)
    }

    private fun onShowMenu(v: View) {
        activity?.let {
            PopupMenu(it, v).also { popupMenu ->
                popupMenu.inflate(realRes)
                popupMenu.setOnMenuItemClickListener(this)
                popupMenu.show()
            }
        }
            ?: ErrorNotification.postErrorNotification("No Activity?\n${Thread.currentThread().stackTrace}")
    }

    // handle action
    override fun onMenuItemClick(item: MenuItem): Boolean {
        return onSongMenuItemClick(
            activity ?: throw RuntimeException("MenuClickListener: activity is null!"),
            song,
            item.itemId
        )
    }

    companion object {
        const val menuResDefault = R.menu.menu_item_song
    }
}
