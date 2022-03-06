/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package util.phonograph.lastfm.rest

import util.phonograph.lastfm.rest.model.LastFmAlbum
import util.phonograph.lastfm.rest.model.LastFmArtist
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object LastFMUtil {

    fun getLargestArtistImageUrl(images: List<LastFmArtist.Artist.Image>): String? {
        val imageUrls: MutableMap<ImageSize, String> = HashMap()
        for (image in images) {
            var size: ImageSize? = null
            val attribute = image.size
            if (attribute == null) {
                size = ImageSize.UNKNOWN
            } else {
                try {
                    size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH))
                } catch (e: IllegalArgumentException) {
                    // if they suddenly again introduce a new image size
                }
            }
            if (size != null) {
                imageUrls[size] = image.text
            }
        }
        return getLargestImageUrl(imageUrls)
    }

    @JvmStatic
    fun getLargestAlbumImageUrl(images: List<LastFmAlbum.Album.Image>): String? {
        val imageUrls: MutableMap<ImageSize, String> = HashMap()
        for (image in images) {
            var size: ImageSize? = null
            val attribute = image.size
            if (attribute == null) {
                size = ImageSize.UNKNOWN
            } else {
                try {
                    size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH))
                } catch (e: IllegalArgumentException) {
                    // if they suddenly again introduce a new image size
                }
            }
            if (size != null) {
                imageUrls[size] = image.text
            }
        }
        return getLargestImageUrl(imageUrls)
    }

    private fun getLargestImageUrl(imageUrls: Map<ImageSize, String>): String? {
        if (imageUrls.containsKey(ImageSize.MEGA)) {
            return imageUrls[ImageSize.MEGA]
        }
        if (imageUrls.containsKey(ImageSize.EXTRALARGE)) {
            return imageUrls[ImageSize.EXTRALARGE]
        }
        if (imageUrls.containsKey(ImageSize.LARGE)) {
            return imageUrls[ImageSize.LARGE]
        }
        if (imageUrls.containsKey(ImageSize.MEDIUM)) {
            return imageUrls[ImageSize.MEDIUM]
        }
        if (imageUrls.containsKey(ImageSize.SMALL)) {
            return imageUrls[ImageSize.SMALL]
        }
        return if (imageUrls.containsKey(ImageSize.UNKNOWN)) {
            imageUrls[ImageSize.UNKNOWN]
        } else null
    }

    enum class ImageSize {
        SMALL, MEDIUM, LARGE, EXTRALARGE, MEGA, UNKNOWN
    }
}
