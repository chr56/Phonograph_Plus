/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package player.phonograph.util

import lib.phonograph.misc.emit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkUtil {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4500, TimeUnit.MILLISECONDS)
        .build()

    suspend fun invokeRequest(request: Request): Response {
        return withContext(Dispatchers.IO) {
            val call = okHttpClient.newCall(request)
            return@withContext try {
                call.emit()
            } catch (e: IOException) {
                throw e
            }
        }
    }
}