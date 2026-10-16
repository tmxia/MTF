package com.aazhou.mtforum.ui.common

import java.util.concurrent.ConcurrentHashMap

/**
 * 全局滚动位置存储。
 *
 * 用于跨页面导航（切 tab / 进详情返回）时保留列表滚动位置。
 * 相比 rememberSaveable，全局单例在页面被销毁重建时不会丢；
 * 相比 ViewModel，即使 ViewModel 被销毁也能保留。
 *
 * key 建议格式："<页面>:<子 tab 或 view>"
 */
object ScrollStore {
    private val positions = ConcurrentHashMap<String, Pair<Int, Int>>()

    fun note(key: String, index: Int, offset: Int) {
        positions[key] = index to offset
    }

    fun get(key: String): Pair<Int, Int>? = positions[key]

    fun clear(key: String) {
        positions.remove(key)
    }
}
