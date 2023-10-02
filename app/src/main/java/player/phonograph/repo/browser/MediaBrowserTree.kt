/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

object MediaBrowserTree {

    class TreeItem(val node: INode?, val parameters: Map<String, String>?)

    interface INode {
        val parent: INode
        val name: String
        val path: String
    }

    data class Node(
        override val parent: INode,
        override val name: String,
    ) : INode {
        override val path: String get() = parent.path + "/" + this.name
    }

    data object Root : INode {
        override val parent: INode get() = Root
        override val name: String get() = "/"
        override val path: String get() = "/"
    }

    fun resolve(raw: String): TreeItem? {
        if (!raw.startsWith('/')) return null
        val parameters = raw.substringAfterLast('?', "")
        val path = raw.substringBefore('?')

        val node = resolvePathInternal(path)
        val parametersMap = resolveParametersInternal(parameters)

        return TreeItem(node, parametersMap)
    }

    private fun resolvePathInternal(path: String): INode? {
        if (path == Root.path) return Root
        val segments = path.pathSplit('/')
        return if (!segments.isNullOrEmpty()) {
            val head = Node(Root, segments.first())
            if (segments.size > 1) {
                val tails = segments.tail()
                var current: INode = head
                for (seg in tails.reversed()) {
                    current = Node(current, seg)
                }
                current
            } else {
                head
            }
        } else {
            null
        }
    }


    private fun resolveParametersInternal(parameters: String): Map<String, String>? {
        return if (parameters.isNotEmpty()) {
            val list = parameters.split('&')
            list.mapNotNull { pair ->
                val kv = pair.split('=', limit = 2)
                if (kv.size == 2) {
                    kv[0] to kv[1]
                } else {
                    null
                }
            }.toMap()
        } else {
            null
        }

    }

    private fun <T> List<T>.tail() = subList(1, size)

    private fun CharSequence.pathSplit(delimiter: Char): List<String>? {
        if (isEmpty() || (length == 1 && this[0] == delimiter)) return null
        var current = 1 // ignore root
        var delimiterIndex = indexOf(delimiter, startIndex = current)
        if (delimiterIndex == -1) return listOf(substring(current, length))
        val result = mutableListOf<String>()
        do {
            result.add(substring(current, delimiterIndex))
            current = delimiterIndex + 1
            delimiterIndex = indexOf(delimiter, startIndex = current)
        } while (delimiterIndex != -1)
        if (current != length) result.add(substring(current, length))
        return result
    }

}