package player.phonograph.model.ui

fun interface ProgressUpdateCallback {
    /**
     * Called when progress updated
     * @param progress current progress
     * @param total total progress
     */
    fun onUpdateProgress(progress: Int, total: Int)
}