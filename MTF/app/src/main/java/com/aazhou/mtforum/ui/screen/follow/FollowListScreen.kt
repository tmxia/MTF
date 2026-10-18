package com.aazhou.mtforum.ui.screen.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.FollowUserItem
import com.aazhou.mtforum.data.model.FollowUserPage
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.RelationStore
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


data class FollowTabState(
    val state: UiState<List<FollowUserItem>> = UiState.Loading,
    val page: Int = 1,
    val loadingMore: Boolean = false,
    val hasNext: Boolean = true,
    





    val resetScrollTick: Int = 0,
)

class FollowListViewModel(
    initialType: String,
    private val repository: ForumRepository,
    private val contentCache: ContentCache,
    private val sessionStore: SessionStore,
    private val relationStore: RelationStore,
) : ViewModel() {

    
    private val _tabStates = MutableStateFlow<Map<String, FollowTabState>>(emptyMap())
    val tabStates: StateFlow<Map<String, FollowTabState>> = _tabStates.asStateFlow()

    
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    
    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    val refreshing: StateFlow<Set<String>> = _refreshing.asStateFlow()

    
    private val loadMoreJobs = mutableMapOf<String, Job>()

    
    private val entered = mutableSetOf<String>()

    





    private val viewportAtTop = mutableMapOf<String, Boolean>()

    
    fun noteViewportAtTop(type: String, atTop: Boolean) {
        viewportAtTop[type] = atTop
    }

    private val cacheJson = Json { ignoreUnknownKeys = true }

    init {
        load(initialType)
    }

    




    fun load(type: String) {
        entered += type
        if (_tabStates.value.containsKey(type)) return
        update(type) { FollowTabState() }
        viewModelScope.launch {
            val uid = sessionStore.uid.first()
            
            val cached = contentCache.read(
                CacheKeys.followList(uid, type),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString<FollowUserPage>(it)
            }
            if (cached != null) {
                update(type) {
                    it.copy(
                        state = UiState.Success(cached.users),
                        page = cached.page,
                        hasNext = cached.hasNext,
                    )
                }
            }
            reload(type, uid)
        }
    }

    
    fun retry(type: String) {
        viewModelScope.launch { reload(type, sessionStore.uid.first()) }
    }






    fun refreshOnEnter(type: String) {
        if (type !in entered) return
        
        
        if (_tabStates.value[type]?.state is UiState.Success) return
        viewModelScope.launch { reload(type, sessionStore.uid.first()) }
    }

    




    fun refresh(type: String) {
        if (type in _refreshing.value) return
        
        loadMoreJobs.remove(type)?.cancel()
        if (_tabStates.value.containsKey(type)) update(type) { it.copy(loadingMore = false) }
        _refreshing.value = _refreshing.value + type
        viewModelScope.launch {
            reload(type, sessionStore.uid.first())
            _refreshing.value = _refreshing.value - type
        }
    }

    private suspend fun reload(type: String, uid: String) {
        runCatching { repository.followList(type = type, page = 1) }
            .onSuccess { data ->
                update(type) {
                    it.copy(
                        state = UiState.Success(data.users),
                        page = 1,
                        hasNext = data.hasNext,
                        
                        resetScrollTick = it.resetScrollTick +
                            if (viewportAtTop[type] == true) 1 else 0,
                    )
                }
                saveToCache(uid, type)
                val uids = data.users.map { it.uid }
                when (type) {
                    "following" -> relationStore.addAllFollowing(uids)
                    "follower" -> relationStore.addAllFollowers(uids)
                }
            }
            .onFailure { e ->
                
                if (_tabStates.value[type]?.state is UiState.Success) {
                    _actionMessage.value = "新数据获取失败"
                } else {
                    update(type) { it.copy(state = UiState.Error(e.message ?: "加载失败")) }
                }
            }
    }

    
    fun loadMore(type: String) {
        val current = _tabStates.value[type] ?: return
        if (current.loadingMore || !current.hasNext) return
        if (current.state !is UiState.Success) return
        update(type) { it.copy(loadingMore = true) }
        loadMoreJobs[type] = viewModelScope.launch {
            val next = current.page + 1
            runCatching { repository.followList(type = type, page = next) }
                .onSuccess { data ->
                    
                    val existing = (_tabStates.value[type]?.state as? UiState.Success)?.data.orEmpty()
                    val known = existing.mapTo(HashSet()) { it.uid }
                    val fresh = data.users.filter { known.add(it.uid) }
                    update(type) {
                        it.copy(
                            state = UiState.Success(existing + fresh),
                            page = next,
                            hasNext = data.hasNext && fresh.isNotEmpty(),
                        )
                    }
                    saveToCache(sessionStore.uid.first(), type)
                    val freshUids = fresh.map { it.uid }
                    when (type) {
                        "following" -> relationStore.addAllFollowing(freshUids)
                        "follower" -> relationStore.addAllFollowers(freshUids)
                    }
                }
                .onFailure { update(type) { it.copy(hasNext = false) } }
            update(type) { it.copy(loadingMore = false) }
        }
    }

    
    fun toggleFollow(type: String, user: FollowUserItem) {
        val target = !user.isFollowing
        val affected = _tabStates.value.keys.filter { key -> listOf(key).any { it.uid == user.uid } }
        val snapshot = affected.associateWith { listOf(it) }
        if (affected.isEmpty()) return
        affected.forEach { key ->
            update(key) { tab ->
                tab.copy(
                    state = UiState.Success(
                        snapshot[key].orEmpty().map {
                            if (it.uid == user.uid) it.copy(isFollowing = target) else it
                        },
                    ),
                )
            }
        }
        viewModelScope.launch {
            val ok = runCatching { repository.toggleFollowUser(user.uid, target) }
                .getOrDefault(false)
            if (ok) {
                AppToast.show(if (target) "关注成功" else "已取消关注")
                relationStore.setFollowing(user.uid, target)
                val uid = sessionStore.uid.first()
                affected.forEach { key -> saveToCache(uid, key) }
            } else {
                AppToast.show("操作失败")
                snapshot.forEach { (key, list) ->
                    update(key) { it.copy(state = UiState.Success(list)) }
                }
            }
        }
    }

    
    fun setRemark(type: String, user: FollowUserItem, remark: String) {
        viewModelScope.launch {
            val result = runCatching { repository.setFollowRemark(user.uid, "", remark) }
                .getOrElse { Result.failure(it) }
            result
                .onSuccess { msg ->
                    AppToast.show(msg.ifBlank { "备注成功" })
                    update(type) { tab ->
                        tab.copy(
                            state = UiState.Success(
                                (_tabStates.value[type]?.state as? UiState.Success)?.data
                                    ?.map { if (it.uid == user.uid) it.copy(remark = remark) else it }
                                    ?: emptyList(),
                            ),
                        )
                    }
                    saveToCache(sessionStore.uid.first(), type)
                }
                .onFailure { e -> AppToast.show(e.message ?: "备注失败") }
        }
    }

    private fun listOf(type: String): List<FollowUserItem> =
        (_tabStates.value[type]?.state as? UiState.Success)?.data ?: emptyList()

    private fun update(type: String, transform: (FollowTabState) -> FollowTabState) {
        val current = _tabStates.value[type] ?: FollowTabState()
        _tabStates.value = _tabStates.value + (type to transform(current))
    }

    
    private suspend fun saveToCache(uid: String, type: String) {
        val tab = _tabStates.value[type] ?: return
        val users = (tab.state as? UiState.Success)?.data ?: return
        contentCache.write(
            key = CacheKeys.followList(uid, type),
            ttlMillis = CacheKeys.LIST_TTL,
            value = FollowUserPage(users = users, page = tab.page, hasNext = tab.hasNext),
            encode = { cacheJson.encodeToString(FollowUserPage.serializer(), it) },
        )
    }
}

