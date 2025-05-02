/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import player.phonograph.R
import player.phonograph.mechanism.actions.ActionMenuProviders.ActionMenuProvider
import player.phonograph.mechanism.actions.actionGotoDetail
import player.phonograph.model.QueueSong
import player.phonograph.ui.dialogs.DeletionDialog
import player.phonograph.ui.modules.tag.TagBrowserActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


interface PlaylistEditorAdapterLinage {

    fun at(position: Int): QueueSong
    fun size(): Int

    suspend fun deleteSong(position: Int)
    suspend fun moveSong(from: Int, to: Int)
}

class PlaylistEditorItemMenuProvider(
    private val adapter: PlaylistEditorAdapterLinage,
) : ActionMenuProvider<QueueSong> {
    override fun inflateMenu(menu: Menu, context: Context, item: QueueSong, position: Int) {
        editorItemMenu(menu, context as FragmentActivity, position)
    }

    private fun editorItemMenu(menu: Menu, activity: FragmentActivity, bindingAdapterPosition: Int) = attach(activity, menu) {
        val queueSong = adapter.at(bindingAdapterPosition)
        menuItem {
            titleRes(R.string.action_remove_from_playlist)
            onClick {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    adapter.deleteSong(bindingAdapterPosition)
                }
                true
            }
        }
        menuItem {
            titleRes(R.string.move_to_top)
            onClick {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    adapter.moveSong(bindingAdapterPosition, 0)
                }
                true
            }
        }
        menuItem {
            titleRes(R.string.move_up)
            onClick {
                if (bindingAdapterPosition != 0) {
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        adapter.moveSong(bindingAdapterPosition, bindingAdapterPosition - 1)
                    }
                    true
                } else false
            }
        }
        menuItem {
            titleRes(R.string.move_down)
            onClick {
                if (bindingAdapterPosition != adapter.size() - 1) {
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        adapter.moveSong(bindingAdapterPosition, bindingAdapterPosition + 1)
                    }
                    true
                } else false
            }
        }
        menuItem {
            titleRes(R.string.move_to_bottom)
            onClick {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    adapter.moveSong(bindingAdapterPosition, adapter.size() - 1)
                }
                true
            }
        }

        menuItem {
            titleRes(R.string.action_details)
            onClick {
                queueSong.song.actionGotoDetail(activity)
                true
            }
        }

        submenu(
            context.getString(R.string.more_actions)
        ) {
            menuItem {
                titleRes(R.string.action_tag_editor)
                onClick {
                    TagBrowserActivity.launch(activity, queueSong.song.data)
                    true
                }
            }
            menuItem {
                titleRes(R.string.action_delete_from_device)
                onClick {
                    DeletionDialog.create(arrayListOf(queueSong.song))
                        .show(activity.supportFragmentManager, "DELETE_SONGS")
                    true
                }
            }
        }
    }
}
