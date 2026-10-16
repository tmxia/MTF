package com.aazhou.mtforum.ui.screen.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.NoticeGroup
import com.aazhou.mtforum.data.model.NoticeTab
import com.aazhou.mtforum.data.model.tabs
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.NoticeRepo
import com.aazhou.mtforum.data.repo.NoticeUnreadStore
import com.aazhou.mtforum.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


data class NoticeTabState(
    val state: UiState<NoticeContentState> = UiState.Loading,
    
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val hasNext: Boolean = false,
    





    val resetScrollTick: Int = 0,
)







class NoticeTabViewModel(
    private val group: NoticeGroup,
    private val repository: ForumRepository,
    private val noticeRepo: NoticeRepo,
    private val contentCache: ContentCache,
    private val unreadStore: NoticeUnreadStore,
) : ViewModel() {

    private val tabs: List<NoticeTab> = group.tabs()

    private val _tabStates = MutableStateFlow<Map<NoticeTab, NoticeTabState>>(emptyMap())
    val tabStates: StateFlow<Map<NoticeTab, NoticeTabState>> = _tabStates.asStateFlow()

    private val _currentTab = MutableStateFlow(tabs.firstOrNull())

    





    private val viewportAtTop = mutableMapOf<NoticeTab, Boolean>()

    
    fun noteViewportAtTop(tab: NoticeTab, atTop: Boolean) {
        viewportAtTop[tab] = atTop
    }

    private val cacheJson = Json { ignoreUnknownKeys = true }

    
    private val memoryCache = mutableMapOf<NoticeTab, NoticeContentState>()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        
        viewModelScope.launch {
            tabs.forEach { tab -> readDiskCache(tab)?.let { memoryCache[tab] = it } }
            if (memoryCache.isNotEmpty()) publishAll(memoryCache)
        }
    }

    
    fun load(tab: NoticeTab) {
        if (tab !in tabs) return
        loadJob?.cancel()
        loadMoreJob?.cancel()
        update(tab) { it.copy(loadingMore = false, hasNext = false) }
        loadJob = viewModelScope.launch {
            val cached = memoryCache[tab] ?: readDiskCache(tab)
            if (cached != null) publish(tab, cached)
            reload(tab)
        }
    }

    
    fun selectTab(tab: NoticeTab) {
        if (tab !in tabs) return
        _currentTab.value = tab
        val content = memoryCache[tab] ?: (_tabStates.value[tab]?.state as? UiState.Success)?.data
        val ids = content?.noticeIds().orEmpty()
        if (ids.isNotEmpty()) {
            viewModelScope.launch { unreadStore.markCategoryRead(tab, ids) }
        }
    }

    
    fun refresh(tab: NoticeTab) {
        if (_tabStates.value[tab]?.refreshing == true) return
        loadMoreJob?.cancel()
        update(tab) { it.copy(loadingMore = false) }
        viewModelScope.launch {
            update(tab) { it.copy(refreshing = true) }
            reload(tab)
            update(tab) { it.copy(refreshing = false) }
        }
    }

    private suspend fun reload(tab: NoticeTab) {
        try {
            val content = when (tab.pmFilter) {
                null -> {
                    val view = tab.view ?: return
                    val page = noticeRepo.notices(view, tab.type)
                    if (page.loggedOut) {
                        fail(tab, "请先登录后查看消息提醒")
                        return
                    }
                    NoticeContentState.Notices(
                        items = page.notices.sortedByDescending { recencyOf(it.timeText) },
                        page = 1,
                        hasNext = 1 < page.totalPages,
                    )
                }

                else -> {
                    val page = repository.pmMessages(tab.pmFilter)
                    if (page.loggedOut) {
                        fail(tab, "请先登录后查看消息")
                        return
                    }
                    NoticeContentState.PrivateMessages(page.messages)
                }
            }
            updateContent(tab, content)
            
            
        if (viewportAtTop[tab] == true) {
            update(tab) { it.copy(resetScrollTick = it.resetScrollTick + 1) }
        }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(tab, e.message ?: "加载失败")
        }
    }

    
    fun loadMore(tab: NoticeTab) {
        val current = _tabStates.value[tab]
        if (current == null || current.loadingMore || !current.hasNext) return
        val data = (current.state as? UiState.Success)?.data as? NoticeContentState.Notices ?: return
        update(tab) { it.copy(loadingMore = true) }
        val view = tab.view ?: return
        loadMoreJob = viewModelScope.launch {
            val next = data.page + 1
            runCatching { noticeRepo.notices(view, tab.type, next) }
                .onSuccess { page ->
                    if (page.loggedOut) {
                        update(tab) { it.copy(loadingMore = false, hasNext = false) }
                        return@onSuccess
                    }
                    
                    
                    val latest = (_tabStates.value[tab]?.state as? UiState.Success)?.data
                        as? NoticeContentState.Notices ?: data
                    val merged = (latest.items + page.notices).distinctBy { it.id }
                    
                    
                    val added = merged.size - latest.items.size
                    updateContent(
                        tab,
                        NoticeContentState.Notices(
                            items = merged.sortedByDescending { recencyOf(it.timeText) },
                            page = next,
                            hasNext = added > 0 && next < page.totalPages,
                        ),
                    )
                }
                .onFailure { update(tab) { it.copy(loadingMore = false, hasNext = false) } }
            update(tab) { it.copy(loadingMore = false) }
        }
    }

    private suspend fun updateContent(tab: NoticeTab, content: NoticeContentState) {
        memoryCache[tab] = content
        publish(tab, content)
        writeDiskCache(tab, content)
        
        val ids = content.noticeIds()
        unreadStore.onCategoryPolled(tab, ids)
        if (_currentTab.value == tab) markRead(tab, ids)
    }

    private suspend fun markRead(tab: NoticeTab, ids: List<String>) {
        if (ids.isEmpty()) return
        unreadStore.markCategoryRead(tab, ids)
    }

    private fun publish(tab: NoticeTab, content: NoticeContentState) {
        val hasNext = (content as? NoticeContentState.Notices)?.hasNext ?: false
        update(tab) { it.copy(state = UiState.Success(content), hasNext = hasNext) }
    }

    private fun publishAll(contents: Map<NoticeTab, NoticeContentState>) {
        contents.forEach { (tab, content) -> publish(tab, content) }
    }

    private fun fail(tab: NoticeTab, message: String) {
        val existing = _tabStates.value[tab]?.state
        if (existing is UiState.Success) return
        update(tab) { it.copy(state = UiState.Error(message)) }
    }

    private fun update(tab: NoticeTab, transform: (NoticeTabState) -> NoticeTabState) {
        val current = _tabStates.value[tab] ?: NoticeTabState()
        _tabStates.value = _tabStates.value + (tab to transform(current))
    }

    private suspend fun readDiskCache(tab: NoticeTab): NoticeContentState? {
        val filter = tab.pmFilter
        return if (filter != null) {
            contentCache.read(CacheKeys.notice(tab.key), CacheKeys.LIST_TTL, allowExpired = true) {
                NoticeContentState.PrivateMessages(cacheJson.decodeFromString(it))
            }
        } else {
            contentCache.read(CacheKeys.notice(tab.key), CacheKeys.LIST_TTL, allowExpired = true) {
                val cached = cacheJson.decodeFromString<CachedNotices>(it)
                NoticeContentState.Notices(cached.notices, cached.page, cached.hasNext)
            }
        }
    }

    private suspend fun writeDiskCache(tab: NoticeTab, content: NoticeContentState) {
        when (content) {
            is NoticeContentState.Notices -> contentCache.write(
                key = CacheKeys.notice(tab.key),
                ttlMillis = CacheKeys.LIST_TTL,
                value = CachedNotices(content.items, content.page, content.hasNext),
                encode = { cacheJson.encodeToString(CachedNotices.serializer(), it) },
            )

            is NoticeContentState.PrivateMessages -> contentCache.write(
                key = CacheKeys.notice(tab.key),
                ttlMillis = CacheKeys.LIST_TTL,
                value = content.items,
                encode = { cacheJson.encodeToString(it) },
            )
        }
    }
}
