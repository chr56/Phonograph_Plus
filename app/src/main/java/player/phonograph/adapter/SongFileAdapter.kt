package player.phonograph.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.MediaStoreSignature
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import java.io.File
import java.util.Locale
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.audiocover.AudioFileCover
import player.phonograph.util.FileUtil.getReadableFileSize
import player.phonograph.util.ImageUtil
import util.mddesign.util.Util

class SongFileAdapter(
    private val activity: AppCompatActivity,
    dataSet: List<File>,
    @LayoutRes private val itemLayoutRes: Int,
    private val callbacks: Callbacks?,
    cabController: MultiSelectionCabController?,
) :
    MultiSelectAdapter<SongFileAdapter.ViewHolder, File>(activity, cabController),
    SectionedAdapter {

    override var multiSelectMenuRes: Int = R.menu.menu_media_selection
    init {
        setHasStableIds(true)
    }

    var dataSet: List<File> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int = if (dataSet[position].isDirectory) FOLDER else FILE

    override fun getItemId(position: Int): Long = dataSet[position].hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val file = dataSet[index]

        holder.itemView.isActivated = isChecked(file)
        holder.title?.text = getFileTitle(file)
        holder.shortSeparator?.visibility =
            if (holder.bindingAdapterPosition == itemCount - 1) {
                View.GONE
            } else {
                View.VISIBLE
            }

        holder.text?.let {
            if (holder.itemViewType == FILE) {
                it.text = getFileText(file)
            } else {
                it.visibility = View.GONE
            }
        }
        holder.image?.let {
            loadFileImage(file, holder)
        }
    }

    private fun getFileTitle(file: File): String = file.name

    private fun getFileText(file: File): String? = if (file.isDirectory) null else getReadableFileSize(file.length())

    private fun loadFileImage(file: File, holder: ViewHolder) {
        val iconColor = Util.resolveColor(activity, R.attr.iconColor)
        if (file.isDirectory) {
            holder.image!!.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            holder.image!!.setImageResource(R.drawable.ic_folder_white_24dp)
        } else {
            val error = ImageUtil.getTintedVectorDrawable(
                activity, R.drawable.ic_file_music_white_24dp, iconColor
            )
            Glide.with(activity)
                .load(AudioFileCover(file.path))
                .apply(SongGlideRequest.DEFAULT_OPTION)
                .transition(SongGlideRequest.DEFAULT_DRAWABLE_TRANSITION_OPTIONS)
                .placeholder(error)
                .signature(MediaStoreSignature("", file.lastModified(), 0))
                .into(holder.image!!)
        }
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItem(datasetPosition: Int): File = dataSet[datasetPosition]

    override fun getName(obj: File): String = getFileTitle(obj)

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<File>) {
        if (callbacks == null) return
        callbacks.onMultipleItemAction(menuItem, selection)
    }

    override fun getSectionName(position: Int): String =
        dataSet[position].name[0].uppercase(Locale.getDefault())

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        init {
            menu?.let { menu ->
                menu.setOnClickListener { v: View? ->
                    if (isPositionInRange(bindingAdapterPosition)) {
                        callbacks?.onFileMenuClicked(dataSet[bindingAdapterPosition], v)
                    }
                }
            }
        }

        override fun onClick(v: View) {
            val position = bindingAdapterPosition
            if (isPositionInRange(position)) {
                if (isInQuickSelectMode) {
                    toggleChecked(position)
                } else {
                    callbacks?.onFileSelected(dataSet[position])
                }
            }
        }

        override fun onLongClick(v: View): Boolean =
            isPositionInRange(bindingAdapterPosition) && toggleChecked(bindingAdapterPosition)

        private fun isPositionInRange(position: Int): Boolean =
            position >= 0 && position < dataSet.size
    }

    interface Callbacks {
        fun onFileSelected(file: File)
        fun onFileMenuClicked(file: File, view: View?)
        fun onMultipleItemAction(item: MenuItem, files: List<File>)
    }

    companion object {
        private const val FILE = 0
        private const val FOLDER = 1
    }

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)
}
