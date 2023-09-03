/*
 *  Copyright (c) 2022~2023 chr_56
 */
@file:JvmName("LastFMUtil")

package util.phonograph.tagsources.lastfm

fun List<LastFmImage>.largestUrl(): String? = maxByOrNull { it.size.ordinal }?.text
