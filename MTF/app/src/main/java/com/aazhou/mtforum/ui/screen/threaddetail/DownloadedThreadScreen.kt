package com.aazhou.mtforum.ui.screen.threaddetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.collectImageUrls
import com.aazhou.mtforum.ui.common.container
import com.aazhou.mtforum.ui.common.openForumLink
import com.aazhou.mtforum.ui.common.scrollToTop
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext







@Composable
fun DownloadedThreadScreen(
    tid: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onThreadClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalMtfColors.current
    val siteHost = context.container().httpClient.host
    val viewModel: OfflineThreadViewModel = appViewModel {
        OfflineThreadViewModel(tid, it.archiveStore)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val waterPidsState by viewModel.waterPids.collectAsStateWithLifecycle()
    val foldExpanded by viewModel.foldExpanded.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val openImage: (List<String>, Int) -> Unit = { urls, index ->
        if (urls.isNotEmpty()) preview = urls to index
    }

    Scaffold(
        containerColor = colors.topBar,
        contentColor = bodyTextColor(),
        topBar = {
            val page = (state as? UiState.Success)?.data
            val op = page?.posts?.firstOrNull { it.isFirstPost }
                ?: page?.posts?.firstOrNull()
            ThreadDetailTopBar(
                post = op,
                following = false,
                followEnabled = false,
                showMore = false,
                downloadBusy = false,
                onBack = onBack,
                onFollow = {},
                onReport = {},
                onReward = {},
                onDownload = {},
                onUserClick = onUserClick,
                onDoubleTap = { scope.launch { listState.scrollToTop() } },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            when (val current = state) {
                is UiState.Loading -> LoadingBox(Modifier.fillMaxSize())
                is UiState.Error -> ErrorBox(current.message, onBack, Modifier.fillMaxSize())
                is UiState.Success -> {
                    val data = current.data
                    if (data.posts.isEmpty()) {
                        EmptyBox("没有内容", Modifier.fillMaxSize())
                    } else {
                        val op = data.posts.firstOrNull { it.isFirstPost }
                            ?: data.posts.firstOrNull()
                        val pageImages by produceState(
                            initialValue = emptyList<String>(),
                            data.posts,
                        ) {
                            value = withContext(Dispatchers.Default) {
                                data.posts.flatMap { collectImageUrls(it.contentHtml) }
                            }
                        }
                        val waterPids = waterPidsState
                        val visibleReplies = remember(data.posts, waterPids, foldExpanded) {
                            val replies = data.posts.filter { it !== op }
                            if (foldExpanded) replies else replies.filter { it.pid !in waterPids }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(
                                bottom = 24.dp +
                                    WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding(),
                            ),
                        ) {
                            if (op != null) {
                                item(key = "header") {
                                    DetailHeader(
                                        post = op,
                                        subject = data.subject,
                                        forumName = data.forumName,
                                        onImageClick = openImage,
                                        pageImages = pageImages,
                                        onUnlock = {},
                                        onLinkClick = { url ->
                                            openForumLink(context, url, siteHost, onThreadClick, onUserClick)
                                        },
                                    )
                                }
                            }
                            item(key = "comment-header") {
                                CommentHeader(
                                    total = data.totalReplies,
                                    folded = waterPids.size,
                                    foldExpanded = foldExpanded,
                                    opOnly = false,
                                    orderType = 0,
                                    sortable = false,
                                    onToggleFold = { viewModel.toggleFoldExpanded() },
                                    onOpOnly = {},
                                    onOrder = {},
                                )
                                HorizontalDivider(color = colors.divider)
                            }
                            items(visibleReplies, key = { it.pid }) { reply ->
                                ReplyBlock(
                                    reply = reply,
                                    onReply = {},
                                    onImageClick = openImage,
                                    onVideoClick = null,
                                    onUserClick = onUserClick,
                                    pageImages = pageImages,
                                    onLongPress = {},
                                    onThreadClick = onThreadClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    preview?.let { (urls, index) ->
        ImagePreview(urls = urls, initialIndex = index, onDismiss = { preview = null })
    }
}
