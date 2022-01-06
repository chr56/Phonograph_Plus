/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("MemberVisibilityCanBePrivate", "ObjectPropertyName")

package player.phonograph.glide

import android.annotation.SuppressLint
import android.os.Build
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import player.phonograph.R

object GlideRequestOptions {
    const val default_animation = android.R.anim.fade_in

    const val default_image_song: Int = R.drawable.default_album_art
    const val default_image_artist = R.drawable.default_artist_image

    @JvmStatic
    val _default_disk_cache_strategy: DiskCacheStrategy = DiskCacheStrategy.NONE

    @SuppressLint("CheckResult")
    @JvmStatic
    private val default_base_option: RequestOptions =
        RequestOptions().diskCacheStrategy(_default_disk_cache_strategy)
            .also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    it.set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                // todo use preference
            }

    @JvmStatic
    val _default_option_song = default_base_option
        .placeholder(default_image_song)
        .error(default_image_song)

    @JvmStatic
    val _default_option_artist = default_base_option
        .placeholder(default_image_artist)
        .error(default_image_artist)

    @JvmStatic
    val _default_drawable_transition_options =
        DrawableTransitionOptions().transition(default_animation)

    @JvmStatic
    val _default_bitmap_transition_options =
        BitmapTransitionOptions().transition(default_animation)
}
