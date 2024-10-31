/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.appwidgets

import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.registerReceiverCompat
import androidx.core.content.ContextCompat
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class AppWidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isPlaying = MusicPlayerRemote.isPlaying
        val ids = intent.getIntArrayExtra(EXTRA_APPWIDGET_IDS)
        val widget = intent.getStringExtra(EXTRA_APP_WIDGET_NAME)
        when (widget) {
            AppWidgetClassic.NAME -> AppWidgetClassic.instance.performUpdate(context, isPlaying, ids)
            AppWidgetSmall.NAME   -> AppWidgetSmall.instance.performUpdate(context, isPlaying, ids)
            AppWidgetBig.NAME     -> AppWidgetBig.instance.performUpdate(context, isPlaying, ids)
            AppWidgetCard.NAME    -> AppWidgetCard.instance.performUpdate(context, isPlaying, ids)
        }
    }


    companion object {
        const val ACTION_APP_WIDGET_UPDATE = "$ACTUAL_PACKAGE_NAME.app_widget_update"
        const val EXTRA_APP_WIDGET_NAME = "$ACTUAL_PACKAGE_NAME.app_widget_name"

        private var _instance: AppWidgetUpdateReceiver? = null
        val instant: AppWidgetUpdateReceiver
            @Synchronized get() {
                if (_instance == null) _instance = AppWidgetUpdateReceiver()
                return _instance!!
            }

        /**
         * register & attach this Receiver to this [context]
         */
        fun register(context: Context) {
            context.registerReceiverCompat(
                instant,
                IntentFilter(ACTION_APP_WIDGET_UPDATE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        /**
         * unregister & detach this Receiver to this [context]
         */
        fun unRegister(context: Context) {
            context.unregisterReceiver(instant)
        }

        fun notifyWidget(context: Context, isPlaying: Boolean, what: String) {
            AppWidgetBig.instance.notifyChange(context, isPlaying, what)
            AppWidgetClassic.instance.notifyChange(context, isPlaying, what)
            AppWidgetSmall.instance.notifyChange(context, isPlaying, what)
            AppWidgetCard.instance.notifyChange(context, isPlaying, what)
        }
    }
}