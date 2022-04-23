package player.phonograph.ui.activities.bugreport.model.github

class ExtraInfo {

    private val extraInfo: MutableMap<String, String> = LinkedHashMap()

    fun put(key: String, value: String) {
        extraInfo[key] = value
    }

    fun put(key: String, value: Boolean) {
        extraInfo[key] = value.toString()
    }

    fun put(key: String, value: Double) {
        extraInfo[key] = value.toString()
    }

    fun put(key: String, value: Float) {
        extraInfo[key] = value.toString()
    }

    fun put(key: String, value: Long) {
        extraInfo[key] = value.toString()
    }

    fun put(key: String, value: Int) {
        extraInfo[key] = value.toString()
    }

    fun put(key: String, value: Any) {
        extraInfo[key] = value.toString()
    }

    fun remove(key: String) {
        extraInfo.remove(key)
    }

    fun toMarkdown(): String {

        if (extraInfo.isEmpty()) return ""

        return """
            Extra info:
            ---
            <table>
            ${ extraInfo.keys.reduce { acc, key ->
            "$acc<tr><td>$key</td><td>${extraInfo[key]}</td></tr>\n"
        } }
            </table>
        """.trimIndent()
    }
}
