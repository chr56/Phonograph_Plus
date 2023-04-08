package player.phonograph.adapter

import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import androidx.recyclerview.widget.RecyclerView
import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.regex.Pattern

class LyricsAdapter(private val context: Activity, stamps: IntArray, lines: Array<String>, private val callbackDialog: Dialog? = null) :
    RecyclerView.Adapter<LyricsAdapter.VH>() {
    private var lyrics = lines
    private var timeStamps = stamps

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val line: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val time: TextView = itemView.findViewById(R.id.dialog_lyrics_times)
    }

    fun update(stamps: IntArray, lines: Array<String>) {
        lyrics = lines
        timeStamps = stamps
        notifyDataSetChanged()
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

        holder.time.text = parseTimeStamp(timeStamps[position])
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

    /**
     * convert a timestamp to a readable String
     *
     * @param t timeStamp to parse (Unit: milliseconds)
     * @return human-friendly time
     */
    private fun parseTimeStamp(t: Int): String {
        val ms = (t % 1000).toLong()
        val s = (t % (1000 * 60) / 1000).toLong()
        val m = (t - s * 1000 - ms) / (1000 * 60)
        return String.format("%d:%02d.%03d", m, s, ms)
    }
}
