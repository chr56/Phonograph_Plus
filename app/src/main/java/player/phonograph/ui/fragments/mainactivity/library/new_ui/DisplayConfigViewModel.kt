/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.library.new_ui

import androidx.lifecycle.ViewModel
import player.phonograph.App
import player.phonograph.util.PreferenceUtil

class DisplayConfigViewModel : ViewModel() {
    fun getSortOrder(page: AbsDisplayPage<*, *>): String {
        val pref = PreferenceUtil.getInstance(App.instance)
        return when (page) {
            is SongPage -> {
                pref.songSortOrder
            }
            else -> { "" }
        }
    }
    fun setSortOrder(page: AbsDisplayPage<*, *>, value: String) {
        if (value.isBlank()) return

        val pref = PreferenceUtil.getInstance(App.instance)
        // todo valid input
        when (page) {
            is SongPage -> {
                pref.songSortOrder = value
            }
            else -> {}
        }
    }
    fun getGridSize(page: AbsDisplayPage<*, *>, isLand: Boolean): Int {

        val pref = PreferenceUtil.getInstance(App.instance)

        return when (page) {
            is SongPage -> {
                if (isLand) pref.songGridSizeLand
                else pref.songGridSize
            }
            else -> 1
        }
    }
    fun setGridSize(page: AbsDisplayPage<*, *>, isLand: Boolean, value: Int) {
        if (value <= 0) return
        val pref = PreferenceUtil.getInstance(App.instance)
        // todo valid input
        when (page) {
            is SongPage -> {
                if (isLand) pref.songGridSizeLand = value
                else pref.songGridSize = value
            }
            else -> {}
        }
    }
    fun getPaletteSetting(page: AbsDisplayPage<*, *>): Boolean {
        val pref = PreferenceUtil.getInstance(App.instance)
        return when (page) {
            is SongPage -> {
                pref.songColoredFooters()
            }
            else -> false
        }
    }
    fun setPaletteSetting(page: AbsDisplayPage<*, *>, value: Boolean) {
        val pref = PreferenceUtil.getInstance(App.instance)
        // todo valid input
        when (page) {
            is SongPage -> {
                pref.setSongColoredFooters(value)
            }
            else -> {}
        }
    }
}
