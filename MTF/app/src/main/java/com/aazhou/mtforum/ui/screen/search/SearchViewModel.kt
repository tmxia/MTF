package com.aazhou.mtforum.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.AtUser
import com.aazhou.mtforum.data.model.Forum
import com.aazhou.mtforum.data.model.ForumGroup
import com.aazhou.mtforum.data.model.SearchResult
import com.aazhou.mtforum.data.model.SearchTab
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SearchViewModel(
    private val repository: ForumRepository,
    private val contentCache: ContentCache,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SearchResult>?>(null)
    val state: StateFlow<UiState<SearchResult>?> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _selectedTab = MutableStateFlow(SearchTab.Thread)
    val selectedTab: StateFlow<SearchTab> = _selectedTab.asStateFlow()

    
    private val _selectedFid = MutableStateFlow<String?>(null)
    val selectedFid: StateFlow<String?> = _selectedFid.asStateFlow()

    private val _selectedFidName = MutableStateFlow("全部板块")
    val selectedFidName: StateFlow<String> = _selectedFidName.asStateFlow()

    private val _forumGroups = MutableStateFlow<List<ForumGroup>>(emptyList())
    val forumGroups: StateFlow<List<ForumGroup>> = _forumGroups.asStateFlow()

    private var forumGroupsLoaded = false

    fun loadForumGroups() {
        if (forumGroupsLoaded) return
        viewModelScope.launch {
            runCatching { repository.forumGroups() }
                .onSuccess {
                    _forumGroups.value = it
                    forumGroupsLoaded = true
                }
        }
    }

    fun selectFid(fid: String?, name: String) {
        if (_selectedFid.value == fid) return
        _selectedFid.value = fid
        _selectedFidName.value = name
        
        if (keyword.isNotBlank()) submit(keyword)
    }

    private val _forumState = MutableStateFlow<UiState<List<Forum>>?>(null)
    val forumState: StateFlow<UiState<List<Forum>>?> = _forumState.asStateFlow()

    private val _userState = MutableStateFlow<UiState<List<AtUser>>?>(null)
    val userState: StateFlow<UiState<List<AtUser>>?> = _userState.asStateFlow()

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    var keyword: String = ""
        private set

    
    private var nextUrl: String? = null

    
    private var entered = false

    
    private val _resetScrollTick = MutableStateFlow(0)
    val resetScrollTick: StateFlow<Int> = _resetScrollTick.asStateFlow()

    private val cacheJson = Json { ignoreUnknownKeys = true }

    
    fun selectTab(tab: SearchTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        if (keyword.isNotBlank()) submit(keyword)
    }

    
    fun submit(keyword: String) {
        entered = true
        when (_selectedTab.value) {
            SearchTab.Thread -> search(keyword)
            SearchTab.Forum -> searchForums(keyword)
            SearchTab.User -> searchUsers(keyword)
        }
    }

    




        fun clearSearchState() {
        keyword = ""
        nextUrl = null
        _state.value = null
        _forumState.value = null
        _userState.value = null
        _loadingMore.value = false
        _selectedTab.value = SearchTab.Thread
    }

    fun refreshOnEnter() {
        if (!entered || keyword.isBlank()) return
        
        if (_state.value is UiState.Success) return
        submit(keyword)
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) return
        val trimmed = keyword.trim()
        this.keyword = trimmed
        this.nextUrl = null
        viewModelScope.launch {
            val cached = contentCache.read(
                CacheKeys.search(trimmed),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString<SearchResult>(it)
            }
            if (cached != null) {
                _state.value = UiState.Success(cached)
                nextUrl = cached.nextUrl
            }
            if (_state.value == null) _state.value = UiState.Loading
            _state.value = try {
                val result = repository.search(trimmed, _selectedFid.value)
                this@SearchViewModel.nextUrl = result.nextUrl
                saveToCache(result)
                _resetScrollTick.value = _resetScrollTick.value + 1
                UiState.Success(result)
            } catch (e: Exception) {
                if (_state.value is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                    _state.value
                } else {
                    UiState.Error(e.message ?: "搜索失败")
                }
            }
        }
    }

    
    fun loadMore() {
        if (_loadingMore.value) return
        val current = (_state.value as? UiState.Success)?.data ?: return
        val target = current.nextUrl ?: return
        _loadingMore.value = true
        viewModelScope.launch {
            runCatching { repository.searchNext(target) }
                .onSuccess { next ->
                    nextUrl = next.nextUrl
                    val latest = (_state.value as? UiState.Success)?.data ?: current
                    val merged = SearchResult(
                        threads = (latest.threads + next.threads).distinctBy { it.tid },
                        nextUrl = next.nextUrl,
                    )
                    _state.value = UiState.Success(merged)
                    saveToCache(merged)
                }
                .onFailure {
                    _state.value = UiState.Success(current.copy(nextUrl = null))
                }
            _loadingMore.value = false
        }
    }

    
    fun searchForums(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        this.keyword = trimmed
        viewModelScope.launch {
            val cached = contentCache.read(
                CacheKeys.searchForum(trimmed),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString(ListSerializer(Forum.serializer()), it)
            }
            if (cached != null) _forumState.value = UiState.Success(cached)
            if (_forumState.value == null) _forumState.value = UiState.Loading
            _forumState.value = try {
                val forums = repository.forumGroups().flatMap { it.forums }
                    .filter { it.name.contains(trimmed, ignoreCase = true) }
                saveForumsToCache(trimmed, forums)
                _resetScrollTick.value = _resetScrollTick.value + 1
                UiState.Success(forums)
            } catch (e: Exception) {
                if (_forumState.value is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                    _forumState.value
                } else {
                    UiState.Error(e.message ?: "搜索失败")
                }
            }
        }
    }

    
    fun searchUsers(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        this.keyword = trimmed
        viewModelScope.launch {
            val cached = contentCache.read(
                CacheKeys.searchUser(trimmed),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString(ListSerializer(AtUser.serializer()), it)
            }
            if (cached != null) _userState.value = UiState.Success(cached)
            if (_userState.value == null) _userState.value = UiState.Loading
            _userState.value = try {
                val users = repository.searchUsers(trimmed)
                saveUsersToCache(trimmed, users)
                _resetScrollTick.value = _resetScrollTick.value + 1
                UiState.Success(users)
            } catch (e: Exception) {
                if (_userState.value is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                    _userState.value
                } else {
                    UiState.Error(e.message ?: "搜索失败")
                }
            }
        }
    }

    private suspend fun saveToCache(result: SearchResult) {
        contentCache.write(
            key = CacheKeys.search(keyword),
            ttlMillis = CacheKeys.LIST_TTL,
            value = result,
            encode = { cacheJson.encodeToString(SearchResult.serializer(), it) },
        )
    }

    private suspend fun saveForumsToCache(keyword: String, forums: List<Forum>) {
        contentCache.write(
            key = CacheKeys.searchForum(keyword),
            ttlMillis = CacheKeys.LIST_TTL,
            value = forums,
            encode = { cacheJson.encodeToString(ListSerializer(Forum.serializer()), it) },
        )
    }

    private suspend fun saveUsersToCache(keyword: String, users: List<AtUser>) {
        contentCache.write(
            key = CacheKeys.searchUser(keyword),
            ttlMillis = CacheKeys.LIST_TTL,
            value = users,
            encode = { cacheJson.encodeToString(ListSerializer(AtUser.serializer()), it) },
        )
    }
}
