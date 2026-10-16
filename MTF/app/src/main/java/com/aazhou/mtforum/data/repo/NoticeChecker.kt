package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.Notice
import com.aazhou.mtforum.data.model.NoticeTab
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope















class NoticeChecker(
    private val repository: ForumRepository,
    private val unreadStore: NoticeUnreadStore,
) {
    suspend fun check(tabs: List<NoticeTab>): List<Notice> {
        val pmTabs = tabs.filter { it.pmFilter != null }
        val noticeTabs = tabs.filter { it.view != null }
        val (notices, pmPages) = coroutineScope {
            val noticesDeferred = async { repository.notices(noticeTabs).notices }
            val pmDeferred = pmTabs.map { tab ->
                async { runCatching { repository.pmMessages(tab.pmFilter!!) }.getOrNull() }
            }
            noticesDeferred.await() to pmDeferred.awaitAll()
        }
        var pmUnread = false
        var pmHasResult = false
        for ((tab, page) in pmTabs.zip(pmPages)) {
            if (page == null || page.loggedOut) continue
            pmHasResult = true
            val ids = page.messages.map { it.plid ?: it.touid ?: it.username }
            unreadStore.onCategoryPolled(tab, ids)
            
            
            pmUnread = pmUnread || page.messages.any { it.unread }
        }
        if (pmHasResult) unreadStore.setPmUnread(pmUnread)
        if (notices.isEmpty()) return emptyList()
        val freshIds = unreadStore.onPolled(notices.map { it.id }).toSet()
        return notices.filter { it.id in freshIds }
    }
}
