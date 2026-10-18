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
        
        
        
        val views = tabs.mapNotNull { it.view }.distinct()
        val pmTabs = tabs.filter { it.pmFilter != null }

        val (viewResults, pmPages) = coroutineScope {
            val viewDeferred = views.map { view ->
                async { runCatching { repository.pollNoticeView(view) }.getOrNull() }
            }
            val pmDeferred = pmTabs.map { tab ->
                async { runCatching { repository.pmMessages(tab.pmFilter!!) }.getOrNull() }
            }
            viewDeferred.awaitAll() to pmDeferred.awaitAll()
        }

        val mergedCounts = mutableMapOf<String, Int>()
        val mergedNotices = mutableListOf<Notice>()
        for (r in viewResults) {
            if (r == null) continue
            mergedCounts.putAll(r.sidebarCounts)
            mergedNotices += r.notices
        }

        val freshIds: Set<String> = if (mergedNotices.isEmpty()) emptySet()
            else unreadStore.onPolled(mergedNotices.map { it.id }).toSet()

        if (mergedCounts.isNotEmpty()) {
            unreadStore.setSidebarCounts(mergedCounts)
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

        if (mergedNotices.isEmpty()) return emptyList()
        return mergedNotices.filter { it.id in freshIds }
    }
}
