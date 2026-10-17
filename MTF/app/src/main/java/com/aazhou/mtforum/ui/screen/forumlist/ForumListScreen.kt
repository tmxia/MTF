package com.aazhou.mtforum.ui.screen.forumlist

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.Forum
import com.aazhou.mtforum.data.model.ForumGroup
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.ui.common.TabSecondaryActionIcons
import com.aazhou.mtforum.ui.common.TopBarActionsEndPadding
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.bottomBarContentPadding
import com.aazhou.mtforum.ui.common.onDoubleTapToTop
import com.aazhou.mtforum.ui.common.scrollToNewest
import com.aazhou.mtforum.ui.common.scrollToTop
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumListScreen(
    onForumClick: (fid: String, name: String) -> Unit,
    onSettings: () -> Unit = {},
    onMessages: () -> Unit = {},
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val viewModel: ForumListViewModel = appViewModel { ForumListViewModel(it.repository, it.contentCache) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val resetScrollTick by viewModel.resetScrollTick.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

var followedMap by remember { mutableStateOf(emptyMap<String, String>()) }
    
    
    LaunchedEffect(Unit) {
        followedMap = viewModel.fetchFollowedForumFids()
    }


    LaunchedEffect(Unit) { viewModel.refreshOnEnter() }
    

    val initialTick = remember { resetScrollTick }

    LaunchedEffect(resetScrollTick) {

        if (resetScrollTick > initialTick) {

            listState.scrollToNewest()

        }

    }

    LaunchedEffect(actionMessage) {
        val message = actionMessage ?: return@LaunchedEffect
        AppToast.show(message)
        viewModel.consumeActionMessage()
    }

    Scaffold(
        containerColor = colors.background,
        contentColor = bodyTextColor(),
        topBar = {
            ForumListTopBar(
                onSettings = onSettings,
                onMessages = onMessages,
                onDoubleTap = {
                    scope.launch { listState.scrollToTop() }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingBox(Modifier.padding(padding))
            is UiState.Error -> ErrorBox(current.message, { viewModel.load() }, Modifier.padding(padding))
            is UiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = bottomBarContentPadding() + 16.dp,
                            start = 6.dp,
                            end = 6.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(current.data, key = { it.name }) { group ->
                            ForumGroupCard(
                                group = group,
                                followedMap = followedMap,

                                onForumClick = onForumClick,
                                showNames = true,
                                onFollowToggle = { forum ->
                                    scope.launch {
                                        val fh = viewModel.postFormhash(forum.fid)
                                        if (fh.isNullOrBlank()) {
                                            AppToast.show("无法获取 formhash，请先登录")
                                            return@launch
                                        }
                                        val favid = followedMap[forum.fid]
                                        val ok = if (favid != null) {
                                            viewModel.unfollowForum(favid, fh).also {
                                                if (it) followedMap = followedMap - forum.fid
                                            }
                                        } else {
                                            viewModel.followForum(forum.fid, fh).also {
                                                if (it) {
                                                    followedMap = viewModel.fetchFollowedForumFids()
                                                }
                                            }
                                        }
                                        AppToast.show(
                                            when {
                                                ok && favid == null -> "已关注「${forum.name}」"
                                                ok && favid != null -> "已取消关注「${forum.name}」"
                                                else -> "操作失败，请重试"
                                            }
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ForumListTopBar(
    onSettings: () -> Unit,
    onMessages: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val colors = LocalMtfColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .statusBarsPadding()
            .height(MtfTopBarHeight)
            .onDoubleTapToTop(onDoubleTap)
            .padding(start = 16.dp, end = TopBarActionsEndPadding),
    ) {
        Text(
            text = "版块导航",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = bodyTextColor(),
        )

        Spacer(Modifier.weight(1f))

        TabSecondaryActionIcons(
            onSettings = onSettings,
            onMessages = onMessages,
        )
    }
}


@Composable
private fun ForumGroupCard(
    group: ForumGroup,
    followedMap: Map<String, String>,
    onForumClick: (fid: String, name: String) -> Unit,
    onFollowToggle: (Forum) -> Unit,
    showNames: Boolean = true,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .then(
                if (isDark) Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(16.dp))
                else Modifier
            ),
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
        ) {
            
            Box(
                modifier = Modifier
                    .size(width = 3.5.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = bodyTextColor(),
                modifier = Modifier.weight(1f),
            )
        }

        
        group.forums.forEachIndexed { index, forum ->
                    ForumItemRow(
                                        forum = forum,
                                        followed = followedMap.containsKey(forum.fid),
                                        todayPosts = if (group.name == "我关注的") forum.todayPosts else null,
                        onClick = { onForumClick(forum.fid, forum.name) },
                        onLongClick = { onFollowToggle(forum) },
                        showName = showNames,
                    )
            if (index < group.forums.size - 1) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colors.divider.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 66.dp, end = 14.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}


@Composable
private fun ForumItemRow(
    forum: Forum,
    followed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showName: Boolean = true,
    todayPosts: Int? = null,
) {
    val colors = LocalMtfColors.current
    val primary = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        
        ForumIconBadge(forum = forum)

        if (showName) {
            Spacer(Modifier.width(12.dp))

            Text(
                text = if (todayPosts != null) "今日: $todayPosts" else forum.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = colors.iconSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}


@Composable
private fun ForumIconBadge(forum: Forum) {
    val colors = LocalMtfColors.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(colors.quoteBackground),
    ) {
        if (!forum.icon.isNullOrBlank()) {
            AsyncImage(
                model = smallImageRequest(forum.icon),
                contentDescription = forum.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            
            val firstChar = forum.name.firstOrNull()?.toString() ?: "版"
            Text(
                text = firstChar,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = primaryColor,
            )
        }
    }
}
