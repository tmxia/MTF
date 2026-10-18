package com.aazhou.mtforum.ui.screen.myfriends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.FriendRequestItem
import com.aazhou.mtforum.data.model.FriendRequestPage
import com.aazhou.mtforum.data.model.FriendUserItem
import com.aazhou.mtforum.data.model.FriendUserPage
import com.aazhou.mtforum.data.repo.BlockedUsersStore
import com.aazhou.mtforum.data.repo.RelationStore
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.FriendsRepo
import com.aazhou.mtforum.data.repo.SessionStore
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.ui.common.SwipeTabIndicator
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.scrollToNewest
import com.aazhou.mtforum.ui.common.reportViewportAtTop
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


data class FriendsTabState(
    val state: UiState<List<FriendUserItem>> = UiState.Loading,
    val page: Int = 1,
    val loadingMore: Boolean = false,
    val hasNext: Boolean = true,
    





    val resetScrollTick: Int = 0,
)

class MyFriendsViewModel(
    private val repository: ForumRepository,
    private val friendsRepo: FriendsRepo,
    private val contentCache: ContentCache,
    private val sessionStore: SessionStore,
    private val blockedUsersStore: BlockedUsersStore,
    private val relationStore: RelationStore,
) : ViewModel() {

    
    private val _tabStates = MutableStateFlow<Map<String, FriendsTabState>>(emptyMap())
    val tabStates: StateFlow<Map<String, FriendsTabState>> = _tabStates.asStateFlow()

    
    private val _friendPending = MutableStateFlow("")
    val friendPending: StateFlow<String> = _friendPending.asStateFlow()

    
    private val _requestState = MutableStateFlow<UiState<List<FriendRequestItem>>>(UiState.Loading)
    val requestState: StateFlow<UiState<List<FriendRequestItem>>> = _requestState.asStateFlow()

    
    private val _requestResetTick = MutableStateFlow(0)
    val requestResetTick: StateFlow<Int> = _requestResetTick.asStateFlow()

    private val _requestHasNext = MutableStateFlow(false)
    val requestHasNext: StateFlow<Boolean> = _requestHasNext.asStateFlow()

    private val _requestLoadingMore = MutableStateFlow(false)
    val requestLoadingMore: StateFlow<Boolean> = _requestLoadingMore.asStateFlow()

    
    private val _requestPending = MutableStateFlow("")
    val requestPending: StateFlow<String> = _requestPending.asStateFlow()

    
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    
    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    val refreshing: StateFlow<Set<String>> = _refreshing.asStateFlow()

    
    private val loadMoreJobs = mutableMapOf<String, Job>()

    
    private var loadMoreRequestsJob: Job? = null

    





    private val viewportAtTop = mutableMapOf<String, Boolean>()

    
    fun noteViewportAtTop(view: String, atTop: Boolean) {
        viewportAtTop[view] = atTop
    }

    private val cacheJson = Json { ignoreUnknownKeys = true }

    private var requestFormhash: String? = null
    private var requestPage = 1
    private var requestLoaded = false


    private val entered = mutableSetOf<String>()

    




    fun load(view: String) {
        entered += view
        if (_tabStates.value.containsKey(view)) return
        update(view) { FriendsTabState() }
        viewModelScope.launch {
            val uid = sessionStore.uid.first()
            
            val cached = contentCache.read(
                CacheKeys.myFriends(uid, view),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString<FriendUserPage>(it)
            }
            if (cached != null) {
                update(view) {
                    it.copy(
                        state = UiState.Success(cached.users),
                        page = cached.page,
                        hasNext = cached.hasNext,
                    )
                }
            }
            reload(view, uid)
        }
    }

    
    fun retry(view: String) {
        viewModelScope.launch { reload(view, sessionStore.uid.first()) }
    }






    fun refreshOnEnter(view: String) {
        if (view !in entered) return
        
        
        if (view == VIEW_REQUEST) {
            if (_requestState.value is UiState.Success) return
        } else {
            if (_tabStates.value[view]?.state is UiState.Success) return
        }
        viewModelScope.launch {
            val uid = sessionStore.uid.first()
            if (view == VIEW_REQUEST) reloadRequests(uid) else reload(view, uid)
        }
    }

    




    fun refresh(view: String) {
        if (view in _refreshing.value) return
        
        loadMoreJobs.remove(view)?.cancel()
        loadMoreRequestsJob?.cancel()
        if (_tabStates.value.containsKey(view)) update(view) { it.copy(loadingMore = false) }
        _requestLoadingMore.value = false
        _refreshing.value = _refreshing.value + view
        viewModelScope.launch {
            if (view == VIEW_REQUEST) {
                reloadRequests(sessionStore.uid.first())
            } else {
                reload(view, sessionStore.uid.first())
            }
            _refreshing.value = _refreshing.value - view
        }
    }

    private suspend fun reload(view: String, uid: String) {
        runCatching { repository.myFriends(view = view, page = 1) }
            .onSuccess { data ->
                update(view) {
                    it.copy(
                        state = UiState.Success(data.users),
                        page = 1,
                        hasNext = data.hasNext,
                        
                        resetScrollTick = it.resetScrollTick +
                            if (viewportAtTop[view] == true) 1 else 0,
                    )
                }
                saveToCache(uid, view)
                
                val uids = data.users.map { it.uid }
                when (view) {
                    "blacklist" -> blockedUsersStore.addAll(uids)
                    "" -> relationStore.addAllFriends(uids)
                }
            }
            .onFailure { e ->
                
                if (_tabStates.value[view]?.state is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                } else {
                    update(view) { it.copy(state = UiState.Error(e.message ?: "加载失败")) }
                }
            }
    }

    
    fun loadMore(view: String) {
        val current = _tabStates.value[view] ?: return
        if (current.loadingMore || !current.hasNext) return
        if (current.state !is UiState.Success) return
        update(view) { it.copy(loadingMore = true) }
        loadMoreJobs[view] = viewModelScope.launch {
            val next = current.page + 1
            runCatching { repository.myFriends(view = view, page = next) }
                .onSuccess { data ->
                    
                    val existing = (_tabStates.value[view]?.state as? UiState.Success)?.data.orEmpty()
                    val known = existing.mapTo(HashSet()) { it.uid }
                    val fresh = data.users.filter { known.add(it.uid) }
                    update(view) {
                        it.copy(
                            state = UiState.Success(existing + fresh),
                            page = next,
                            hasNext = data.hasNext && fresh.isNotEmpty(),
                        )
                    }
                    saveToCache(sessionStore.uid.first(), view)
                    val freshUids = fresh.map { it.uid }
                    when (view) {
                        "blacklist" -> blockedUsersStore.addAll(freshUids)
                        "" -> relationStore.addAllFriends(freshUids)
                    }
                }
                .onFailure { update(view) { it.copy(hasNext = false) } }
            update(view) { it.copy(loadingMore = false) }
        }
    }

    
    fun loadRequests() {
        entered += VIEW_REQUEST
        if (requestLoaded) return
        requestLoaded = true
        viewModelScope.launch {
            val uid = sessionStore.uid.first()
            val cached = contentCache.read(
                CacheKeys.friendRequests(uid),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString<FriendRequestPage>(it)
            }
            if (cached != null) {
                requestPage = cached.page
                _requestHasNext.value = cached.hasNext
                _requestState.value = UiState.Success(cached.requests)
            }
            reloadRequests(uid)
        }
    }

    
    fun retryRequests() {
        viewModelScope.launch { reloadRequests(sessionStore.uid.first()) }
    }

    private suspend fun reloadRequests(uid: String) {
        runCatching { friendsRepo.friendRequests(1) }
            .onSuccess { pageData ->
                requestFormhash = pageData.formhash
                requestPage = 1
                _requestHasNext.value = pageData.hasNext
                _requestState.value = UiState.Success(pageData.requests)
                
                if (viewportAtTop[VIEW_REQUEST] == true) {
                    _requestResetTick.value = _requestResetTick.value + 1
                }
                saveRequestsToCache(uid)
            }
            .onFailure { e ->
                if (_requestState.value is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                } else {
                    _requestState.value = UiState.Error(e.message ?: "加载失败")
                }
            }
    }

    fun loadMoreRequests() {
        if (_requestLoadingMore.value || !_requestHasNext.value) return
        if (_requestState.value !is UiState.Success) return
        _requestLoadingMore.value = true
        loadMoreRequestsJob = viewModelScope.launch {
            val next = requestPage + 1
            runCatching { friendsRepo.friendRequests(next) }
                .onSuccess { pageData ->
                    val current = (_requestState.value as? UiState.Success)?.data.orEmpty()
                    val known = current.mapTo(HashSet()) { it.uid }
                    val fresh = pageData.requests.filter { known.add(it.uid) }
                    _requestState.value = UiState.Success(current + fresh)
                    requestPage = next
                    _requestHasNext.value = pageData.hasNext && fresh.isNotEmpty()
                    saveRequestsToCache(sessionStore.uid.first())
                }
                .onFailure { _requestHasNext.value = false }
            _requestLoadingMore.value = false
        }
    }

    fun approveRequest(item: FriendRequestItem) {
        operateRequest(item, "已同意") { friendsRepo.approveFriendRequest(item.uid, requestFormhash.orEmpty()) }
    }

    fun ignoreRequest(item: FriendRequestItem) {
        operateRequest(item, "已忽略") { friendsRepo.ignoreFriendRequest(item.uid, requestFormhash.orEmpty()) }
    }

    private fun operateRequest(item: FriendRequestItem, successText: String, op: suspend () -> Result<String>) {
        if (_requestPending.value.isNotEmpty()) return
        _requestPending.value = item.uid
        viewModelScope.launch {
            val result = runCatching { op() }.getOrElse { Result.failure(it) }
            result
                .onSuccess {
                    AppToast.show(it.ifBlank { successText })
                    val current = (_requestState.value as? UiState.Success)?.data.orEmpty()
                    _requestState.value = UiState.Success(current.filterNot { it.uid == item.uid })
                    saveRequestsToCache(sessionStore.uid.first())
                }
                .onFailure { e -> AppToast.show(e.message ?: "操作失败") }
            _requestPending.value = ""
        }
    }

    
    fun removeFriend(user: FriendUserItem) {
        if (_friendPending.value.isNotEmpty()) return
        _friendPending.value = user.uid
        viewModelScope.launch {
            val result = runCatching { friendsRepo.removeFriend(user.uid, "") }
                .getOrElse { Result.failure(it) }
            result
                .onSuccess {
                    AppToast.show(it)
                    dropUserEverywhere(user.uid)
                }
                .onFailure { e -> AppToast.show(e.message ?: "删除失败") }
            _friendPending.value = ""
        }
    }

    
    fun unblacklistUser(user: FriendUserItem) {
        if (_friendPending.value.isNotEmpty()) return
        _friendPending.value = user.uid
        viewModelScope.launch {
            val result = runCatching { repository.removeUserBlacklist(user.uid) }
                .getOrElse { Result.failure(it) }
            result
                .onSuccess {
                    AppToast.show(it)
                    blockedUsersStore.remove(user.uid)   
                    dropUserEverywhere(user.uid)
                }
                .onFailure { e -> AppToast.show(e.message ?: "移出黑名单失败") }
            _friendPending.value = ""
        }
    }

    
    private suspend fun dropUserEverywhere(uid: String) {
        val affected = _tabStates.value.keys.filter { view ->
            (_tabStates.value[view]?.state as? UiState.Success)?.data?.any { it.uid == uid } == true
        }
        affected.forEach { view ->
            val current = (_tabStates.value[view]?.state as? UiState.Success)?.data.orEmpty()
            update(view) {
                it.copy(state = UiState.Success(current.filterNot { user -> user.uid == uid }))
            }
        }
        val currentUid = sessionStore.uid.first()
        affected.forEach { view -> saveToCache(currentUid, view) }
    }

    private fun update(view: String, transform: (FriendsTabState) -> FriendsTabState) {
        val current = _tabStates.value[view] ?: FriendsTabState()
        _tabStates.value = _tabStates.value + (view to transform(current))
    }

    
    private suspend fun saveToCache(uid: String, view: String) {
        val tab = _tabStates.value[view] ?: return
        val users = (tab.state as? UiState.Success)?.data ?: return
        contentCache.write(
            key = CacheKeys.myFriends(uid, view),
            ttlMillis = CacheKeys.LIST_TTL,
            value = FriendUserPage(users = users, page = tab.page, hasNext = tab.hasNext),
            encode = { cacheJson.encodeToString(FriendUserPage.serializer(), it) },
        )
    }

    



    private suspend fun saveRequestsToCache(uid: String) {
        val requests = (_requestState.value as? UiState.Success)?.data ?: return
        contentCache.write(
            key = CacheKeys.friendRequests(uid),
            ttlMillis = CacheKeys.LIST_TTL,
            value = FriendRequestPage(requests = requests, page = requestPage, hasNext = _requestHasNext.value),
            encode = { cacheJson.encodeToString(FriendRequestPage.serializer(), it.copy(formhash = null)) },
        )
    }

    companion object {
        const val VIEW_REQUEST = "request"
    }
}

