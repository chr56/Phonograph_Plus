/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph

import android.app.Application
import android.content.Context

abstract class BaseApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    companion object {
        @JvmStatic
        lateinit var instance: Application
            private set
    }
}