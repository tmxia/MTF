package com.aazhou.mtforum.ui.screen.myfavorites

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.FavoriteItem
import com.aazhou.mtforum.data.model.FavoritePage
import com.aazhou.mtforum.data.repo.ActionStore
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.SessionStore
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.MtfConfirmDialog
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


data class FavoritesTabState(
    val state: UiState<List<FavoriteItem>> = UiState.Loading,
    val page: Int = 1,
    val loadingMore: Boolean = false,
    val hasNext: Boolean = true,
    





    val resetScrollTick: Int = 0,
)

class MyFavoritesViewModel(
    private val repository: ForumRepository,
    private val actionStore: ActionStore,
    private val contentCache: ContentCache,
    private val sessionStore: SessionStore,
) : ViewModel() {

    
    private val _tabStates = MutableStateFlow<Map<String, FavoritesTabState>>(emptyMap())
    val tabStates: StateFlow<Map<String, FavoritesTabState>> = _tabStates.asStateFlow()

    
    private val _removing = MutableStateFlow("")
    val removing: StateFlow<String> = _removing.asStateFlow()

    
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

    




    fun load(type: String) {
        entered += type
        if (_tabStates.value.containsKey(type)) return
        update(type) { FavoritesTabState() }
        viewModelScope.launch {
            val uid = sessionStore.uid.first()
            
            val cached = contentCache.read(
                CacheKeys.myFavorites(uid, type),
                CacheKeys.LIST_TTL,
                allowExpired = true,
            ) {
                cacheJson.decodeFromString<FavoritePage>(it)
            }
            if (cached != null) {
                update(type) {
                    it.copy(
                        state = UiState.Success(cached.items),
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
        runCatching { repository.myFavorites(type = type, page = 1) }
            .onSuccess { data ->
                update(type) {
                    it.copy(
                        state = UiState.Success(data.items),
                        page = 1,
                        hasNext = data.hasNext,
                        
                        resetScrollTick = it.resetScrollTick +
                            if (viewportAtTop[type] == true) 1 else 0,
                    )
                }
                saveToCache(uid, type)
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
            runCatching { repository.myFavorites(type = type, page = next) }
                .onSuccess { data ->
                    
                    val existing = (_tabStates.value[type]?.state as? UiState.Success)?.data.orEmpty()
                    val known = existing.mapTo(HashSet()) { favoriteKey(it) }
                    val fresh = data.items.filter { known.add(favoriteKey(it)) }
                    update(type) {
                        it.copy(
                            state = UiState.Success(existing + fresh),
                            page = next,
                            hasNext = data.hasNext && fresh.isNotEmpty(),
                        )
                    }
                    saveToCache(sessionStore.uid.first(), type)
                }
                .onFailure { update(type) { it.copy(hasNext = false) } }
            update(type) { it.copy(loadingMore = false) }
        }
    }

    
    fun removeFavorite(item: FavoriteItem) {
        val key = item.favid.ifEmpty { item.id }
        if (_removing.value.isNotEmpty() || key.isEmpty()) return
        val affected = _tabStates.value.keys.filter { type -> listOf(type).any { sameFavorite(it, item) } }
        if (affected.isEmpty()) return
        val snapshot = affected.associateWith { listOf(it) }
        _removing.value = key
        affected.forEach { type ->
            update(type) {
                it.copy(
                    state = UiState.Success(snapshot[type].orEmpty().filterNot { fav -> sameFavorite(fav, item) }),
                )
            }
        }
        viewModelScope.launch {
            val ok = runCatching { repository.deleteFavorite(item.favid) }
                .getOrElse { false }
            if (ok) {
                AppToast.show("已取消收藏")
                
                if (item.type == "thread" && item.id.isNotBlank()) {
                    actionStore.setFavorited(item.id, false)
                }
                val uid = sessionStore.uid.first()
                affected.forEach { type -> saveToCache(uid, type) }
            } else {
                AppToast.show("取消收藏失败")
                snapshot.forEach { (type, list) ->
                    update(type) { it.copy(state = UiState.Success(list)) }
                }
            }
            _removing.value = ""
        }
    }

    private fun listOf(type: String): List<FavoriteItem> =
        (_tabStates.value[type]?.state as? UiState.Success)?.data ?: emptyList()

    private fun favoriteKey(item: FavoriteItem): String = item.favid.ifEmpty { item.id }

    private fun sameFavorite(a: FavoriteItem, b: FavoriteItem): Boolean =
        (a.favid.isNotEmpty() && a.favid == b.favid) || (a.id.isNotEmpty() && a.id == b.id)

    private fun update(type: String, transform: (FavoritesTabState) -> FavoritesTabState) {
        val current = _tabStates.value[type] ?: FavoritesTabState()
        _tabStates.value = _tabStates.value + (type to transform(current))
    }

    
    private suspend fun saveToCache(uid: String, type: String) {
        val tab = _tabStates.value[type] ?: return
        val items = (tab.state as? UiState.Success)?.data ?: return
        contentCache.write(
            key = CacheKeys.myFavorites(uid, type),
            ttlMillis = CacheKeys.LIST_TTL,
            value = FavoritePage(items = items, page = tab.page, hasNext = tab.hasNext),
            encode = { cacheJson.encodeToString(FavoritePage.serializer(), it) },
        )
    }
}

private val MY_FAVORITES_TABS = listOf("全部" to "all", "帖子" to "thread", "版块" to "forum")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFavoritesScreen(
    onBack: () -> Unit,
    onThreadClick: (String) -> Unit,
    onForumClick: (String, String) -> Unit,
) {
    val colors = LocalMtfColors.current
    val viewModel: MyFavoritesViewModel = appViewModel {
        MyFavoritesViewModel(it.repository, it.actionStore, it.contentCache, it.sessionStore)
    }
    val tabs = MY_FAVORITES_TABS
    val tabStates by viewModel.tabStates.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val removingKey by viewModel.removing.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    
    val listStates = tabs.map { _k -> key(_k) { rememberLazyListState() } }
    val scope = rememberCoroutineScope()

    
    var pendingRemove by remember { mutableStateOf<FavoriteItem?>(null) }


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
                            "我的收藏",
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
            val tab = tabStates[type] ?: FavoritesTabState()
            FavoritesTab(
                tab = tab,
                refreshing = type in refreshing,
                listState = listStates[page],
                onViewportAtTopChange = { viewModel.noteViewportAtTop(type, it) },
                removingKey = removingKey,
                onRetry = { viewModel.retry(type) },
                onRefresh = { viewModel.refresh(type) },
                onLoadMore = { viewModel.loadMore(type) },
                onClick = { fav ->
                    if (fav.type == "forum") {
                        onForumClick(fav.id, fav.title)
                    } else {
                        onThreadClick(fav.id)
                    }
                },
                onDelete = { fav -> pendingRemove = fav },
            )
        }
    }

    pendingRemove?.let { target ->
        MtfConfirmDialog(
            title = "取消收藏",
            message = "确定不再收藏「${target.title}」吗？",
            confirmText = "取消收藏",
            onDismissRequest = { pendingRemove = null },
            onConfirm = {
                viewModel.removeFavorite(target)
                pendingRemove = null
            },
        )
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FavoritesTab(
    tab: FavoritesTabState,
    refreshing: Boolean,
    listState: LazyListState,
    onViewportAtTopChange: (Boolean) -> Unit,
    removingKey: String,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onClick: (FavoriteItem) -> Unit,
    onDelete: (FavoriteItem) -> Unit,
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
            val items = current.data
            if (items.isEmpty()) {
                EmptyBox("暂无收藏内容")
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
                        items(items, key = { if (it.favid.isNotEmpty()) it.favid else it.id }) { fav ->
                            FavoriteCard(
                                item = fav,
                                removing = removingKey == fav.favid.ifEmpty { fav.id },
                                onClick = { onClick(fav) },
                                onDelete = { onDelete(fav) },
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
private fun FavoriteCard(
    item: FavoriteItem,
    removing: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    Column(
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.type == "forum") colors.quoteBackground else MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    painter = painterResource(
                        if (item.type == "forum") R.drawable.ic_home else R.drawable.ic_favorite_outline,
                    ),
                    contentDescription = null,
                    tint = if (item.type == "forum") colors.iconSecondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Medium,
                    color = bodyTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!item.dateText.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.dateText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textHint,
                    )
                }
            }
            IconButton(onClick = onDelete, enabled = !removing) {
                if (removing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.textHint,
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = "取消收藏",
                        tint = colors.textHint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
