package player.phonograph.ui.fragments.mainactivity.library.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import util.mdcolor.pref.ThemeColor
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.databinding.FragmentMainActivityRecyclerViewBinding
import player.phonograph.util.ViewUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsLibraryPagerRecyclerViewFragment<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsLibraryPagerFragment(), OnOffsetChangedListener {

    private var _viewBinding: FragmentMainActivityRecyclerViewBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewBinding = FragmentMainActivityRecyclerViewBinding.inflate(inflater, container, false)
        bind()
        return binding.root
    }
    protected var container: View? = null
    protected var recyclerView: RecyclerView? = null
    protected var empty: TextView? = null
    private fun bind() {
        container = binding.container
        recyclerView = binding.recyclerView
        empty = binding.empty
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryFragment!!.addOnAppBarOffsetChangedListener(this)

        initLayoutManager()
        initAdapter()
        setUpRecyclerView()
    }

    protected var adapter: A? = null
        private set
    protected var layoutManager: LM? = null
        private set

    private fun setUpRecyclerView() {
//        if (binding.recyclerView is FastScrollRecyclerView) {
        ViewUtil.setUpFastScrollRecyclerViewColor(
            requireActivity(), binding.recyclerView as FastScrollRecyclerView?, ThemeColor.accentColor(requireActivity())
        )
//        }
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.adapter = adapter
    }

    protected fun invalidateLayoutManager() {
        initLayoutManager()
        recyclerView!!.layoutManager = layoutManager
    }

    protected fun invalidateAdapter() {
        initAdapter()
        checkIsEmpty()
        recyclerView!!.adapter = adapter
    }

    private fun initAdapter() {
        adapter = createAdapter()
        adapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    private fun initLayoutManager() { layoutManager = createLayoutManager() }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        container!!.setPadding(
            container!!.paddingLeft,
            container!!.paddingTop,
            container!!.paddingRight,
            libraryFragment!!.totalAppBarScrollingRange + i
        )
    }

    private fun checkIsEmpty() {
        empty?.let {
            it.setText(emptyMessage)
            it.visibility = if (adapter == null || adapter!!.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    protected open val emptyMessage: Int
        @StringRes
        get() = R.string.empty

    protected val layoutRes: Int
        @LayoutRes
        get() = R.layout.fragment_main_activity_recycler_view

    protected abstract fun createLayoutManager(): LM
    protected abstract fun createAdapter(): A

    override fun onDestroyView() {
        super.onDestroyView()
        libraryFragment!!.removeOnAppBarOffsetChangedListener(this)
        _viewBinding = null
    }
}
