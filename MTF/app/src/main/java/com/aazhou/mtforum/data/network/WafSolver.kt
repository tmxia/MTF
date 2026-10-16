package com.aazhou.mtforum.data.network

import java.util.regex.Pattern











object WafSolver {

    const val COOKIE_NAME = "acw_sc__v2"

    private val ARG1 = Pattern.compile("var\\s+arg1\\s*=\\s*['\"]([0-9A-Fa-f]+)['\"]")

    
    private val PERMUTATION = intArrayOf(
        15, 35, 29, 24, 33, 16, 1, 38, 10, 9,
        19, 31, 40, 27, 22, 23, 25, 13, 6, 11,
        39, 18, 20, 8, 14, 21, 32, 26, 2, 30,
        7, 4, 17, 5, 3, 28, 34, 37, 12, 36,
    )

    private const val XOR_KEY_HEX = "3000176000856006061501533003690027800375"

    private const val HEX = "0123456789abcdef"

    
    fun looksLikeChallenge(body: String?): Boolean {
        if (body.isNullOrEmpty()) return false
        
        if (body.length > 64 * 1024) return false
        if (!body.contains(COOKIE_NAME)) return false
        return ARG1.matcher(body).find()
    }

    
    fun solve(body: String?): String? {
        if (body == null) return null
        val matcher = ARG1.matcher(body)
        if (!matcher.find()) return null
        val arg1 = matcher.group(1) ?: return null

        
        val reordered = StringBuilder()
        for (oneBased in PERMUTATION) {
            val idx = oneBased - 1
            if (idx in arg1.indices) reordered.append(arg1[idx])
        }
        val u = reordered.toString()

        val out = StringBuilder()
        val len = minOf(u.length, XOR_KEY_HEX.length)
        var i = 0
        while (i + 1 < len) {
            val a = hexVal(u[i]) * 16 + hexVal(u[i + 1])
            val b = hexVal(XOR_KEY_HEX[i]) * 16 + hexVal(XOR_KEY_HEX[i + 1])
            val v = a xor b
            out.append(HEX[v ushr 4]).append(HEX[v and 0xF])
            i += 2
        }
        return out.toString()
    }

    private fun hexVal(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> 0
    }
}
