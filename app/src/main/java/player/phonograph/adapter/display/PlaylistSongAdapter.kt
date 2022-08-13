/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter.display

import android.content.Context
import android.content.Intent
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.glide.SongGlideRequest
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.Song
import player.phonograph.util.ViewUtil.hitTest
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity

class PlaylistSongAdapter(
    activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<Song>,
    cfg: (DisplayAdapter<Song>.() -> Unit)?
) : DisplayAdapter<Song>(activity, cabController, dataSet, R.layout.item_list, cfg), DraggableItemAdapter<PlaylistSongAdapter.ViewHolder> {

    override fun getSectionNameImp(position: Int): String = (position + 1).toString()

    override fun setImage(holder: DisplayViewHolder, position: Int) {
        SongGlideRequest.Builder.from(Glide.with(context), dataset[position])
            .checkIgnoreMediaStore(activity).build().into(holder.image!!)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(layoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        if (editMode) holder.dragView?.visibility = View.VISIBLE
        super.onBindViewHolder(holder, position)
    }

    var editMode: Boolean = false

    var onMove: (fromPosition: Int, toPosition: Int) -> Boolean = { _, _ -> true }
    var onDelete: (position: Int) -> Unit = {}

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        position >= 0 &&
            (hitTest(holder.dragView!!, x, y) || hitTest(holder.image!!, x, y))

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange =
        ItemDraggableRange(0, dataset.size - 1)

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition != toPosition) {
            if (onMove(fromPosition, toPosition)
            ) {
                // update dataset(playlistSongs)
                val newSongs: MutableList<Song> = dataset.toMutableList()
                val song = newSongs.removeAt(fromPosition)
                newSongs.add(toPosition, song)
                dataset = newSongs
            }
        }
    }

    override fun onMenuClick(bindingAdapterPosition: Int, menuButtonView: View) {
        if (editMode) {
            PopupMenu(activity, menuButtonView).apply {
                injectPlaylistEditor(menu, activity, bindingAdapterPosition)
            }.show()
        } else {
            super.onMenuClick(bindingAdapterPosition, menuButtonView)
        }
    }

    private fun injectPlaylistEditor(menu: Menu, context: Context, bindingAdapterPosition: Int) =
        context.attach(menu) {
            val song = dataset[bindingAdapterPosition]
            menuItem {
                titleRes(R.string.action_remove_from_playlist)
                onClick {
                    onDelete(bindingAdapterPosition)
                    (dataset as MutableList).removeAt(bindingAdapterPosition)
                    notifyItemRangeChanged(bindingAdapterPosition, dataset.size - 1) // so we can reorder the items behind removed one
                    true
                }
            }
            menuItem {
                titleRes(R.string.move_to_top)
                onClick {
                    if (onMove(bindingAdapterPosition, 0)) {
                        (dataset as MutableList).removeAt(bindingAdapterPosition)
                        (dataset as MutableList).add(0, song)
                        notifyItemRangeChanged(0, bindingAdapterPosition + 1) // so we can reorder the items affected
                    }
                    true
                }
            }
            menuItem {
                titleRes(R.string.move_up)
                onClick {
                    if (bindingAdapterPosition != 0) {
                        if (onMove(bindingAdapterPosition, bindingAdapterPosition - 1)) {
                            (dataset as MutableList).removeAt(bindingAdapterPosition)
                            (dataset as MutableList).add(bindingAdapterPosition - 1, song)
                            notifyItemRangeChanged(
                                bindingAdapterPosition - 1,
                                bindingAdapterPosition
                            )
                        }
                        true
                    } else false
                }
            }
            menuItem {
                titleRes(R.string.move_down)
                onClick {
                    if (bindingAdapterPosition != dataset.size - 1) {
                        if (onMove(bindingAdapterPosition, bindingAdapterPosition + 1)) {
                            (dataset as MutableList).removeAt(bindingAdapterPosition)
                            (dataset as MutableList).add(bindingAdapterPosition + 1, song)
                            notifyItemRangeChanged(
                                bindingAdapterPosition,
                                bindingAdapterPosition + 1
                            )
                        }
                        true
                    } else false
                }
            }
            menuItem {
                titleRes(R.string.move_to_bottom)
                onClick {
                    if (onMove(bindingAdapterPosition, dataset.size - 1)) {
                        (dataset as MutableList).removeAt(bindingAdapterPosition)
                        (dataset as MutableList).add(song)
                        notifyItemRangeChanged(bindingAdapterPosition - 1, dataset.size - 1) // so we can reorder the items affected
                    }
                    true
                }
            }

            // todo

            menuItem {
                itemId = R.id.action_details
                titleRes(R.string.action_details)
                onClick {
                    SongDetailDialog.create(song).show(
                        activity.supportFragmentManager,
                        "SONG_DETAILS"
                    )
                    true
                }
            }

            submenu(
                context.getString(R.string.more_actions)
            ) {
                menuItem {
                    itemId = R.id.action_tag_editor
                    titleRes(R.string.action_tag_editor)
                    onClick {
                        activity.startActivity(
                            Intent(activity, SongTagEditorActivity::class.java).apply {
                                putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
                                if (activity is PaletteColorHolder) {
                                    putExtra(
                                        AbsTagEditorActivity.EXTRA_PALETTE,
                                        (activity as PaletteColorHolder).paletteColor
                                    )
                                }
                            }
                        )
                        true
                    }
                }
                menuItem {
                    itemId = R.id.action_delete_from_device
                    titleRes(R.string.action_delete_from_device)
                    onClick {
                        DeleteSongsDialog.create(listOf(song))
                            .show(activity.supportFragmentManager, "DELETE_SONGS")
                        true
                    }
                }
            }
        }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean =
        (dropPosition >= 0) && (dropPosition <= dataset.size - 1)

    override fun onItemDragStarted(position: Int) { }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        when {
            fromPosition < toPosition -> notifyItemRangeChanged(fromPosition, toPosition)
            fromPosition > toPosition -> notifyItemRangeChanged(toPosition, fromPosition)
            fromPosition == toPosition -> notifyItemChanged(fromPosition)
        }

        // special for top and bottle
        if (fromPosition == 0) {
            notifyItemChanged(toPosition)
        }
        if (fromPosition == dataset.size - 1 && toPosition == 0) {
            notifyItemChanged(dataset.size - 1)
        }
    }

    inner class ViewHolder(itemView: View) :
        DisplayViewHolder(itemView),
        DraggableItemViewHolder {

        @DraggableItemStateFlags
        private var mDragStateFlags = 0

        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int = mDragStateFlags
        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        override fun getDragState(): DraggableItemState {
            return DraggableItemState().apply {
                this.flags = mDragStateFlags
            }
        }
    }
}
