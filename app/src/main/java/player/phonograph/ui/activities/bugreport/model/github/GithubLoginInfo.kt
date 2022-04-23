package player.phonograph.ui.activities.bugreport.model.github

data class GithubLoginInfo(
    val username: String?,
    val password: String?,
    val apiToken: String? = null,
) {
    fun shouldUseApiToken(): Boolean {
        return username.isNullOrBlank() || username.isNullOrBlank()
    }
}
