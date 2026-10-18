package com.aazhou.mtforum.data.repo

import android.content.Context






class ForumFollowStore(context: Context) {

    private val sp = context.getSharedPreferences("mtf_forum_follow", Context.MODE_PRIVATE)
    private val key = "followed_fids"

    private val cache: MutableSet<String> = mutableSetOf()

    init {
        cache.addAll(sp.getStringSet(key, emptySet()) ?: emptySet())
    }

    fun isFollowed(fid: String): Boolean = cache.contains(fid)

    fun getAll(): Set<String> = cache.toSet()

    
    fun toggle(fid: String): Boolean {
        val nowFollowed = if (cache.remove(fid)) false else { cache.add(fid); true }
        sp.edit().putStringSet(key, cache.toSet()).apply()
        return nowFollowed
    }
}
