/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.google.android.material.chip.Chip
import lib.phonograph.dialog.LargeDialog
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import mt.pref.ThemeColor
import mt.util.color.lightenColor
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.DialogLyricsBinding
import player.phonograph.misc.MusicProgressViewUpdateHelper
import player.phonograph.model.lyrics.DEFAULT_TITLE
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.model.lyrics.TextLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.player.LyricsViewModel
import player.phonograph.util.reportError
import player.phonograph.util.text.lyricsTimestamp
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.warning
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.regex.Pattern

/**
 * Large Dialog to show Lyrics.
 *
 * **MUST** be created from a view-model owner possessing [LyricsViewModel]
 */
class LyricsDialog : LargeDialog(), MusicProgressViewUpdateHelper.Callback {

    private var _viewBinding: DialogLyricsBinding? = null
    val binding: DialogLyricsBinding get() = _viewBinding!!

    private val viewModel: LyricsViewModel by viewModels({ requireParentFragment() })

    //region Fragment LifeCycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = DialogLyricsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lyricsInfo: LyricsInfo = viewModel.lyricsInfo.value

        updateChips(lyricsInfo)
        updateTitle(lyricsInfo)
        setupRecycleView(lyricsInfo)
        scroller = LyricsSmoothScroller(view.context)
        progressUpdater = MusicProgressViewUpdateHelper(this@LyricsDialog, 500, 1000)
        progressUpdater.start()

        // corner
        requireDialog().window!!.setBackgroundDrawable(GradientDrawable().apply {
            this.cornerRadius = 0f
            setColor(
                requireContext().theme.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorBackgroundFloating))
                    .getColor(0, 0)
            )
        })
        binding.ok.setOnClickListener { requireDialog().dismiss() }
        binding.viewStub.setOnClickListener { requireDialog().dismiss() }
        setupFollowing(lyricsInfo)
//        scrollingOffset = binding.root.height / 4
        observe()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressUpdater.destroy()
        _viewBinding = null
    }

    //endregion


    private fun observe() {
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


    //region Chip & Title


    private var chipSelected: Chip? = null
    private fun updateChips(info: LyricsInfo) {
        binding.types.removeAllViews()
        binding.types.isSingleSelection = true
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
        val lyricsInfo = viewModel.lyricsInfo.value
        if (lyricsInfo.isActive(index)) return // do not change
        viewModel.forceReplaceLyrics(lyricsInfo[index])
        chip.isChecked = true
        chip.chipBackgroundColor = correctChipBackgroundColor(true)
        chip.setTextColor(correctChipTextColor(true))
        chipSelected?.isChecked = false
        chipSelected?.chipBackgroundColor = correctChipBackgroundColor(false)
        chipSelected?.setTextColor(correctChipTextColor(false))
        chipSelected = chip
    }

    private fun updateTitle(info: LyricsInfo) {
        val activated = info.activatedLyrics
        binding.title.text = if (activated != null && activated.getTitle() != DEFAULT_TITLE) {
            activated.getTitle()
        } else {
            info.linkedSong.title
        }
    }

    //endregion


    //region Manual Load
    private fun manualLoadLyrics() {
        val activity = requireActivity()
        val accessor = activity as? IOpenFileStorageAccess
        if (accessor != null) {
            accessor.openFileStorageAccessTool.launch(OpenDocumentContract.Config(arrayOf("*/*"))) { uri ->
                lifecycleScope.launch(Dispatchers.IO) {
                    while (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) yield()
                    viewModel.insert(activity, uri)
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
        lyricsAdapter = LyricsAdapter(
            requireContext(), lyrics.getLyricsTimeArray(), lyrics.getLyricsLineArray()
        ) { dialog?.dismiss() }
        binding.recyclerViewLyrics.apply {
            layoutManager = this@LyricsDialog.linearLayoutManager
            adapter = this@LyricsDialog.lyricsAdapter
        }
    }

    private fun updateRecycleView(info: LyricsInfo) {
        val activated = info.activatedLyrics ?: info.first()
        lyricsAdapter.update(activated.getLyricsTimeArray(), activated.getLyricsLineArray())
    }
    //endregion


    //region Scroll

    private lateinit var progressUpdater: MusicProgressViewUpdateHelper

    private fun setupFollowing(info: LyricsInfo) {
        binding.lyricsFollowing.apply {
            buttonTintList = backgroundCsl
            setOnCheckedChangeListener { button: CompoundButton, newValue: Boolean ->
                viewModel.requireLyricsFollowing.update {
                    if (info.activatedLyrics is LrcLyrics) {
                        newValue
                    } else {
                        // text lyrics can not follow
                        button.isChecked = false
                        false
                    }
                }
            }
        }
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        val lrcLyrics = viewModel.lyricsInfo.value.activatedLyrics as? LrcLyrics ?: return
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

    private val accentColor by lazy { ThemeColor.accentColor(App.instance) }
    private val primaryColor by lazy { ThemeColor.primaryColor(App.instance) }
    private val textColor by lazy { App.instance.primaryTextColor(App.instance.nightMode) }


    private fun correctChipBackgroundColor(checked: Boolean) = ColorStateList.valueOf(
        if (checked) lightenColor(primaryColor)
        else resources.getColor(R.color.defaultFooterColor, requireContext().theme)
    )

    private fun correctChipTextColor(checked: Boolean) = ColorStateList.valueOf(
        if (checked) requireContext().secondaryTextColor(primaryColor)
        else textColor
    )

    private val backgroundCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ), intArrayOf(
                lightenColor(primaryColor),
                lightenColor(primaryColor),
                resources.getColor(R.color.defaultFooterColor, requireContext().theme)
            )
        )
    }
    private val textColorCsl: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ), intArrayOf(requireContext().primaryTextColor(primaryColor), textColor)
        )
    }
    //endregion

}

