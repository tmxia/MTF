package com.aazhou.mtforum.ui.screen.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.RankCard
import com.aazhou.mtforum.ui.common.RankCollapsedLimit
import com.aazhou.mtforum.ui.common.ThreadCard
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.accountIconModel
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.bottomBarContentPadding
import com.aazhou.mtforum.ui.common.container
import com.aazhou.mtforum.ui.common.scrollToNewest
import com.aazhou.mtforum.ui.common.reportViewportAtTop
import com.aazhou.mtforum.ui.common.scrollToTop
import com.aazhou.mtforum.ui.common.TopBarActionIcons
import com.aazhou.mtforum.ui.common.TopBarActionsEndPadding
import com.aazhou.mtforum.ui.common.TopBarVerticalPadding
import com.aazhou.mtforum.ui.common.onDoubleTapToTop
import com.aazhou.mtforum.ui.screen.login.LoginDialog
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter






object HomeTabs {
    const val NEWEST = "newthread"
    const val HOT = "hot"
    const val DIGEST = "digest"
    const val REPLY = "new"
    const val SOFA = "sofa"

    val views = listOf(NEWEST, HOT, DIGEST, REPLY, SOFA)
    val labels = listOf("最新", "热门", "精华", "回复", "抢沙发")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onToggleTheme: () -> Unit,
    onSettings: () -> Unit,
    onMessages: () -> Unit,
    onForumClick: (fid: String, name: String) -> Unit,
    onThreadClick: (tid: String) -> Unit,
    onUserClick: (uid: String) -> Unit,
    onProfileClick: (uid: String) -> Unit,
    onLogin: () -> Unit,
    
