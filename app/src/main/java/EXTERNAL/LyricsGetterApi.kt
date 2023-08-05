@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
package cn.lyric.getter.api.tools

import android.content.Context

object EventTools {
    const val API_VERSION = 4
    fun hasEnable() = false


    /**
     * 发送歌词
     *
     * @param context     Context
     * @param lyric       歌词
     * @param packageName 音乐包名
     */
    fun sendLyric(context: Context, lyric: String, packageName: String) {
        sendLyric(context, lyric, false, "", false, "", packageName)
    }

    /**
     * 把歌词
     *
     * @param context               Context
     * @param lyric                 歌词
     * @param customIcon            是否传入自定义图标
     * @param base64Icon            Base64图标，仅在customIcon为true时生效
     * @param useOwnMusicController 音乐软件自己控制歌词暂停
     * @param serviceName           音乐服务名称，仅在useOwnMusicController为false时生效
     * @param packageName           音乐包名
     */
    fun sendLyric(context: Context, lyric: String, customIcon: Boolean, base64Icon: String, useOwnMusicController: Boolean, serviceName: String, packageName: String) {}

    fun stopLyric(context: Context) {}

}