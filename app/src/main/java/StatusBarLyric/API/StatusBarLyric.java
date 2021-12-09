/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package StatusBarLyric.API;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.ByteArrayOutputStream;

import player.phonograph.util.PreferenceUtil;

@Keep
public class StatusBarLyric {

    String icon;
    Context context;
    String serviceName;
    boolean useSystemMusicActive;

    /**
     * InitStatusBarLyric / 初始化状态栏歌词 &nbsp;
     * <p>
     * Call {@link #sendLyric} to send lyrics, Call {@link #stopLyric} to clear (of course, your app will be cleared if killed), Call {@link #hasEnable} to determine whether to activate the Xposed module.
     * </p><p>
     * 调用{@link #sendLyric}发送歌词, 调用{@link #stopLyric}清除(当然你的应用被杀死也会清除), 调用{@link #hasEnable}判断是否激活模块.
     * </p>
     *
     * @param context              context
     * @param drawable             (notification) icon (you can use your music service's notification icon), Null: do not display icon, Drawable, format should be webp. 通知栏图标, null 为不显示图标, (Webp格式, Drawable)
     * @param serviceName          ServiceName, for example (demo.abc.Service) 服务名称, 例如 (demo.abc.Service)
     * @param useSystemMusicActive detect your music service running status via system. 是否使用系统检测音乐是否播放
     */
    public StatusBarLyric(Context context, Drawable drawable, String serviceName, boolean useSystemMusicActive) {
        icon = drawableToBase64(drawable);
        this.context = context;
        this.useSystemMusicActive = useSystemMusicActive;
        this.serviceName = serviceName;
    }

    /**
     * SendLyric / 发送歌词 &nbsp;
     * <p>
     * this function will broadcast a intent containing a single line lyrics, which would be displayed (and remained) on statusbar until you send another intent or you call {@link #stopLyric} manually or your app is killed.
     * </p><p>
     * 发送单行歌词的广播Intent, 歌词将一直停留在状态栏! 调用{@link #stopLyric}清除(当然你的应用被杀死也会清除)
     * </p>
     *
     * @param lyric A single line lyrics 单行歌词
     */
    public void updateLyric(String lyric) {
        sendLyric(context, lyric, icon, serviceName, useSystemMusicActive);
    }


    /**
     * Whether to activate the Xposed module / 是否激活模块 &nbsp;
     * <p>
     * Get whether the Xposed module is activated for this software
     * </p><p>
     * 获取模块是否对本软件激活
     * </p>
     */
    public boolean hasEnable() {
        return false;
    }

    /**
     * StopLyric (useSystemMusicActive for 'true' No need to use) / 停止播放 (useSystemMusicActive 为 'true' 无需使用)
     */
    public void stopLyric() {
        stopLyric(context);
    }

    protected String drawableToBase64(Drawable drawable) {
        if (drawable == null) {
            return "";
        }
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\n","");
    }


    @Keep
    protected void sendLyric(Context context, String lyric, String icon, String serviceName, boolean useSystemMusicActive) {

        if (!hasEnable() && PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) {
            Log.d("statusbar_lyric", "use fallback: " + lyric);
            if (!lyric.isEmpty()) {
                context.sendBroadcast(
                        new Intent().setAction("Lyric_Server")
                                .putExtra("Lyric_Type", "app")
                                .putExtra("Lyric_Data", lyric)
                                .putExtra("Lyric_PackName", serviceName)
                                // Actually, PackName is (music) service name, so we have no suffix (.plus.BUILD_TYPE)
                                .putExtra("Lyric_Icon", icon)
                                .putExtra("Lyric_UseSystemMusicActive", useSystemMusicActive)
                );
            }
        }
    }

    @Keep
    protected void stopLyric(Context context) {
        if (!hasEnable()) {
            context.sendBroadcast(
                    new Intent().setAction("Lyric_Server").putExtra("Lyric_Type", "app_stop")
            );
        }
    }

}
