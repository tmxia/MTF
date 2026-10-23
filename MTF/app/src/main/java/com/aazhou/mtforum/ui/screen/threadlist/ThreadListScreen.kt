package com.aazhou.mtforum.ui.screen.threadlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.data.repo.ForumSort
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.ui.common.RankCard
import com.aazhou.mtforum.ui.common.RankCollapsedLimit
import com.aazhou.mtforum.ui.common.SwipeTabIndicator
import com.aazhou.mtforum.ui.common.ThreadCard
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.container
import com.aazhou.mtforum.ui.common.onDoubleTapToTop
import com.aazhou.mtforum.ui.common.scrollToNewest
import com.aazhou.mtforum.ui.common.reportViewportAtTop
import com.aazhou.mtforum.ui.common.scrollToTop
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.bodyTextColor
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    fid: String,
    title: String,
    onBack: () -> Unit,
    onThreadClick: (tid: String) -> Unit,
    onUserClick: (uid: String) -> Unit,
    onNewPost: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val viewModel: ThreadListViewModel = appViewModel { ThreadListViewModel(fid, it.repository, it.contentCache) }

    val _container = LocalContext.current.container()
    var blockTarget by remember { mutableStateOf<Thread?>(null) }
    val blockedUids by _container.blockedUsersStore.blockedUids.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { _container.blockedUsersStore.refresh() }
    val tabStates by viewModel.tabStates.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val openImage: (List<String>, Int) -> Unit = { urls, index ->
        if (urls.isNotEmpty()) preview = urls to index
    }
    val scope = rememberCoroutineScope()
    val sorts = ForumSort.entries
    val pagerState = rememberPagerState(initialPage = 0) { sorts.size }
    
    val listStates = sorts.map { _k -> key(_k) { rememberLazyListState() } }

    
    LaunchedEffect(actionMessage) {
        val message = actionMessage ?: return@LaunchedEffect
        AppToast.show(message)
        viewModel.consumeActionMessage()
    }

    
    LaunchedEffect(Unit) { viewModel.refreshOnEnter(sorts[pagerState.currentPage]) }

    Scaffold(
        containerColor = colors.background,
        contentColor = bodyTextColor(),
        topBar = {
            Column(Modifier.background(colors.topBar)) {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = "返回",
                                tint = bodyTextColor(),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNewPost) {
                            Icon(
                                painterResource(R.drawable.ic_new_thread),
                                contentDescription = "发帖",
                                tint = bodyTextColor(),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.topBar,
                        titleContentColor = bodyTextColor(),
                    ),
                    modifier = Modifier.onDoubleTapToTop {
                        scope.launch { listStates[pagerState.currentPage].scrollToTop() }
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
                    sorts.forEachIndexed { index, sort ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    sort.label,
                                    fontWeight = if (pagerState.currentPage == index) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    },
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
            val sort = sorts[page]
            
            LaunchedEffect(sort) { viewModel.load(sort) }
            val tab = tabStates[sort] ?: ForumTabState()
            ForumThreadList(
                tab = tab,
                listState = listStates[page],
                blockedUids = blockedUids,
                    onViewportAtTopChange = { viewModel.noteViewportAtTop(sort, it) },
                onRetry = { viewModel.retry(sort) },
                onRefresh = { viewModel.refresh(sort) },
                onLoadMore = { viewModel.loadMore(sort) },
                onThreadClick = onThreadClick,
                onUserClick = onUserClick,
                onImageClick = openImage,
                onLongClick = { blockTarget = it },
            )
        }
    }

    
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

    preview?.let { (urls, index) ->
        ImagePreview(urls = urls, initialIndex = index, onDismiss = { preview = null })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForumThreadList(
    tab: ForumTabState,
    listState: LazyListState,
    blockedUids: Set<String>,
    onViewportAtTopChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (tid: String) -> Unit,
    onUserClick: (uid: String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onLongClick: (Thread) -> Unit,
) {
    
    var announcementExpanded by remember { mutableStateOf(false) }
    
    
    
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
            val data = current.data
            if (data.threads.isEmpty()) {
                EmptyBox("本页没有主题")
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
                    isRefreshing = tab.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
        LazyColumn(
                        state = listState,
                        
                        contentPadding = PaddingValues(top = 2.5.dp, bottom = 24.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (data.announcements.isNotEmpty()) {
                            item(key = "announcements") {
                                RankCard(
                                    "公告",
                                    data.announcements,
                                    onThreadClick,
                                    collapsedLimit = RankCollapsedLimit,
                                    expanded = announcementExpanded,
                                    onToggleExpanded = { announcementExpanded = !announcementExpanded },
                                )
                            }
                        }
                        val visibleThreads = data.threads.filter { t ->
                    t.authorUid.isNullOrBlank() || t.authorUid !in blockedUids
                }
                items(visibleThreads, key = { it.tid }) { thread ->
                            
                            ThreadCard(
                                thread = thread,
                                onClick = { onThreadClick(thread.tid) },
                                onLongClick = { onLongClick(thread) },
                                onImageClick = onImageClick,
                                onUserClick = onUserClick,
                                showForumName = false,
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
