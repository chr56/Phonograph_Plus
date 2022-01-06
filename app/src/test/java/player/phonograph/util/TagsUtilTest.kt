/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import kotlin.collections.forEach

internal class TagsUtilTest {

    @org.junit.Test
    fun testParse() {

        val data: Array<String> = arrayOf(
            "a",
            "b&c",
            "a;d"
        )

        data.forEach {

            val result = TagsUtil.parse(it)
            if (result == null) org.junit.Assert.fail("no result")
            else result.forEach { s ->
                println(s)
                org.junit.Assert.assertNotEquals("empty", "", s)
            }
            println()
        }


//        MediaStoreUtil.getAllSongs(App.instance)
//            .forEach { song ->
//            song?.let {
//                val result = player.phonograph.util.TagsUtil.parse(it.artistName)
//                if (result == null) org.junit.Assert.fail("no result")
//                else result.forEach { s ->
//                    org.junit.Assert.assertNotEquals("empty", "", s)
//                }
//            }
//        }
    }
}
