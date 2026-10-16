package com.aazhou.mtforum.ui.screen.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.model.NoticeTab
import com.aazhou.mtforum.data.model.PREFETCH_TABS
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.NoticePollStore
import com.aazhou.mtforum.data.repo.NoticeRepo
import com.aazhou.mtforum.data.repo.NoticeUnreadStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch







class NoticeViewModel(
    private val repository: ForumRepository,
    private val noticeRepo: NoticeRepo,
    private val unreadStore: NoticeUnreadStore,
    private val pollStore: NoticePollStore,
    
    private val onRead: (List<String>) -> Unit,
) : ViewModel() {

    
    private val _unreadTabs = MutableStateFlow<Set<NoticeTab>>(emptySet())
    val unreadTabs: StateFlow<Set<NoticeTab>> = _unreadTabs.asStateFlow()

    private val _pollEnabled = MutableStateFlow(false)
    val pollEnabled: StateFlow<Boolean> = _pollEnabled.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            unreadStore.unreadTabs.collect { tabs -> _unreadTabs.value = tabs }
        }
        viewModelScope.launch {
            pollStore.enabled.collect { enabled -> _pollEnabled.value = enabled }
        }
        prefetch()
    }

    
    fun prefetch() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val loaded = coroutineScope {
                PREFETCH_TABS.map { tab -> async { runCatching { firstPageOf(tab) }.getOrNull() } }
                    .awaitAll()
            }
            val readIds = mutableListOf<String>()
            for ((tab, content) in PREFETCH_TABS.zip(loaded)) {
                if (content == null) continue
                val ids = content.noticeIds()
                
                unreadStore.onCategoryPolled(tab, ids)
                
                if (tab.view != null) readIds += ids
            }
            if (readIds.isNotEmpty()) onRead(readIds.distinct())
        }
    }

    
    private suspend fun firstPageOf(tab: NoticeTab): NoticeContentState? {
        val filter = tab.pmFilter
        if (filter != null) {
            return repository.pmMessages(filter).takeIf { !it.loggedOut }
                ?.let { NoticeContentState.PrivateMessages(it.messages) }
        }
        val view = tab.view ?: return null
        return noticeRepo.notices(view, tab.type).takeIf { !it.loggedOut }?.let { page ->
            NoticeContentState.Notices(
                items = page.notices.sortedByDescending { recencyOf(it.timeText) },
                page = 1,
                hasNext = 1 < page.totalPages,
            )
        }
    }

    
    fun setPollEnabled(enabled: Boolean) {
        viewModelScope.launch { pollStore.setEnabled(enabled) }
    }
}
