/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.dialogs

import com.google.android.material.chip.Chip
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import player.phonograph.R
import player.phonograph.databinding.DialogLyricsBinding
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.model.lyrics.TextLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.player.LyricsViewModel
import player.phonograph.util.MusicProgressViewUpdateHelper
import player.phonograph.util.reportError
import player.phonograph.util.text.lyricsTimestamp
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.ui.applyLargeDialog
import player.phonograph.util.warning
import util.theme.color.lightenColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Large Dialog to show Lyrics.
 *
 * **MUST** be created from a view-model owner possessing [LyricsViewModel]
 */
class LyricsDialog : DialogFragment(), MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private val viewModel: LyricsViewModel by viewModels({ requireActivity() })

    //region Fragment LifeCycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.ok.setOnClickListener { requireDialog().dismiss() }
        binding.viewStub.setOnClickListener { requireDialog().dismiss() }

        scroller = LyricsSmoothScroller(view.context)
        progressUpdater = MusicProgressViewUpdateHelper(this@LyricsDialog, 500, 1000)
        progressUpdater.start()
        requireDialog().window!!.setBackgroundDrawable(GradientDrawable().apply {
            this.cornerRadius = 0f
            setColor(
                requireContext().theme.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorBackgroundFloating))
                    .getColor(0, 0)
            )
        })

        val lyricsInfo: LyricsInfo? = viewModel.lyricsInfo.value
        if (lyricsInfo == null) {
            dismissNow()
            return
        }

        updateChips(lyricsInfo)
        updateTitle(lyricsInfo)
        setupRecycleView(lyricsInfo)

        setupFollowing(lyricsInfo)

        lifecycleScope.launch {
            viewModel.lyricsInfo.collect { info ->
                withContext(Dispatchers.Main) {
                    updateTitle(info)
                    updateChips(info)
                    updateRecycleView(info)
                    lastHighlightPosition = -1
                }
            }
        }
        lifecycleScope.launch {
            viewModel.requireLyricsFollowing.collect {
                binding.lyricsFollowing.isChecked = it
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressUpdater.destroy()
        _viewBinding = null
    }

    override fun onStart() {
        super.onStart()
        applyLargeDialog()
    }

    //endregion


    //region Chip & Title


    private var chipSelected: Chip? = null
    private fun updateChips(info: LyricsInfo?) {
        binding.types.removeAllViews()
        binding.types.isSingleSelection = true
        if (info == null) return
        for ((index, lyrics) in info.withIndex()) {
            val requireCheck = info.isActive(index)
            val chip = createChip(
                lyrics.source.name(requireContext()), index, requireCheck, null, this::onChipClicked
            )
            binding.types.addView(chip)
            if (requireCheck) chipSelected = chip
        }
        binding.types.addView(createChip(
            getString(R.string.action_load),
            -1,
            false,
            requireContext().getTintedDrawable(R.drawable.ic_add_white_24dp, Color.BLACK)
        ) { _, _ -> manualLoadLyrics() })
        // binding.types.isSelectionRequired = true
    }


    private fun createChip(
        label: String,
        index: Int,
        checked: Boolean = false,
        icon: Drawable? = null,
        callback: (Chip, Int) -> Unit,
    ) = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice).apply {
        text = label
        isChecked = checked
        setTextColor(correctChipTextColor(checked))
        chipBackgroundColor = correctChipBackgroundColor(checked)
        setOnClickListener {
            callback(it as Chip, index)
        }
        if (icon != null) chipIcon = icon
    }

    private fun onChipClicked(chip: Chip, index: Int) {
        val lyricsInfo = viewModel.lyricsInfo.value ?: return
        if (lyricsInfo.isActive(index)) return // do not change
        lifecycleScope.launch {
            viewModel.activateLyrics(lyricsInfo[index])
            chip.isChecked = true
            chip.chipBackgroundColor = correctChipBackgroundColor(true)
            chip.setTextColor(correctChipTextColor(true))
            chipSelected?.isChecked = false
            chipSelected?.chipBackgroundColor = correctChipBackgroundColor(false)
            chipSelected?.setTextColor(correctChipTextColor(false))
            chipSelected = chip
        }
    }

    private fun updateTitle(info: LyricsInfo?) {
        val activated = info?.activatedLyrics
        binding.title.text = if (activated != null) activated.title else AbsLyrics.DEFAULT_TITLE
    }

    //endregion


    //region Manual Load
    private fun manualLoadLyrics() {
        val activity = requireActivity()
        val accessor = activity as? IOpenFileStorageAccessible
        if (accessor != null) {
            accessor.openFileStorageAccessDelegate.launch(OpenDocumentContract.Config(arrayOf("*/*"))) { uri ->
                if (uri == null) return@launch
                CoroutineScope(Dispatchers.IO).launch {
                    val lyricsViewModel = ViewModelProvider(activity)[LyricsViewModel::class.java]
                    lyricsViewModel.appendLyricsFrom(activity, uri)
                }
            }
        } else {
            warning("Lyrics", "Can not open file from $activity")
        }
    }
    //endregion

    //region RecycleView
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private fun setupRecycleView(lyricsInfo: LyricsInfo) {
        val lyrics =
            lyricsInfo.activatedLyrics ?: lyricsInfo.getOrElse(0) { TextLyrics.from("NOT FOUND!?") }
        linearLayoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        lyricsAdapter = LyricsAdapter(requireContext(), lyrics) { dialog?.dismiss() }
        binding.recyclerViewLyrics.apply {
            layoutManager = this@LyricsDialog.linearLayoutManager
            adapter = this@LyricsDialog.lyricsAdapter
        }
    }

    private fun updateRecycleView(info: LyricsInfo?) {
        val activated = info?.activatedLyrics
        if (activated != null) {
            binding.recyclerViewLyrics.visibility = View.VISIBLE
            lyricsAdapter.update(activated)
        } else {
            binding.recyclerViewLyrics.visibility = View.INVISIBLE
        }
    }
    //endregion


    //region Scroll

    private lateinit var progressUpdater: MusicProgressViewUpdateHelper

    private fun setupFollowing(info: LyricsInfo?) {
        binding.lyricsFollowing.apply {
            buttonTintList = backgroundCsl
            setOnCheckedChangeListener { button: CompoundButton, newValue: Boolean ->
                viewModel.updateRequireLyricsFollowing(
                    if (info?.activatedLyrics is LrcLyrics) {
                        newValue
                    } else {
                        // text lyrics can not follow
                        button.isChecked = false
                        false
                    }
                )
            }
        }
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        val lyrics = viewModel.lyricsInfo.value?.activatedLyrics
        val lrcLyrics = lyrics as? LrcLyrics ?: return
        val position = lrcLyrics.getPosition(progress)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            updateHighlight(position)
            if (viewModel.requireLyricsFollowing.value) {
                scrollingTo(position)
            }
        }
    }

    private lateinit var scroller: LyricsSmoothScroller

    class LyricsSmoothScroller(context: Context) : LinearSmoothScroller(context) {

        private val densityDpi = context.resources.displayMetrics.densityDpi
        private val minDeviation = densityDpi / 10
        private val offset = densityDpi / 5

        override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
            val dyStart = calculateDyToMakeVisible(targetView, SNAP_TO_START)
            val dyEnd = calculateDyToMakeVisible(targetView, SNAP_TO_END)
            var dy = (dyEnd + dyStart) / 2
            if (abs(dy) < minDeviation) dy = 0  // omit slight deviation
            dy -= offset // slightly upper
            // debug { Log.v("SmoothScroller", "dy:$dy dyStart:$dyStart dyEnd:$dyEnd") }
            val time = calculateTimeForDeceleration(dy)
            if (time > 0) {
                action.update(0, -dy, time, mDecelerateInterpolator)
            }
        }
    }

    private fun scrollingTo(position: Int) {
        try {
            if (position >= 0) {
                scroller.targetPosition = position
                linearLayoutManager.startSmoothScroll(scroller)
            }
        } catch (e: Exception) {
            reportError(e, "LyricsScroll", "Failed to scroll to $position")
        }
    }

    private var lastHighlightPosition = -1
    private fun updateHighlight(position: Int) {
        if (lastHighlightPosition >= 0) {
            // cancel last
            val lastViewHolder =
                binding.recyclerViewLyrics.findViewHolderForAdapterPosition(lastHighlightPosition) as? LyricsAdapter.ViewHolder
            lastViewHolder?.highlight(false)
        }
        if (position >= 0) {
            val viewHolder =
                binding.recyclerViewLyrics.findViewHolderForAdapterPosition(position) as? LyricsAdapter.ViewHolder
            viewHolder?.highlight(true)
            lastHighlightPosition = position
        }
    }

    //endregion


    //region Theme& Color

    private fun correctChipBackgroundColor(checked: Boolean) = ColorStateList.valueOf(
        if (checked) lightenColor(primaryColor())
        else themeFooterColor(requireContext())
    )

    private fun correctChipTextColor(checked: Boolean) = ColorStateList.valueOf(
        if (checked) requireContext().secondaryTextColor(primaryColor())
        else requireContext().primaryTextColor(primaryColor())
    )

    private val backgroundCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ), intArrayOf(
                lightenColor(primaryColor()),
                lightenColor(primaryColor()),
                themeFooterColor(requireContext())
            )
        )
    }
    private val textColorCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(
                requireContext().primaryTextColor(primaryColor()),
                requireContext().primaryTextColor(requireContext().nightMode)
            )
        )
    }
    //endregion

}