    scrollToTopTick: Int,
) {
    val colors = LocalMtfColors.current
    val container = LocalContext.current.container()
    val viewModel: HomeViewModel = appViewModel { HomeViewModel(it.repository, it.contentCache) }

    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val hot by viewModel.hot.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    
    val me by container.currentUser.collectAsStateWithLifecycle()

    
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val openImage: (List<String>, Int) -> Unit = { urls, index ->
        if (urls.isNotEmpty()) preview = urls to index
    }
    val scope = rememberCoroutineScope()
    var showLoginDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = 0) { HomeTabs.views.size }
    
    
    
    
    val listStates = HomeTabs.views.map { _k ->
        key(_k) {
            rememberSaveable(saver = LazyListState.Saver) {
                LazyListState()
            }
        }
    }

    var handledScrollTopTick by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > handledScrollTopTick) {
            handledScrollTopTick = scrollToTopTick
            listStates[pagerState.currentPage].scrollToTop()
        }
    }


    LaunchedEffect(Unit) { viewModel.refreshOnEnter(HomeTabs.views[pagerState.currentPage]) }

    
    LaunchedEffect(actionMessage) {
        val message = actionMessage ?: return@LaunchedEffect
        AppToast.show(message)
        viewModel.consumeActionMessage()
    }

    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 200.dp),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        androidx.compose.material3.Text(
            text = "IdxDebug p=${pagerState.currentPage} i=${listStates[pagerState.currentPage].firstVisibleItemIndex}",
            color = androidx.compose.ui.graphics.Color.Red,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
    }

    Scaffold(
        containerColor = colors.background,
        
        contentColor = bodyTextColor(),
        topBar = {
            HomeTopBar(
                avatarUrl = me?.avatarUrl,
                onProfileClick = { me?.uid?.let(onProfileClick) },
                onLogin = { showLoginDialog = true },
                onSearch = onSearch,
                onToggleTheme = onToggleTheme,
                onSettings = onSettings,
                onMessages = onMessages,
                
                onDoubleTap = {
                    scope.launch { listStates[pagerState.currentPage].scrollToTop() }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HomeTabRow(
                selectedIndex = pagerState.currentPage,
                scrollFraction = pagerState.currentPageOffsetFraction,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                
                beyondViewportPageCount = 2,
            ) { page ->
                val view = HomeTabs.views[page]
                
                LaunchedEffect(view) { viewModel.load(view) }
                val feed = feeds[view] ?: FeedState()
                FeedPage(
                    feed = feed,
                    refreshing = view in refreshing,
                    listState = listStates[page],
                    rankThreads = if (view == HomeTabs.HOT) hot?.hotThreads.orEmpty() else emptyList(),
                    onViewportAtTopChange = { viewModel.noteViewportAtTop(view, it) },
                    onRefresh = { viewModel.refresh(view) },
                    onRetry = { viewModel.retry(view) },
                    onLoadMore = { viewModel.loadMore(view) },
                    onThreadClick = onThreadClick,
                    onUserClick = onUserClick,
                    onImageClick = openImage,
                )
            }
        }
    }

    preview?.let { (urls, index) ->
        ImagePreview(urls = urls, initialIndex = index, onDismiss = { preview = null })
    }

    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onSuccess = {
                showLoginDialog = false
                container.refreshCurrentUserAsync()
                HomeTabs.views.forEach { viewModel.refresh(it) }
            },
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPage(
    feed: FeedState,
    refreshing: Boolean,
    listState: LazyListState,
    rankThreads: List<Thread>,
    onViewportAtTopChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (tid: String) -> Unit,
    onUserClick: (uid: String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    
    val _container = LocalContext.current.container()
    val scope = rememberCoroutineScope()
    var blockTarget by remember { mutableStateOf<Thread?>(null) }
    val blockedUids by _container.blockedUsersStore.blockedUids.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { _container.blockedUsersStore.refresh() }

    
    blockTarget?.let { target ->
        com.aazhou.mtforum.ui.common.BlockUserDialog(
            targetName = target.authorName ?: "该用户",
            isBlock = true,
            onDismiss = { blockTarget = null },
            onConfirm = {
                val t2 = target
                blockTarget = null
                scope.launch {
                                        val uid = t2.authorUid
                    uid?.let { _container.blockedUsersStore.add(it) }

                    val res = _container.repository.addUserBlacklist(t2.authorName ?: "", "")
                    res.onSuccess { msg ->
                        
                        AppToast.show(msg)
                    }.onFailure { e ->
                        t2.authorUid?.let { _container.blockedUsersStore.remove(it) }
                        AppToast.show(e.message ?: "拉黑失败")
                    }
                }
            },
        )
    }

    var rankExpanded by remember { mutableStateOf(false) }

    
    
    listState.reportViewportAtTop(onViewportAtTopChange)

    
    
    
    val initialTick = remember { feed.resetScrollTick }
    LaunchedEffect(feed.resetScrollTick) {
        if (feed.resetScrollTick > initialTick) {
            listState.scrollToNewest()
        }
    }
    
    var rankWasMissing by remember { mutableStateOf(rankThreads.isEmpty()) }
    LaunchedEffect(rankThreads.isNotEmpty()) {
        if (rankThreads.isNotEmpty() && rankWasMissing) listState.scrollToNewest()
        rankWasMissing = rankThreads.isEmpty()
    }
    when (val current = feed.state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(current.message, onRetry)
        is UiState.Success -> {
            
            val shouldLoadMore by remember {
                derivedStateOf {
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = listState.layoutInfo.totalItemsCount
                    total > 0 && last >= total - 3
                }
            }
            LaunchedEffect(shouldLoadMore, feed.hasNext) {
                if (shouldLoadMore && feed.hasNext) onLoadMore()
            }
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomBarContentPadding()),
                ) {
                    if (rankThreads.isNotEmpty()) {
                        item(key = "hot-rank") {
                            RankCard(
                                "热帖排行",
                                rankThreads,
                                onThreadClick,
                                collapsedLimit = RankCollapsedLimit,
                                expanded = rankExpanded,
                                onToggleExpanded = { rankExpanded = !rankExpanded },
                            )
                        }
                    }
                    val visibleThreads = current.data.filter { t ->
                        t.authorUid.isNullOrBlank() || t.authorUid !in blockedUids
                    }
                    items(visibleThreads.distinctBy { it.tid }, key = { it.tid }) { thread ->
                        ThreadCard(
                            thread = thread,
                            onClick = { onThreadClick(thread.tid) },
                            onLongClick = { blockTarget = thread },
                            onImageClick = onImageClick,
                            onUserClick = onUserClick,
                        )
                    }
                    if (feed.loadingMore) {
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








@Composable
private fun HomeTabRow(selectedIndex: Int, scrollFraction: Float, onSelect: (Int) -> Unit) {
    val colors = LocalMtfColors.current
    val accent = accentTextColor()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .padding(horizontal = 8.dp),
    ) {
        val tabWidth = maxWidth / HomeTabs.labels.size
        val indicatorWidth = 18.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTabs.labels.forEachIndexed { index, label ->
                val active = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(top = 8.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        color = if (active) accent else colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
        
        val indicatorOffset = tabWidth * (selectedIndex + scrollFraction) +
            (tabWidth - indicatorWidth) / 2
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 2.5.dp)
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(indicatorWidth)
                .height(2.dp)
                .background(accent, RoundedCornerShape(1.dp)),
        )
    }
}


@Composable
private fun HomeTopBar(
    avatarUrl: String?,
    onProfileClick: () -> Unit,
    onLogin: () -> Unit,
    onSearch: () -> Unit,
    onToggleTheme: () -> Unit,
    onSettings: () -> Unit,
    onMessages: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val colors = LocalMtfColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .statusBarsPadding()
            
            .onDoubleTapToTop { onDoubleTap() }
            .padding(
                start = 16.dp,
                end = TopBarActionsEndPadding,
                top = TopBarVerticalPadding,
                bottom = TopBarVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarUrl.isNullOrBlank()) {
            
            
            AsyncImage(
                model = accountIconModel(logout = false),
                contentDescription = "登录",
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onLogin),
            )
        } else {
            AsyncImage(
                model = smallImageRequest(avatarUrl),
                contentDescription = "个人主页",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.quoteBackground)
                    .dimImageInDark()
                    .clickable(onClick = onProfileClick),
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.quoteBackground)
                .clickable(onClick = onSearch)
                .padding(horizontal = 12.dp),
        ) {
            Icon(
                painterResource(com.aazhou.mtforum.R.drawable.ic_search_new),
                contentDescription = null,
                tint = colors.textHint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "搜索帖子、用户...",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = colors.textHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        TopBarActionIcons(
            onToggleTheme = onToggleTheme,
            onSettings = onSettings,
            onMessages = onMessages,
        )
    }
}
