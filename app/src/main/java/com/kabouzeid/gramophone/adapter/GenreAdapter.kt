package com.kabouzeid.gramophone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.kabouzeid.gramophone.adapter.base.MediaEntryViewHolder
import com.kabouzeid.gramophone.model.Genre
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.NavigationUtil
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter

class GenreAdapter(
    private val activity: AppCompatActivity,
    dataSet: List<Genre>,
    @LayoutRes private val itemLayoutRes: Int
) : RecyclerView.Adapter<GenreAdapter.ViewHolder>(), SectionedAdapter {

    var dataSet: List<Genre> = dataSet
        get() = field
        private set(dataSet: List<Genre>) {
            field = dataSet
        }

    fun swapDataSet(dataSet: List<Genre>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = dataSet[position]

        holder.separator?.let {
            it.visibility =
                if (holder.bindingAdapterPosition == itemCount - 1) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }
        holder.shortSeparator?.let {
            it.visibility = View.GONE
        }
        holder.menu?.let {
            it.visibility = View.GONE
        }
        holder.title?.let {
            it.text = genre.name
        }
        holder.text?.let {
            // Genre count
            it.text = MusicUtil.getGenreInfoString(activity, genre)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getSectionName(position: Int): String {
        val genre = dataSet[position]
        return if (genre.id == -1L) "" else MusicUtil.getSectionName(dataSet[position].name)
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        override fun onClick(view: View) {
            val genre = dataSet[bindingAdapterPosition]
            NavigationUtil.goToGenre(activity, genre)
        }
    }
}
