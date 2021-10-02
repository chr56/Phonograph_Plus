package com.kabouzeid.gramophone.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import chr_56.MDthemer.util.Util
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.MediaStoreSignature
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.adapter.base.AbsMultiSelectAdapter
import com.kabouzeid.gramophone.adapter.base.MediaEntryViewHolder
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCover
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.util.ImageUtil
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

class SongFileAdapter(
    private val activity: AppCompatActivity,
    private var dataSet: List<File>,
    @LayoutRes private val itemLayoutRes: Int,
    private val callbacks: Callbacks?,
    cabHolder: CabHolder?
) : AbsMultiSelectAdapter<SongFileAdapter.ViewHolder?, File?>(
    activity, cabHolder, R.menu.menu_media_selection
),
    SectionedAdapter {

    init {
        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataSet[position].isDirectory) FOLDER
        else FILE
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].hashCode().toLong()
    }

    fun swapDataSet(songFiles: List<File>) {
        dataSet = songFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, index: Int) {
        val file = dataSet[index]
        holder.itemView.isActivated = isChecked(file)
        holder.shortSeparator?.let {
            it.visibility =
                if (holder.bindingAdapterPosition == itemCount - 1) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }
        holder.title?.let {
            it.text = getFileTitle(file)
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

    private fun getFileTitle(file: File): String {
        return file.name
    }

    private fun getFileText(file: File): String? {
        return if (file.isDirectory) null else readableFileSize(file.length())
    }

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
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .error(error)
                .placeholder(error)
                .animate(android.R.anim.fade_in)
                .signature(MediaStoreSignature("", file.lastModified(), 0))
                .into(holder.image)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): File {
        return dataSet[position]
    }

    override fun getName(obj: File?): String {
        return getFileTitle(obj!!)
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<File?>) {
        if (callbacks == null) return
        callbacks.onMultipleItemAction(menuItem, selection as List<File>)
    }

    override fun getSectionName(position: Int): String {
        return dataSet[position].name[0].toString().uppercase(Locale.getDefault())
    }

    inner class ViewHolder(itemView: View?) : MediaEntryViewHolder(itemView) {

        init {
            if (menu != null && callbacks != null) {
                menu!!.setOnClickListener { v: View? ->
                    val position = bindingAdapterPosition
                    if (isPositionInRange(position)) {
                        callbacks.onFileMenuClicked(dataSet[position], v)
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

        override fun onLongClick(view: View): Boolean {
            val position = bindingAdapterPosition
            return isPositionInRange(position) && toggleChecked(position)
        }

        private fun isPositionInRange(position: Int): Boolean {
            return position >= 0 && position < dataSet.size
        }
    }

    companion object {
        private const val FILE = 0
        private const val FOLDER = 1

        fun readableFileSize(size: Long): String {
            if (size <= 0) return "$size B"

            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups =
                (log10(size.toDouble()) / log10(1024.0)).toInt()
            return DecimalFormat("#,##0.##").format(
                size / 1024.0.pow(digitGroups.toDouble())
            ) + " " + units[digitGroups]
        }
    }

    interface Callbacks {
        fun onFileSelected(file: File?)
        fun onFileMenuClicked(file: File?, view: View?)
        fun onMultipleItemAction(item: MenuItem?, files: List<File>?)
    }
}
