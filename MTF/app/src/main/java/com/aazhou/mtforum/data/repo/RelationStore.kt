package com.aazhou.mtforum.data.repo

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock















class RelationStore(
    context: Context,
    private val repository: ForumRepository,
) {
    private val sp = context.applicationContext
        .getSharedPreferences("mtf_relations", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    @Volatile private var lastRefreshAt = 0L
    @Volatile private var lastBlockedRefreshAt = 0L

    private val _following = MutableStateFlow<Set<String>>(emptySet())
    val following: StateFlow<Set<String>> = _following.asStateFlow()

    private val _followers = MutableStateFlow<Set<String>>(emptySet())
    val followers: StateFlow<Set<String>> = _followers.asStateFlow()

    private val _friends = MutableStateFlow<Set<String>>(emptySet())
    val friends: StateFlow<Set<String>> = _friends.asStateFlow()

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    init {
        
        _following.value = load("following")
        _followers.value = load("followers")
        _friends.value = load("friends")
        _blocked.value = load("blocked")
    }

    
    
    fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshAt < FULL_REFRESH_INTERVAL_MS) return
        scope.launch {
            refreshMutex.withLock {
                val now2 = System.currentTimeMillis()
                if (!force && now2 - lastRefreshAt < FULL_REFRESH_INTERVAL_MS) return@withLock
                try {
                    pullBlocked()
                    pullFriends()
                    pullFollowing()
                    pullFollowers()
                    lastRefreshAt = System.currentTimeMillis()
                    lastBlockedRefreshAt = lastRefreshAt
                } catch (_: Exception) {
                    
                }
            }
        }
    }

    
    fun refreshBlocked(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastBlockedRefreshAt < SHORT_REFRESH_INTERVAL_MS) return
        scope.launch {
            refreshMutex.withLock {
                try {
                    pullBlocked()
                    lastBlockedRefreshAt = System.currentTimeMillis()
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun pullBlocked() {
        val all = mutableSetOf<String>()
        var page = 1
        while (page <= MAX_PAGES) {
            val data = repository.myFriends(view = "blacklist", page = page)
            all += data.users.mapNotNull { it.uid.takeIf { u -> u.isNotBlank() } }
            if (!data.hasNext) break
            page++
        }
        _blocked.value = all
        save("blocked", all)
    }

    private suspend fun pullFriends() {
        val all = mutableSetOf<String>()
        var page = 1
        while (page <= MAX_PAGES) {
            val data = repository.myFriends(view = "", page = page)
            all += data.users.mapNotNull { it.uid.takeIf { u -> u.isNotBlank() } }
            if (!data.hasNext) break
            page++
        }
        _friends.value = all
        save("friends", all)
    }

    private suspend fun pullFollowing() {
        val all = mutableSetOf<String>()
        var page = 1
        while (page <= MAX_PAGES) {
            val data = repository.followList(type = "following", page = page)
            all += data.users.mapNotNull { it.uid.takeIf { u -> u.isNotBlank() } }
            if (!data.hasNext) break
            page++
        }
        _following.value = all
        save("following", all)
    }

    private suspend fun pullFollowers() {
        val all = mutableSetOf<String>()
        var page = 1
        while (page <= MAX_PAGES) {
            val data = repository.followList(type = "follower", page = page)
            all += data.users.mapNotNull { it.uid.takeIf { u -> u.isNotBlank() } }
            if (!data.hasNext) break
            page++
        }
        _followers.value = all
        save("followers", all)
    }

    
    fun addBlocked(uid: String) {
        if (uid.isBlank()) return
        val next = _blocked.value + uid
        _blocked.value = next
        save("blocked", next)
    }

    fun removeBlocked(uid: String) {
        if (uid.isBlank()) return
        val next = _blocked.value - uid
        _blocked.value = next
        save("blocked", next)
    }

    fun addAllBlocked(uids: Collection<String>) {
        val valid = uids.filter { it.isNotBlank() }.toSet()
        if (valid.isEmpty()) return
        val next = _blocked.value + valid
        _blocked.value = next
        save("blocked", next)
    }

    
    fun setFollowing(uid: String, following: Boolean) {
        if (uid.isBlank()) return
        val next = if (following) _following.value + uid else _following.value - uid
        _following.value = next
        save("following", next)
    }

    fun addAllFollowing(uids: Collection<String>) {
        val valid = uids.filter { it.isNotBlank() }.toSet()
        if (valid.isEmpty()) return
        val next = _following.value + valid
        _following.value = next
        save("following", next)
    }

    
    fun addAllFriends(uids: Collection<String>) {
        val valid = uids.filter { it.isNotBlank() }.toSet()
        if (valid.isEmpty()) return
        val next = _friends.value + valid
        _friends.value = next
        save("friends", next)
    }

    fun addAllFollowers(uids: Collection<String>) {
        val valid = uids.filter { it.isNotBlank() }.toSet()
        if (valid.isEmpty()) return
        val next = _followers.value + valid
        _followers.value = next
        save("followers", next)
    }

    
    private fun load(key: String): Set<String> {
        val raw = sp.getStringSet(key, null) ?: return emptySet()
        return raw.toSet()  
    }

    private fun save(key: String, value: Set<String>) {
        sp.edit().putStringSet(key, value.toSet()).apply()
    }

    companion object {
        private const val MAX_PAGES = 1000
        private const val FULL_REFRESH_INTERVAL_MS = 5 * 60 * 1000L  
        private const val SHORT_REFRESH_INTERVAL_MS = 60 * 1000L     
    }
}
