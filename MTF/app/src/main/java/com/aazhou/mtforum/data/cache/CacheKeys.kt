package com.aazhou.mtforum.data.cache




object CacheKeys {

    
    const val LIST_TTL: Long = 30 * 60 * 1000L

    fun notice(): String = "notice"

    fun notice(category: String): String = "notice|$category"

    
    fun forum(fid: String, sort: String): String = "forum|$fid|$sort"

    fun home(): String = "home"

    
    fun guide(view: String): String = "guide|$view"

    fun search(keyword: String): String = "search|$keyword"

    
    fun searchForum(keyword: String): String = "search_forum|$keyword"

    fun searchUser(keyword: String): String = "search_user|$keyword"

    fun forumList(): String = "forumlist_v2"

    


    fun myThreads(uid: String, type: String): String = "my_threads|$uid|$type"

    fun myFavorites(uid: String, type: String): String = "my_favorites|$uid|$type"

    
    fun myFriends(uid: String, view: String): String = "my_friends|$uid|$view"

    fun friendRequests(uid: String): String = "friend_requests|$uid"

    fun followList(uid: String, type: String): String = "follow_list|$uid|$type"

    
    fun userProfile(uid: String): String = "user_profile|$uid"

    
    fun profile(uid: String): String = "profile|$uid"

    
    fun chat(touid: String): String = "chat|$touid"
}
