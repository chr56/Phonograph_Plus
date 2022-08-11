package player.phonograph.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.heinrichreimersoftware.materialintro.view.InkPageIndicator
import player.phonograph.R
import player.phonograph.settings.Setting
import player.phonograph.model.NowPlayingScreen
import player.phonograph.util.ViewUtil
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class NowPlayingScreenPreferenceDialog : DialogFragment(), OnPageChangeListener {
    private var viewPagerPosition = 0
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams") val view = LayoutInflater.from(context).inflate(R.layout.preference_dialog_now_playing_screen, null)
        val viewPager: ViewPager = view.findViewById(R.id.now_playing_screen_view_pager)
        viewPager.adapter = NowPlayingScreenAdapter(context)
        viewPager.addOnPageChangeListener(this)
        viewPager.pageMargin = ViewUtil.convertDpToPixel(32f, resources).toInt()
        viewPager.currentItem = Setting.instance.nowPlayingScreen.ordinal
        val pageIndicator: InkPageIndicator = view.findViewById(R.id.page_indicator)
        pageIndicator.setViewPager(viewPager)
        pageIndicator.onPageSelected(viewPager.currentItem)
        val dialog = MaterialDialog(requireContext())
            .title(R.string.pref_title_now_playing_screen_appearance)
            .positiveButton(android.R.string.ok) {
                Setting.instance.nowPlayingScreen = NowPlayingScreen.values()[viewPagerPosition]
            }
            .negativeButton(android.R.string.cancel)
            .customView(view = view, dialogWrapContent = false)
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        return dialog
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        viewPagerPosition = position
    }

    override fun onPageScrollStateChanged(state: Int) {}
    private class NowPlayingScreenAdapter(private val context: Context?) : PagerAdapter() {
        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val nowPlayingScreen = NowPlayingScreen.values()[position]
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
            return NowPlayingScreen.values().size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context!!.getString(NowPlayingScreen.values()[position].titleRes)
        }
    }

    companion object {
        fun newInstance(): NowPlayingScreenPreferenceDialog {
            return NowPlayingScreenPreferenceDialog()
        }
    }
}
