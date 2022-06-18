/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.databinding.ItemTextBinding
import player.phonograph.model.Location
import util.mdcolor.pref.ThemeColor

@Suppress("JoinDeclarationAndAssignment")
class BreadCrumbView : FrameLayout {

    constructor (context: Context) : super(context)
    constructor (context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor (context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context, attrs, defStyleAttr, defStyleRes
    )

    var callBack: (Location) -> Unit
        get() = adapter.callBack
        set(value) {
            adapter.callBack = value
        }

    var location: Location = Location.HOME
        set(value) {
            field = value
            adapter.location = value
        }

    val recyclerView: RecyclerView
    val adapter: BreadCrumbAdapter
    val layoutManager: LinearLayoutManager

    init {
        recyclerView = RecyclerView(context)
        adapter = BreadCrumbAdapter(context, location)
        layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager

        addView(
            recyclerView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.CENTER }
        )
    }

    class BreadCrumbAdapter(
        val context: Context,
        location: Location,
        var callBack: (Location) -> Unit = {}
    ) : RecyclerView.Adapter<BreadCrumbAdapter.ViewHolder>() {

        var location: Location = location
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                crumbs.clear()
                crumbs.addAll(generateCrumbs(value.basePath))
                notifyDataSetChanged()
            }

        val volumeName: String get() = location.storageVolume.getDescription(context)
        val crumbs: MutableList<String> = generateCrumbs(location.basePath).toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemTextBinding.inflate(LayoutInflater.from(context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder.viewBinding.text) {
                text = if (position == 0) volumeName else crumbs[position - 1]
                setTextColor(ThemeColor.textColorPrimary(context))
            }
            holder.itemView.setOnClickListener {
                callBack(
                    Location.from(
                        crumbs.subList(0, position).fold("") { acc, s -> "$acc/$s" },
                        location.storageVolume
                    )
                )
            }
        }

        private fun generateCrumbs(path: String): List<String> =
            path.removePrefix("/").split("/")

        override fun getItemCount(): Int = crumbs.size + 1

        class ViewHolder constructor(
            val viewBinding: ItemTextBinding
        ) : RecyclerView.ViewHolder(viewBinding.root)
    }
}
