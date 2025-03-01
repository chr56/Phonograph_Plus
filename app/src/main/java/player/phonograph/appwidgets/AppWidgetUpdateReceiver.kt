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
        when (intent.action) {
            ACTION_APPWIDGET_UPDATE -> updateWidgets(intent, context)
            ACTION_APPWIDGET_CONNECT -> connectWidgets(intent, context)
        }
    }

    private fun connectWidgets(intent: Intent, context: Context) {
        val widget = findWidget(intent.getStringExtra(EXTRA_APPWIDGET_NAME)) ?: return
        widget.connected = true
        updateWidgets(intent, context)
    }

    private fun updateWidgets(intent: Intent, context: Context) {
        val widget = findWidget(intent.getStringExtra(EXTRA_APPWIDGET_NAME)) ?: return
        val ids = intent.getIntArrayExtra(EXTRA_APPWIDGET_IDS)
        val isPlaying = intent.getBooleanExtra(EXTRA_APPWIDGET_IS_PLAYING, MusicPlayerRemote.isPlaying)
        widget.update(context, ids, isPlaying)
    }


    companion object {
        const val ACTION_APPWIDGET_UPDATE = "$ACTUAL_PACKAGE_NAME.update_widgets"
        const val ACTION_APPWIDGET_CONNECT = "$ACTUAL_PACKAGE_NAME.connect_widgets"

        const val EXTRA_APPWIDGET_NAME = "$ACTUAL_PACKAGE_NAME.app_widget_name"
        const val EXTRA_APPWIDGET_IS_PLAYING = "$ACTUAL_PACKAGE_NAME.app_widget_is_playing"


        private val ALL_WIDGETS = mapOf(
            AppWidgetClassic.NAME to AppWidgetClassic::class.java,
            AppWidgetSmall.NAME to AppWidgetSmall::class.java,
            AppWidgetBig.NAME to AppWidgetBig::class.java,
            AppWidgetCard.NAME to AppWidgetCard::class.java,
        )

        private fun findWidget(name: String?): BaseAppWidget? =
            when (name) {
                AppWidgetClassic.NAME -> AppWidgetClassic.instance
                AppWidgetSmall.NAME   -> AppWidgetSmall.instance
                AppWidgetBig.NAME     -> AppWidgetBig.instance
                AppWidgetCard.NAME    -> AppWidgetCard.instance
                else                  -> null
            }

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
                IntentFilter().apply {
                    addAction(ACTION_APPWIDGET_UPDATE)
                    addAction(ACTION_APPWIDGET_CONNECT)
                },
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
         * run [block] if certain widgets existed
         * @param block lambda of (AppWidgetIds, AppWidgetName) -> Unit
         */
        private inline fun selectExistedWidgets(context: Context, block: (IntArray, String) -> Unit) {
            val manager = AppWidgetManager.getInstance(context)
            for ((name, clazz) in ALL_WIDGETS) {
                val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
                if (ids.isNotEmpty()) {
                    block(ids, name)
                }
            }
        }

        /**
         * refresh App Widgets
         */
        fun notifyWidgets(context: Context, isPlaying: Boolean) {
            selectExistedWidgets(context) { ids, name ->
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

        /**
         * link widgets
         */
        fun connect(context: Context) {
            selectExistedWidgets(context) { ids, name ->
                context.sendBroadcast(
                    Intent(ACTION_APPWIDGET_CONNECT).apply {
                        `package` = ACTUAL_PACKAGE_NAME
                        putExtra(EXTRA_APPWIDGET_NAME, name)
                        putExtra(EXTRA_APPWIDGET_IDS, ids)
                        addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
                    }
                )
            }
        }

    }
}