package com.aazhou.mtforum.ui.screen.notice

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.Notice
import com.aazhou.mtforum.data.model.NoticeGroup
import com.aazhou.mtforum.data.model.PrivateMessage
import com.aazhou.mtforum.data.model.tabs
import com.aazhou.mtforum.data.repo.NoticeRepo
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.ui.common.SwipeTabIndicator
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.bottomBarContentPadding
import com.aazhou.mtforum.ui.common.scrollToNewest
import com.aazhou.mtforum.ui.common.reportViewportAtTop
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import kotlinx.coroutines.launch







@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeTabScreen(
    group: NoticeGroup,
    onBack: () -> Unit,
    onThreadClick: (tid: String) -> Unit,
    onUserClick: (String) -> Unit,
    onChatClick: ((uid: String, name: String) -> Unit)? = null,
) {
    val colors = LocalMtfColors.current
    val viewModel: NoticeTabViewModel = appViewModel {
        NoticeTabViewModel(
            group,
            it.repository,
            NoticeRepo(it.httpClient),
            it.contentCache,
            it.noticeUnreadStore,
        )
    }
    val tabStates by viewModel.tabStates.collectAsStateWithLifecycle()
    val tabs = remember(group) { group.tabs() }

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    
    val listStates = tabs.map { _k -> key(_k) { rememberLazyListState() } }
    val scope = rememberCoroutineScope()

    
    LaunchedEffect(pagerState.settledPage) {
        val tab = tabs[pagerState.settledPage]
        viewModel.selectTab(tab)
        viewModel.load(tab)
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
                    title = { Text(group.title) },
                    expandedHeight = MtfTopBarHeight,
                )
                val accent = accentTextColor()
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = colors.topBar,
                    contentColor = accent,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        SwipeTabIndicator(pagerState, tabPositions, accent)
                    },
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    tab.title,
                                    fontWeight = if (pagerState.currentPage == index) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    fontSize = 14.sp,
                                    color = if (pagerState.currentPage == index) {
                                        accent
                                    } else {
                                        colors.textSecondary
                                    },
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
            val tab = tabs[page]
            val tabState = tabStates[tab] ?: NoticeTabState()
            NoticeTabPage(
                tabState = tabState,
                emptyText = "暂无${tab.title}",
                    onViewportAtTopChange = { viewModel.noteViewportAtTop(tab, it) },
                listState = listStates[page],
                onRefresh = { viewModel.refresh(tab) },
                onLoadMore = { viewModel.loadMore(tab) },
                onRetry = { viewModel.load(tab) },
                onThreadClick = onThreadClick,
                onUserClick = onUserClick,
                onChatClick = onChatClick,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeTabPage(
    tabState: NoticeTabState,
    emptyText: String,
    listState: LazyListState,
    onViewportAtTopChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onThreadClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onChatClick: ((String, String) -> Unit)?,
) {
    when (val current = tabState.state) {
        is UiState.Loading -> LoadingBox(Modifier.fillMaxSize())
        is UiState.Error -> ErrorBox(current.message, onRetry, Modifier.fillMaxSize())
        is UiState.Success -> {
            val content = current.data
            
            
            
            listState.reportViewportAtTop(onViewportAtTopChange)

            


            val initialTick = remember { tabState.resetScrollTick }


            LaunchedEffect(tabState.resetScrollTick) {


                if (tabState.resetScrollTick > initialTick) {


                    listState.scrollToNewest()


                }


            }
            if (content.isEmpty()) {
                EmptyBox(emptyText, Modifier.fillMaxSize())
            } else {
                PullToRefreshBox(
                    isRefreshing = tabState.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CrossfadeContent(
                        content = content,
                        listState = listState,
                        hasNext = tabState.hasNext,
                        loadingMore = tabState.loadingMore,
                        onLoadMore = onLoadMore,
                        onThreadClick = onThreadClick,
                        onUserClick = onUserClick,
                        onChatClick = onChatClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CrossfadeContent(
    content: NoticeContentState,
    listState: LazyListState,
    hasNext: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onThreadClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onChatClick: ((String, String) -> Unit)?,
) {
    when (content) {
        is NoticeContentState.Notices -> NoticeListView(
            listState = listState,
            notices = content.items,
            onUserClick = onUserClick,
            onThreadClick = onThreadClick,
            hasNext = hasNext,
            loadingMore = loadingMore,
            onLoadMore = onLoadMore,
        )

        is NoticeContentState.PrivateMessages -> PrivateMessageListView(
            listState = listState,
            items = content.items,
            onUserClick = onUserClick,
            onChatClick = onChatClick,
        )
    }
}


@Composable
private fun NoticeListView(
    listState: LazyListState,
    notices: List<Notice>,
    onUserClick: (String) -> Unit,
    onThreadClick: (String) -> Unit,
    hasNext: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    
    val shouldLoadMore by remember(notices) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, hasNext) {
        if (shouldLoadMore && hasNext) onLoadMore()
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomBarContentPadding() + 12.dp),
    ) {
        items(notices, key = { it.id }) { notice ->
            NoticeRow(
                notice = notice,
                onUserClick = onUserClick,
                onClick = { notice.tid?.let { tid -> onThreadClick(tid) } },
            )
        }
        if (loadingMore) {
            item(key = "notice-loading-more") {
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


@Composable
private fun PrivateMessageListView(
    listState: LazyListState,
    items: List<PrivateMessage>,
    onUserClick: (String) -> Unit,
    onChatClick: ((String, String) -> Unit)? = null,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomBarContentPadding() + 12.dp),
    ) {
        items(items, key = { it.plid ?: it.touid ?: it.username }) { pm ->
            PrivateMessageCard(
                pm = pm,
                onUserClick = onUserClick,
                onChatClick = onChatClick,
            )
        }
    }
}


@Composable
private fun PrivateMessageCard(
    pm: PrivateMessage,
    onUserClick: (String) -> Unit,
    onChatClick: ((String, String) -> Unit)?,
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
            .clickable {
                val touid = pm.touid
                if (!touid.isNullOrBlank() && onChatClick != null) {
                    onChatClick(touid, pm.username)
                } else {
                    touid?.let(onUserClick)
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            AsyncImage(
                model = smallImageRequest(pm.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.quoteBackground)
                    .dimImageInDark(),
            )
            if (pm.unread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4D4F))
                        .border(1.5.dp, colors.surface, CircleShape)
                        .align(Alignment.TopEnd),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pm.username,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                if (pm.summary.isNotBlank()) {
                    Text(
                        text = pm.summary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (!pm.timeText.isNullOrBlank()) {
                    if (pm.summary.isNotBlank()) {
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = colors.textHint,
                        )
                    }
                    Text(
                        text = pm.timeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textHint,
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = colors.textHint,
            modifier = Modifier.size(16.dp),
        )
    }
}






@Composable
private fun NoticeRow(
    notice: Notice,
    onUserClick: (String) -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val clickable = notice.tid != null

    Row(
        verticalAlignment = Alignment.Top,
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
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        val avatarUrl = notice.avatarUrl
        if (avatarUrl != null) {
            AsyncImage(
                model = smallImageRequest(avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.quoteBackground)
                    .dimImageInDark()
                    .then(
                        notice.uid?.let { uid -> Modifier.clickable { onUserClick(uid) } }
                            ?: Modifier,
                    ),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color(0xFF142C20) else Color(0xFFE8F8F0),
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notice_system),
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notice.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!notice.timeText.isNullOrBlank()) {
                    Text(
                        text = notice.timeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textHint,
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
            }
        }

        if (clickable) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = colors.textHint,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(16.dp),
            )
        }
    }

}
