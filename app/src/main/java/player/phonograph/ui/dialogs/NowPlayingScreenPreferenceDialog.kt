/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.appintro.indicator.DotIndicatorController
import player.phonograph.R
import player.phonograph.model.NowPlayingScreen
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.ui.convertDpToPixel
import util.theme.color.primaryDisabledTextColor
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class NowPlayingScreenPreferenceDialog : DialogFragment(), OnPageChangeListener {
    private lateinit var pageIndicator: DotIndicatorController

    private var viewPagerPosition = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val preference = Setting(requireContext()).Composites[Keys.nowPlayingScreen]
        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.preference_dialog_now_playing_screen, null) as LinearLayout

        pageIndicator = DotIndicatorController(requireContext())
        view.addView(
            pageIndicator.newInstance(requireContext()),
            1,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER }
        )
        with(pageIndicator) {
            initialize(NowPlayingScreen.entries.size)
            selectedIndicatorColor = ThemeSetting.accentColor(requireActivity())
            unselectedIndicatorColor = requireActivity().primaryDisabledTextColor()
        }
        val viewPager: ViewPager = view.findViewById(R.id.now_playing_screen_view_pager)
        with(viewPager) {
            adapter = NowPlayingScreenAdapter(context)
            addOnPageChangeListener(this@NowPlayingScreenPreferenceDialog)
            pageMargin = convertDpToPixel(32f, resources).toInt()
            currentItem = preference.data.ordinal
        }
        val dialog = MaterialDialog(requireContext())
            .title(R.string.pref_title_now_playing_screen_appearance)
            .positiveButton(android.R.string.ok) {
                Setting(it.context).Composites[Keys.nowPlayingScreen].data = NowPlayingScreen.entries[viewPagerPosition]
            }
            .negativeButton(android.R.string.cancel)
            .customView(view = view, dialogWrapContent = false)
            .tintButtons()
        return dialog
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        viewPagerPosition = position
        pageIndicator.selectPosition(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}
    private class NowPlayingScreenAdapter(private val context: Context?) : PagerAdapter() {
        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val nowPlayingScreen = NowPlayingScreen.entries[position]
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.preference_now_playing_screen_item, collection, false) as ViewGroup
            collection.addView(layout)
            val image = layout.findViewById<ImageView>(R.id.image)
            val title = layout.findViewById<TextView>(R.id.title)
            image.setImageResource(nowPlayingScreen.drawableResId)
            title.setText(nowPlayingScreen.titleRes)
            return layout
        }

        override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
            collection.removeView(view as View)
        }

        override fun getCount(): Int {
            return NowPlayingScreen.entries.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context!!.getString(NowPlayingScreen.entries[position].titleRes)
        }
    }

    companion object {
        fun newInstance(): NowPlayingScreenPreferenceDialog {
            return NowPlayingScreenPreferenceDialog()
        }
    }
}
