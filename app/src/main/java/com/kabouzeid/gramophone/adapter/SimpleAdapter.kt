package com.kabouzeid.gramophone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.kabouzeid.gramophone.R

class SimpleAdapter(data: Array<String>) : RecyclerView.Adapter<SimpleAdapter.VH>() {
    var lyrics = data

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val line: TextView = itemView.findViewById<TextView>(R.id.dialog_lyrics_line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_lyrics, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.line.text = lyrics[position]
        holder.line.setOnLongClickListener {
            Toast.makeText((it.parent as View).context, "Test", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun getItemCount(): Int {
        return lyrics.size
    }
}
