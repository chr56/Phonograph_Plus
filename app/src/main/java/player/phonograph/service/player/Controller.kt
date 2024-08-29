/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service.player

interface Controller {

    /**
     * Play
     */
    fun play()

    /**
     * Pause
     * @param releaseResource false if not release taken resource
     * @param reason cause of this pause (see [PauseReason])
     */
    fun pause(releaseResource: Boolean, @PauseReason reason: Int)


    /**
     * Stop
     */
    fun stop()


    /**
     * Play or Pause
     */
    fun togglePlayPause()


    /**
     * True if it is playing
     */
    val isPlaying: Boolean


    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    val songProgressMillis: Int

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    val songDurationMillis: Int

    /**
     * Move current time to [position]
     * @param position time in millisecond
     */
    fun seekTo(position: Long): Int


    /**
     * Jump to beginning of this song
     */
    fun rewindToBeginning()

    /**
     * Return to previous song
     */
    fun jumpBackward(force: Boolean)

    /**
     * [rewindToBeginning] or [jumpBackward]
     */
    fun back(force: Boolean)

    /**
     * Skip and jump to next song
     */
    fun jumpForward(force: Boolean)



    var playerSpeed: Float


    fun setVolume(vol: Float)

    val audioSessionId: Int
}