package com.aazhou.mtforum.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.HomeData
import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


data class FeedState(
    val state: UiState<List<Thread>> = UiState.Loading,
    val page: Int = 1,
    val loadingMore: Boolean = false,
    val hasNext: Boolean = true,
    





    val resetScrollTick: Int = 0,
)

class HomeViewModel(
    private val repository: ForumRepository,
    private val contentCache: ContentCache,
) : ViewModel() {

    
    private val _hot = MutableStateFlow<HomeData?>(null)
    val hot: StateFlow<HomeData?> = _hot.asStateFlow()

    
    private val _feeds = MutableStateFlow<Map<String, FeedState>>(emptyMap())
    val feeds: StateFlow<Map<String, FeedState>> = _feeds.asStateFlow()

    
    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    val refreshing: StateFlow<Set<String>> = _refreshing.asStateFlow()

    
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    





    private val viewportAtTop = mutableMapOf<String, Boolean>()

    
    private val scrollPositions = mutableMapOf<String, Pair<Int, Int>>()

    
    fun noteScrollPosition(view: String, index: Int, offset: Int) {
        scrollPositions[view] = index to offset
    }

    
    fun getScrollPosition(view: String): Pair<Int, Int>? = scrollPositions[view]

    
    fun noteViewportAtTop(view: String, atTop: Boolean) {
        viewportAtTop[view] = atTop
    }

    private val cacheJson = Json { ignoreUnknownKeys = true }


    private val entered = mutableSetOf<String>()

    




    fun load(view: String) {
        entered += view
        if (_feeds.value.containsKey(view)) return
        _feeds.value = _feeds.value + (view to FeedState())
        viewModelScope.launch {
            
            if (view == HomeTabs.HOT) readHomeCache()?.let { _hot.value = it }
            
            val cached = contentCache.read(CacheKeys.guide(view), CacheKeys.LIST_TTL, allowExpired = true) {
                cacheJson.decodeFromString<CachedFeed>(it)
            }
            if (cached != null) {
                update(view) {
                    it.copy(state = UiState.Success(cached.threads), page = cached.page)
                }
            }
            reload(view)
        }
    }

    




    fun refresh(view: String) {
        if (view in _refreshing.value) return
        _refreshing.value = _refreshing.value + view
        viewModelScope.launch {
            reload(view, userInitiated = true)
            _refreshing.value = _refreshing.value - view
        }
    }

    


    fun retry(view: String) {
        viewModelScope.launch { reload(view) }
    }

    




    fun refreshOnEnter(view: String) {
        if (view !in entered) return
        
        
        
        val feed = _feeds.value[view]
        if (feed?.state is UiState.Success) return
        viewModelScope.launch { reload(view) }
    }

    private suspend fun reload(view: String, userInitiated: Boolean = false) {
        if (view == HomeTabs.HOT) {
            
            runCatching { repository.home() }.onSuccess {
                _hot.value = it
                saveHomeToCache(it)
            }
        }
        runCatching { repository.guideFeed(view, 1) }
            .onSuccess { data ->
                update(view) {
                    it.copy(
                        state = UiState.Success(data.threads.distinctBy { it.tid }), 
                        page = 1,
                        hasNext = data.hasNext,
                        
                        resetScrollTick = it.resetScrollTick +
                            if (userInitiated) 1 else 0,
                    )
                }
                saveFeedToCache(view)
            }
            .onFailure { e ->
                
                if (_feeds.value[view]?.state is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                } else {
                    update(view) { it.copy(state = UiState.Error(e.message ?: "加载失败")) }
                }
            }
    }

    
    fun loadMore(view: String) {
        val current = _feeds.value[view] ?: return
        if (current.loadingMore || !current.hasNext) return
        if (current.state !is UiState.Success) return
        update(view) { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val next = current.page + 1
            runCatching { repository.guideFeed(view, next) }
                .onSuccess { data ->
                    
                    val existing = (_feeds.value[view]?.state as? UiState.Success)?.data.orEmpty()
                    
                    val known = existing.mapTo(HashSet()) { it.tid }
                    val fresh = data.threads.filter { known.add(it.tid) }
                    update(view) {
                        it.copy(
                            state = UiState.Success((existing + fresh).distinctBy { it.tid }), 
                            page = next,
                            hasNext = data.hasNext && fresh.isNotEmpty(),
                        )
                    }
                    saveFeedToCache(view)
                }
                .onFailure { update(view) { it.copy(hasNext = false) } }
            update(view) { it.copy(loadingMore = false) }
        }
    }

    private fun update(view: String, transform: (FeedState) -> FeedState) {
        val current = _feeds.value[view] ?: FeedState()
        _feeds.value = _feeds.value + (view to transform(current))
    }

    
    private suspend fun saveFeedToCache(view: String) {
        val feed = _feeds.value[view] ?: return
        val threads = (feed.state as? UiState.Success)?.data ?: return
        contentCache.write(
            key = CacheKeys.guide(view),
            ttlMillis = CacheKeys.LIST_TTL,
            value = CachedFeed(page = feed.page, threads = threads),
            encode = { cacheJson.encodeToString(CachedFeed.serializer(), it) },
        )
    }

    





    private suspend fun readHomeCache(): HomeData? =
        contentCache.read(CacheKeys.home(), CacheKeys.LIST_TTL, allowExpired = true) {
            cacheJson.decodeFromString<HomeData>(it)
        }

    private suspend fun saveHomeToCache(data: HomeData) {
        contentCache.write(
            key = CacheKeys.home(),
            ttlMillis = CacheKeys.LIST_TTL,
            value = data,
            encode = { cacheJson.encodeToString(HomeData.serializer(), it) },
        )
    }
}


@Serializable
private data class CachedFeed(
    val page: Int = 1,
    val threads: List<Thread> = emptyList(),
)
