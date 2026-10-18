package com.aazhou.mtforum.ui.screen.threadlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.ForumPage
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.ForumSort
import com.aazhou.mtforum.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


data class ForumTabState(
    val state: UiState<ForumPage> = UiState.Loading,
    val page: Int = 1,
    val loadingMore: Boolean = false,
    val hasNext: Boolean = true,
    
    val refreshing: Boolean = false,
    





    val resetScrollTick: Int = 0,
)

class ThreadListViewModel(
    private val fid: String,
    private val repository: ForumRepository,
    private val contentCache: ContentCache,
) : ViewModel() {

    
    private val _tabStates = MutableStateFlow<Map<ForumSort, ForumTabState>>(emptyMap())
    val tabStates: StateFlow<Map<ForumSort, ForumTabState>> = _tabStates.asStateFlow()

    
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    
    private val loadMoreJobs = mutableMapOf<ForumSort, Job>()

    
    private val entered = mutableSetOf<ForumSort>()

    





    private val viewportAtTop = mutableMapOf<ForumSort, Boolean>()

    
    fun noteViewportAtTop(sort: ForumSort, atTop: Boolean) {
        viewportAtTop[sort] = atTop
    }

    private val cacheJson = Json { ignoreUnknownKeys = true }

    




    fun load(sort: ForumSort) {
        entered += sort
        if (_tabStates.value.containsKey(sort)) return
        update(sort) { ForumTabState() }
        viewModelScope.launch {
            
            val cached = contentCache.read(
                CacheKeys.forum(fid, sort.name),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString<ForumPage>(it)
            }
            if (cached != null) {
                update(sort) {
                    it.copy(
                        state = UiState.Success(cached),
                        page = cached.page,
                        hasNext = cached.hasNext,
                    )
                }
            }
            reload(sort)
        }
    }

    
    fun retry(sort: ForumSort) {
        viewModelScope.launch { reload(sort) }
    }






    fun refreshOnEnter(sort: ForumSort) {
        if (sort !in entered) return
        
        
        
        if (_tabStates.value[sort]?.state is UiState.Success) return
        viewModelScope.launch { reload(sort) }
    }

    
    fun refresh(sort: ForumSort) {
        if (_tabStates.value[sort]?.refreshing == true) return
        loadMoreJobs[sort]?.cancel()
        update(sort) { it.copy(loadingMore = false) }
        viewModelScope.launch {
            update(sort) { it.copy(refreshing = true) }
            reload(sort)
            update(sort) { it.copy(refreshing = false) }
        }
    }

    private suspend fun reload(sort: ForumSort) {
        runCatching { repository.forumPage(fid, 1, sort) }
            .onSuccess { data ->
                update(sort) {
                    it.copy(
                        state = UiState.Success(data),
                        page = 1,
                        hasNext = data.hasNext,
                        
                        resetScrollTick = it.resetScrollTick +
                            if (viewportAtTop[sort] == true) 1 else 0,
                    )
                }
                saveToCache(sort)
            }
            .onFailure { e ->
                
                if (_tabStates.value[sort]?.state is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                } else {
                    update(sort) { it.copy(state = UiState.Error(e.message ?: "加载失败")) }
                }
            }
    }

    
    fun loadMore(sort: ForumSort) {
        val current = _tabStates.value[sort] ?: return
        if (current.loadingMore || !current.hasNext) return
        if (current.state !is UiState.Success) return
        update(sort) { it.copy(loadingMore = true) }
        loadMoreJobs[sort] = viewModelScope.launch {
            val next = current.page + 1
            runCatching { repository.forumPage(fid, next, sort) }
                .onSuccess { pageData ->
                    val existing = (_tabStates.value[sort]?.state as? UiState.Success)?.data
                    update(sort) {
                        it.copy(
                            state = UiState.Success(
                                existing?.copy(
                                    threads = existing.threads + pageData.threads,
                                    hasNext = pageData.hasNext,
                                ) ?: pageData,
                            ),
                            page = next,
                            hasNext = pageData.hasNext,
                        )
                    }
                    saveToCache(sort)
                }
                .onFailure { update(sort) { it.copy(hasNext = false) } }
            update(sort) { it.copy(loadingMore = false) }
        }
    }

    private fun update(sort: ForumSort, transform: (ForumTabState) -> ForumTabState) {
        val current = _tabStates.value[sort] ?: ForumTabState()
        _tabStates.value = _tabStates.value + (sort to transform(current))
    }

    
    private suspend fun saveToCache(sort: ForumSort) {
        val data = (_tabStates.value[sort]?.state as? UiState.Success)?.data ?: return
        contentCache.write(
            key = CacheKeys.forum(fid, sort.name),
            ttlMillis = CacheKeys.LIST_TTL,
            value = data,
            encode = { cacheJson.encodeToString(ForumPage.serializer(), it) },
        )
    }
}
