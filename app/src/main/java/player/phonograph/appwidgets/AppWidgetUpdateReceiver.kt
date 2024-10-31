/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.appwidgets

import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.registerReceiverCompat
import androidx.core.content.ContextCompat
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class AppWidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val widget = intent.getStringExtra(EXTRA_APPWIDGET_NAME)
        val ids = intent.getIntArrayExtra(EXTRA_APPWIDGET_IDS)
        val isPlaying = intent.getBooleanExtra(EXTRA_APPWIDGET_IS_PLAYING, MusicPlayerRemote.isPlaying)
        when (widget) {
            AppWidgetClassic.NAME -> AppWidgetClassic.instance.update(context, ids, isPlaying)
            AppWidgetSmall.NAME   -> AppWidgetSmall.instance.update(context, ids, isPlaying)
            AppWidgetBig.NAME     -> AppWidgetBig.instance.update(context, ids, isPlaying)
            AppWidgetCard.NAME    -> AppWidgetCard.instance.update(context, ids, isPlaying)
        }
    }


    companion object {
        const val ACTION_APPWIDGET_UPDATE = "$ACTUAL_PACKAGE_NAME.app_widget_update"

        const val EXTRA_APPWIDGET_NAME = "$ACTUAL_PACKAGE_NAME.app_widget_name"
        const val EXTRA_APPWIDGET_IS_PLAYING = "$ACTUAL_PACKAGE_NAME.app_widget_is_playing"


        private val ALL_WIDGETS = mapOf(
            AppWidgetClassic.NAME to AppWidgetClassic::class.java,
            AppWidgetSmall.NAME to AppWidgetSmall::class.java,
            AppWidgetBig.NAME to AppWidgetBig::class.java,
            AppWidgetCard.NAME to AppWidgetCard::class.java,
        )

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
                IntentFilter(ACTION_APPWIDGET_UPDATE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        /**
         * unregister & detach this Receiver to this [context]
         */
        fun unRegister(context: Context) {
            context.unregisterReceiver(instant)
        }

        /**
         * refresh App Widgets
         */
        fun notifyWidgets(context: Context, isPlaying: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            for ((name, clazz) in ALL_WIDGETS) {
                val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
                if (ids.isNotEmpty()) { // update if existed
                    context.sendBroadcast(
                        Intent(ACTION_APPWIDGET_UPDATE).apply {
                            `package` = ACTUAL_PACKAGE_NAME
                            putExtra(EXTRA_APPWIDGET_NAME, name)
                            putExtra(EXTRA_APPWIDGET_IDS, ids)
                            putExtra(EXTRA_APPWIDGET_IS_PLAYING, isPlaying)
                            addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
                        }
                    )
                }
            }
        }

    }
}