private class LyricsAdapter(
    private val context: Context,
    private var lyric: AbsLyrics,
    private val dismiss: (() -> Unit)?,
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    private var lyricLines: Array<String> = lyric.lyricsLineArray
    private var lyricTimestamps: IntArray = lyric.lyricsTimeArray

    @SuppressLint("NotifyDataSetChanged")
    fun update(newLyric: AbsLyrics) {
        lyric = newLyric
        lyricLines = newLyric.lyricsLineArray
        lyricTimestamps = newLyric.lyricsTimeArray
        notifyDataSetChanged()
    }

    class ViewHolder private constructor(itemView: View, val enableTimestamp: Boolean) :
            RecyclerView.ViewHolder(itemView) {

        val textLine: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val textTime: TextView = itemView.findViewById(R.id.dialog_lyrics_times)

        fun bindImpl(line: String, showTimestamp: Boolean, timestamp: Int, dismiss: (() -> Unit)?) {

            // parse line feed
            val actual = StringBuffer()
            line.split(Pattern.compile("\\\\[nNrR]")).forEach {
                actual.append(it).appendLine()
            }

            // Text Line
            textLine.text = actual.trim().toString()
            textLine.typeface = Typeface.DEFAULT
            textLine.setOnLongClickListener {
                if (timestamp >= 0) {
                    MusicPlayerRemote.seekTo(timestamp)
                    dismiss?.invoke()
                }
                true
            }

            // Text Timestamp
            if (showTimestamp) {
                textTime.text = lyricsTimestamp(timestamp)
                textTime.typeface = Typeface.DEFAULT
                textTime.visibility = View.VISIBLE
            } else {
                textTime.visibility = View.GONE
            }
        }

        fun bind(line: String, dismiss: (() -> Unit)?) =
            bindImpl(line, false, -1, dismiss)

        fun bind(line: String, timestamp: Int, dismiss: (() -> Unit)?) =
            bindImpl(line, enableTimestamp, timestamp, dismiss)

        fun highlight(highlight: Boolean) {
            textLine.typeface = if (highlight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textTime.typeface = if (highlight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        companion object {
            fun inflate(context: Context, parent: ViewGroup, enableTimestamp: Boolean) =
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_lyrics, parent, false), enableTimestamp)
        }
    }

    private val enableTimestamp: Boolean = Setting(context)[Keys.displaySynchronizedLyricsTimeAxis].data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.inflate(context, parent, enableTimestamp)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (lyric is LrcLyrics) {
            holder.bind(lyricLines[position], lyricTimestamps[position], dismiss)
        } else if (lyric is TextLyrics) {
            holder.bind(lyricLines[position], dismiss)
        }
    }

    override fun getItemCount(): Int = lyric.length

}