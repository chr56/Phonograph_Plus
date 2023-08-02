package player.phonograph.adapter

import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.regex.Pattern

class LyricsAdapter(
    private val context: Activity,
    stamps: IntArray,
    lines: Array<String>,
    private val callbackDialog: Dialog? = null,
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    private var lyrics = lines
    private var timeStamps = stamps

    @SuppressLint("NotifyDataSetChanged")
    fun update(stamps: IntArray, lines: Array<String>) {
        lyrics = lines
        timeStamps = stamps
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val line: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val time: TextView = itemView.findViewById(R.id.dialog_lyrics_times)

        fun bind(context: Context, lyrics: Array<String>, timeStamps: IntArray, dismiss: (() -> Unit)?) {
            // parse line feed
            val b = StringBuffer()
            lyrics[bindingAdapterPosition].split(Pattern.compile("\\\\[nNrR]")).forEach {
                b.append(it).appendLine()
            }

            time.text = parseTimeStamp(timeStamps[bindingAdapterPosition])
            time.setTextColor(context.getColor(R.color.dividerColor))
            if (timeStamps[bindingAdapterPosition] < 0 || !Setting.instance.displaySynchronizedLyricsTimeAxis)
                time.visibility = View.GONE

            line.text = b.trim().toString()

            line.setOnLongClickListener {
                MusicPlayerRemote.seekTo(timeStamps[bindingAdapterPosition])
                dismiss?.invoke()
                true
            }
        }


        companion object {
            fun inflate(context: Context, parent: ViewGroup) =
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_lyrics, parent, false))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder.inflate(context, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(context, lyrics, timeStamps) {
            callbackDialog?.dismiss()
        }
    }

    override fun getItemCount(): Int = lyrics.size

    companion object {
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
}