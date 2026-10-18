package com.aazhou.mtforum.ui.common

import java.util.concurrent.ConcurrentHashMap










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