private val FOLLOW_TABS = listOf("我的关注" to "following", "我的粉丝" to "follower")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    initialType: String = "following",
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
) {
    val colors = LocalMtfColors.current
    val viewModel: FollowListViewModel = appViewModel {
        FollowListViewModel(initialType, it.repository, it.contentCache, it.sessionStore, it.relationStore)
    }
    val tabs = FOLLOW_TABS
    val tabStates by viewModel.tabStates.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = if (initialType == "follower") 1 else 0,
    ) { tabs.size }
    
    val listStates = tabs.map { _k -> key(_k) { rememberLazyListState() } }
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) { viewModel.refreshOnEnter(tabs[pagerState.currentPage].second) }

    
    var remarkTarget by remember { mutableStateOf<FollowUserItem?>(null) }

    
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
                            tabs[pagerState.currentPage].first,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = bodyTextColor(),
                        )
                    },
                    expandedHeight = MtfTopBarHeight,
                )
                val accent = accentTextColor()
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = colors.topBar,
                    contentColor = accent,
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
            val type = tabs[page].second
            
            LaunchedEffect(type) { viewModel.load(type) }
            val tab = tabStates[type] ?: FollowTabState()
            FollowTab(
                tab = tab,
                refreshing = type in refreshing,
                listState = listStates[page],
                onViewportAtTopChange = { viewModel.noteViewportAtTop(type, it) },
                emptyText = "暂无${tabs[page].first}用户",
                showRemark = type == "following",
                onRetry = { viewModel.retry(type) },
                onRefresh = { viewModel.refresh(type) },
                onLoadMore = { viewModel.loadMore(type) },
                onUserClick = onUserClick,
                onToggleFollow = { user -> viewModel.toggleFollow(type, user) },
                onRemark = { user -> remarkTarget = user },
            )
        }
    }

    remarkTarget?.let { target ->
        RemarkDialog(
            target = target,
            onDismiss = { remarkTarget = null },
            onConfirm = { remark ->
                viewModel.setRemark("following", target, remark)
                remarkTarget = null
            },
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowTab(
    tab: FollowTabState,
    refreshing: Boolean,
    listState: LazyListState,
    onViewportAtTopChange: (Boolean) -> Unit,
    emptyText: String,
    showRemark: Boolean,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (String) -> Unit,
    onToggleFollow: (FollowUserItem) -> Unit,
    onRemark: (FollowUserItem) -> Unit,
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
                            FollowUserCard(
                                user = user,
                                showRemark = showRemark,
                                onClick = { onUserClick(user.uid) },
                                onToggleFollow = { onToggleFollow(user) },
                                onRemark = { onRemark(user) },
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

@Composable
private fun FollowUserCard(
    user: FollowUserItem,
    showRemark: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onRemark: () -> Unit,
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
        val remark = user.remark?.takeIf { it.isNotBlank() }
        Column(Modifier.weight(1f)) {
            Text(
                user.username,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (remark != null) {
                Text(
                    "备注：$remark",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val stats = listOfNotNull(
                user.fansCount?.let { "$it 粉丝" },
                user.followCount?.let { "$it 关注" },
            ).joinToString(" · ")
            if (stats.isNotBlank()) {
                Text(
                    stats,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showRemark) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(0.8.dp, colors.divider, CircleShape)
                        .clickable(onClick = onRemark),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_two),
                        contentDescription = "备注",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            val isFollowed = user.isFollowing
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isFollowed) {
                            Modifier
                                .border(0.8.dp, colors.divider, RoundedCornerShape(14.dp))
                                .background(colors.surface)
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        },
                    )
                    .clickable(onClick = onToggleFollow)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isFollowed) "已关注" else "+ 关注",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = if (isFollowed) FontWeight.Normal else FontWeight.Medium,
                    color = if (isFollowed) colors.textSecondary else accentTextColor(),
                )
            }
        }
    }
}


@Composable
private fun RemarkDialog(
    target: FollowUserItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var remark by remember { mutableStateOf(target.remark.orEmpty()) }
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .then(
                    if (isDark) {
                        Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(20.dp))
                    } else {
                        Modifier
                    },
                ),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (target.remark.isNullOrBlank()) "添加备注" else "修改备注",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = bodyTextColor(),
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "为「${target.username}」设置备注",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = remark,
                    onValueChange = { if (it.length <= 30) remark = it },
                    placeholder = {
                        Text(
                            "备注名（留空清除备注）",
                            color = colors.textHint,
                            fontSize = 14.sp,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.quoteBackground,
                        unfocusedContainerColor = colors.quoteBackground,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = colors.divider,
                        focusedTextColor = bodyTextColor(),
                        unfocusedTextColor = bodyTextColor(),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.quoteBackground)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Medium,
                            color = bodyTextColor(),
                        )
                    }

                    val btnBg = if (isDark) primaryColor.copy(alpha = 0.22f) else primaryColor
                    val btnTextColor = if (isDark) primaryColor else Color.White
                    Button(
                        onClick = { onConfirm(remark.trim()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .then(
                                if (isDark) {
                                    Modifier.border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = btnBg,
                            contentColor = btnTextColor,
                        ),
                    ) {
                        Text(
                            text = "保存",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