private class LyricsAdapter(
    private val context: Context,
    stamps: IntArray,
    lines: Array<String>,
    private val dismiss: (() -> Unit)?,
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    private var lyrics = lines
    private var timeStamps = stamps

    @SuppressLint("NotifyDataSetChanged")
    fun update(stamps: IntArray, lines: Array<String>) {
        lyrics = lines
        timeStamps = stamps
        notifyDataSetChanged()
    }

    class ViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val line: TextView = itemView.findViewById(R.id.dialog_lyrics_line)
        val time: TextView = itemView.findViewById(R.id.dialog_lyrics_times)

        fun bind(context: Context, lyrics: Array<String>, timeStamps: IntArray, dismiss: (() -> Unit)?) {
            // parse line feed
            val actual = StringBuffer()
            lyrics[bindingAdapterPosition].split(Pattern.compile("\\\\[nNrR]")).forEach {
                actual.append(it).appendLine()
            }

            time.text = lyricsTimestamp(timeStamps[bindingAdapterPosition])
            time.setTextColor(context.getColor(R.color.dividerColor))
            if (timeStamps[bindingAdapterPosition] < 0 || !Setting.instance.displaySynchronizedLyricsTimeAxis)
                time.visibility = View.GONE

            line.text = actual.trim().toString()

            line.setOnLongClickListener {
                MusicPlayerRemote.seekTo(timeStamps[bindingAdapterPosition])
                dismiss?.invoke()
                true
            }
            line.typeface = Typeface.DEFAULT
            time.typeface = Typeface.DEFAULT
        }

        fun highlight(highlight: Boolean) {
            line.typeface = if (highlight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            time.typeface = if (highlight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        companion object {
            fun inflate(context: Context, parent: ViewGroup) =
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_lyrics, parent, false))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder.inflate(context, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(context, lyrics, timeStamps, dismiss)
    }

    override fun getItemCount(): Int = lyrics.size

}