private val MY_FRIENDS_TABS = listOf(
    "全部好友" to "",
    "最近来访" to "visitor",
    "我看过谁" to "trace",
    "黑名单" to "blacklist",
    "好友请求" to MyFriendsViewModel.VIEW_REQUEST,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFriendsScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
) {
    val colors = LocalMtfColors.current
    val viewModel: MyFriendsViewModel = appViewModel {
        MyFriendsViewModel(it.repository, FriendsRepo(it.httpClient), it.contentCache, it.sessionStore, it.blockedUsersStore, it.relationStore)
    }
    val tabs = MY_FRIENDS_TABS
    val tabStates by viewModel.tabStates.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val friendPending by viewModel.friendPending.collectAsStateWithLifecycle()
    val requestState by viewModel.requestState.collectAsStateWithLifecycle()
    val requestHasNext by viewModel.requestHasNext.collectAsStateWithLifecycle()
    val requestLoadingMore by viewModel.requestLoadingMore.collectAsStateWithLifecycle()
    val requestPending by viewModel.requestPending.collectAsStateWithLifecycle()
    val requestResetTick by viewModel.requestResetTick.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    
    val listStates = tabs.map { _k -> key(_k) { rememberLazyListState() } }
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) { viewModel.refreshOnEnter(tabs[pagerState.currentPage].second) }

    
    LaunchedEffect(actionMessage) {
        val message = actionMessage ?: return@LaunchedEffect
        AppToast.show(message)
        viewModel.consumeActionMessage()
    }

    Scaffold(
        containerColor = colors.background,
        contentColor = bodyTextColor(),
        topBar = {
            Column(Modifier.background(colors.topBar)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.topBar),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = "返回",
                                tint = bodyTextColor(),
                            )
                        }
                    },
                    title = {
                        Text(
                            "我的好友",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = bodyTextColor(),
                        )
                    },
                    expandedHeight = MtfTopBarHeight,
                )
                val accent = accentTextColor()
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = colors.topBar,
                    contentColor = accent,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        SwipeTabIndicator(pagerState, tabPositions, accent)
                    },
                ) {
                    tabs.forEachIndexed { index, (title, _) ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = if (pagerState.currentPage == index) accent else colors.textSecondary,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            
            beyondViewportPageCount = 2,
        ) { page ->
            val view = tabs[page].second
            if (view == MyFriendsViewModel.VIEW_REQUEST) {
                
                LaunchedEffect(view) { viewModel.loadRequests() }
                FriendRequestList(
                    requestState = requestState,
                    resetScrollTick = requestResetTick,
                    refreshing = view in refreshing,
                    pending = requestPending,
                    loadingMore = requestLoadingMore,
                    hasNext = requestHasNext,
                    listState = listStates[page],
                    onViewportAtTopChange = { viewModel.noteViewportAtTop(MyFriendsViewModel.VIEW_REQUEST, it) },
                    onRetry = { viewModel.retryRequests() },
                    onRefresh = { viewModel.refresh(view) },
                    onApprove = { viewModel.approveRequest(it) },
                    onIgnore = { viewModel.ignoreRequest(it) },
                    onLoadMore = { viewModel.loadMoreRequests() },
                )
            } else {
                
                LaunchedEffect(view) { viewModel.load(view) }
                val tab = tabStates[view] ?: FriendsTabState()
                FriendsTab(
                    tab = tab,
                    refreshing = view in refreshing,
                    view = view,
                    listState = listStates[page],
                    onViewportAtTopChange = { viewModel.noteViewportAtTop(view, it) },
                    emptyText = "暂无${tabs[page].first}信息",
                    pendingUid = friendPending,
                    onRetry = { viewModel.retry(view) },
                    onRefresh = { viewModel.refresh(view) },
                    onLoadMore = { viewModel.loadMore(view) },
                    onUserClick = onUserClick,
                    onDelete = { viewModel.removeFriend(it) },
                    onUnblock = { viewModel.unblacklistUser(it) },
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsTab(
    tab: FriendsTabState,
    refreshing: Boolean,
    view: String,
    listState: LazyListState,
    onViewportAtTopChange: (Boolean) -> Unit,
    emptyText: String,
    pendingUid: String,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (String) -> Unit,
    onDelete: (FriendUserItem) -> Unit,
    onUnblock: (FriendUserItem) -> Unit,
) {
    
    
    
    listState.reportViewportAtTop(onViewportAtTopChange)

    


    val initialTick = remember { tab.resetScrollTick }


    LaunchedEffect(tab.resetScrollTick) {


        if (tab.resetScrollTick > initialTick) {


            listState.scrollToNewest()


        }


    }
    when (val current = tab.state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(current.message, onRetry)
        is UiState.Success -> {
            val users = current.data
            if (users.isEmpty()) {
                EmptyBox(emptyText)
            } else {
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = listState.layoutInfo.totalItemsCount
                        total > 0 && last >= total - 3
                    }
                }
                LaunchedEffect(shouldLoadMore, tab.hasNext) {
                    if (shouldLoadMore && tab.hasNext) onLoadMore()
                }
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(users, key = { it.uid }) { user ->
                            FriendUserCard(
                                user = user,
                                removing = pendingUid == user.uid,
                                showDelete = view.isEmpty(),
                                showUnblock = view == "blacklist",
                                onClick = { onUserClick(user.uid) },
                                onDelete = { onDelete(user) },
                                onUnblock = { onUnblock(user) },
                            )
                        }
                        if (tab.loadingMore) {
                            item(key = "loading-more") {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendRequestList(
    requestState: UiState<List<FriendRequestItem>>,
    resetScrollTick: Int,
    refreshing: Boolean,
    pending: String,
    loadingMore: Boolean,
    hasNext: Boolean,
    listState: LazyListState,
    onViewportAtTopChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onApprove: (FriendRequestItem) -> Unit,
    onIgnore: (FriendRequestItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    
    
    
    listState.reportViewportAtTop(onViewportAtTopChange)

    


    val initialTick = remember { resetScrollTick }


    LaunchedEffect(resetScrollTick) {


        if (resetScrollTick > initialTick) {


            listState.scrollToNewest()


        }


    }
    when (val current = requestState) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(current.message, onRetry)
        is UiState.Success -> {
            val requests = current.data
            if (requests.isEmpty()) {
                EmptyBox("暂无好友请求")
            } else {
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = listState.layoutInfo.totalItemsCount
                        total > 0 && last >= total - 3
                    }
                }
                LaunchedEffect(shouldLoadMore, hasNext) {
                    if (shouldLoadMore && hasNext) onLoadMore()
                }
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(requests, key = { it.uid }) { item ->
                            FriendRequestCard(
                                item = item,
                                pending = pending == item.uid,
                                onApprove = { onApprove(item) },
                                onIgnore = { onIgnore(item) },
                            )
                        }
                        if (loadingMore) {
                            item(key = "loading-more") {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    item: FriendRequestItem,
    pending: Boolean,
    onApprove: () -> Unit,
    onIgnore: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isDark) {
                    Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        AsyncImage(
            model = smallImageRequest(item.avatarUrl),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.quoteBackground)
                .dimImageInDark(),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.username,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.description.isNullOrBlank()) {
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (pending) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.textHint)
        } else {
            TextButton(onClick = onIgnore) {
                Text("忽略", fontSize = 13.sp, color = colors.textSecondary)
            }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(
                onClick = onApprove,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text("同意", fontSize = 13.sp, color = accentTextColor())
            }
        }
    }
}

@Composable
private fun FriendUserCard(
    user: FriendUserItem,
    removing: Boolean,
    showDelete: Boolean,
    showUnblock: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onUnblock: () -> Unit = {},
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isDark) {
                    Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        AsyncImage(
            model = smallImageRequest(user.avatarUrl),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.quoteBackground)
                .dimImageInDark(),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                user.username,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!user.description.isNullOrBlank()) {
                Text(
                    user.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (showUnblock) {
            if (removing) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.textHint,
                )
            } else {
                Text(
                    "移出黑名单",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onUnblock)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        } else if (showDelete) {
            if (removing) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.textHint,
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = "删除好友",
                        tint = colors.textHint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            Icon(
                painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = colors.textHint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
