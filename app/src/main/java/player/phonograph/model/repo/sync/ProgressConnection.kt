/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.sync

interface ProgressConnection {

    fun onStart()

    fun onStart(notificationId: Int)

    fun onProcessUpdate(message: String?)

    fun onProcessUpdate(current: Int, total: Int)

    fun onProcessUpdate(current: Int, total: Int, message: String?)

    fun onCompleted()

    fun onReset()
}