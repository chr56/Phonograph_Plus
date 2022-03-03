package player.phonograph.adapter

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.R
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil
import java.util.regex.Pattern

class LyricsAdapter(private val context: Activity, stamps: IntArray, lines: Array<CharSequence>, private val callbackDialog: Dialog? = null) :
    RecyclerView.Adapter<LyricsAdapter.VH>() {
    private var lyrics = lines
    private var timeStamps = stamps

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val line: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val time: TextView = itemView.findViewById(R.id.dialog_lyrics_times)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_lyrics, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        // parse line feed
        val b = StringBuffer()
        lyrics[position].split(Pattern.compile("\\\\[nNrR]")).forEach {
            b.append(it).appendLine()
        }

        holder.time.text = MusicUtil.parseTimeStamp(timeStamps[position])
        holder.time.setTextColor(context.resources.getColor(R.color.dividerColor))
        if (timeStamps[position] < 0 || !Setting.instance.displaySynchronizedLyricsTimeAxis)
            holder.time.visibility = View.GONE

        holder.line.text = b.trim().toString()

        holder.line.setOnLongClickListener {
            MusicPlayerRemote.seekTo(timeStamps[position])
            callbackDialog?.dismiss()
            true
        }
    }

    override fun getItemCount(): Int {
        return lyrics.size
    }
}
