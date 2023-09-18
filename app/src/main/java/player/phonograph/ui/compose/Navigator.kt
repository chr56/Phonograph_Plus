/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class Navigator<P : Navigator.IPage>(rootRage: P) {

    interface IPage

    private val _pages: MutableList<P> = mutableListOf(rootRage)
    val pages get() = _pages.toList()

    private val _currentPage: MutableStateFlow<P> = MutableStateFlow(rootRage)
    val currentPage get() = _currentPage.asStateFlow()

    fun navigateTo(page: P) {
        _pages.add(page)
        _currentPage.value = page
    }

    /**
     * @return false if reaching to root
     */
    fun navigateUp(level: Int = 1): Boolean {
        if (level < 1) return true
        if (level >= _pages.size) return false
        repeat(level) { _pages.removeLastOrNull() }
        val last = _pages.lastOrNull()
        return if (last != null) {
            _currentPage.value = last
            true
        } else {
            false
        }
    }

    fun isRoot(page: P) = page == _pages.firstOrNull()

}