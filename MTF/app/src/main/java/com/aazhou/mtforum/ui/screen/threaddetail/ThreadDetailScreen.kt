package com.aazhou.mtforum.ui.screen.threaddetail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import com.aazhou.mtforum.ui.common.MtfConfirmDialog
import com.aazhou.mtforum.ui.common.MtfContentDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.parser.SmileyPack
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.BottomBarBottomMargin
import com.aazhou.mtforum.ui.common.ReplyBarCapsuleHeight
import com.aazhou.mtforum.ui.common.ReplyBarTopPadding
import com.aazhou.mtforum.ui.common.replyBarContentPadding
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.image.ImageAspectCache
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.PostContent
import com.aazhou.mtforum.ui.common.saveAttachment
import com.aazhou.mtforum.ui.common.saveThreadDownload
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.VideoPlayerScreen
import com.aazhou.mtforum.ui.common.scrollToTop
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.collectImageUrls
import com.aazhou.mtforum.ui.common.container
import com.aazhou.mtforum.ui.common.openForumLink
import com.aazhou.mtforum.ui.common.plainTextOf
import com.aazhou.mtforum.ui.common.onDoubleTapToTop
import com.aazhou.mtforum.ui.common.TopBarVerticalPadding
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


private const val EDIT_WINDOW_MS = 10_080L * 60_000L


private val RE_FLOOR_ABS_TIME =
    Regex("""(\d{4})-(\d{1,2})-(\d{1,2})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?""")







private fun isEditExpired(timeText: String?): Boolean {
    val raw = timeText?.trim().orEmpty()
    if (raw.isEmpty()) return false
    val m = RE_FLOOR_ABS_TIME.find(raw) ?: return false
    val calendar = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, m.groupValues[1].toIntOrNull() ?: return false)
        set(Calendar.MONTH, (m.groupValues[2].toIntOrNull() ?: return false) - 1)
        set(Calendar.DAY_OF_MONTH, m.groupValues[3].toIntOrNull() ?: return false)
        set(Calendar.HOUR_OF_DAY, m.groupValues[4].toIntOrNull() ?: 0)
        set(Calendar.MINUTE, m.groupValues[5].toIntOrNull() ?: 0)
        set(Calendar.SECOND, m.groupValues[6].toIntOrNull() ?: 0)
    }
    return System.currentTimeMillis() - calendar.timeInMillis > EDIT_WINDOW_MS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThreadDetailScreen(
    tid: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onThreadClick: (String) -> Unit,
    
    onEditThread: (String, String, String, String) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val colors = LocalMtfColors.current
    val container = context.container()
    val viewModel: ThreadDetailViewModel = appViewModel {
        ThreadDetailViewModel(
            tid,
            it.repository,
            it.actionStore,
                it.historyStore,
            it.sessionStore.uid,
            it.blockedUsersStore,
        )
    }
    val myUid by viewModel.myUid.collectAsStateWithLifecycle()
    val blockedUids by viewModel.blockedUids.collectAsStateWithLifecycle()
    
    
    
    val currentUser by container.currentUser.collectAsStateWithLifecycle()
    val loggedIn = currentUser?.isLoggedIn == true
    val state by viewModel.state.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    val replyState by viewModel.replyState.collectAsStateWithLifecycle()
    val orderType by viewModel.orderType.collectAsStateWithLifecycle()
    val opOnly by viewModel.opOnly.collectAsStateWithLifecycle()
    val replyTarget by viewModel.replyTarget.collectAsStateWithLifecycle()
    val followState by viewModel.followState.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val likeCount by viewModel.likeCount.collectAsStateWithLifecycle()
    val favorited by viewModel.favorited.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val loadMoreState by viewModel.loadMoreState.collectAsStateWithLifecycle()
    val waterPidsState by viewModel.waterPids.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val likeUsers by viewModel.likeUsers.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val foldExpanded by viewModel.foldExpanded.collectAsStateWithLifecycle()

    var composerVisible by remember { mutableStateOf(false) }
    
    val editTarget by viewModel.editTarget.collectAsStateWithLifecycle()
    val editForm by viewModel.editForm.collectAsStateWithLifecycle()
    
    val editAttachments by viewModel.editAttachments.collectAsStateWithLifecycle()
    
    var editText by remember { mutableStateOf(TextFieldValue("")) }
    
    val recentCodes by container.smileyStore.recent.collectAsStateWithLifecycle(initialValue = emptyList())
    var smileyPacks by remember { mutableStateOf<List<SmileyPack>>(emptyList()) }
    
    
    
    val drafts = remember { mutableStateMapOf<String?, TextFieldValue>() }
    
    
    var sendingDraftKey by remember { mutableStateOf<String?>(null) }
    
    
    
    
    
    var keyboardShown by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    val imeDensity = LocalDensity.current
    val imeInsets = WindowInsets.ime
    
    
    LaunchedEffect(composerVisible) {
        if (!composerVisible) {
            keyboardShown = false
            return@LaunchedEffect
        }
        var lastPx = -1
        var hiding = false
        while (true) {
            withFrameNanos { }
            val px = imeInsets.getBottom(imeDensity)
            when {
                px <= 0 -> {
                    hiding = false
                    keyboardShown = false
                }
                hiding -> {
                    
                    if (px > lastPx) hiding = false else keyboardShown = false
                }
                lastPx > 0 && px < lastPx -> {
                    hiding = true
                    keyboardShown = false
                    focusManager.clearFocus()
                }
                else -> keyboardShown = true
            }
            lastPx = px
        }
    }
    
    
    val dismissComposer: () -> Unit = {
        keyboardShown = false
        focusManager.clearFocus()
        keyboardController?.hide()
        composerVisible = false
        
        if (editTarget != null) viewModel.cancelEdit()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    
    LaunchedEffect(composerVisible) {
        if (composerVisible && smileyPacks.isEmpty()) {
            smileyPacks = runCatching { container.repository.smileyPacks() }.getOrDefault(emptyList())
        }
    }

    
    
    val pendingScrollAnchor by viewModel.pendingScrollAnchor.collectAsStateWithLifecycle()
    val anchorNow: () -> ScrollAnchor = {
        ScrollAnchor(
            index = listState.firstVisibleItemIndex,
            offset = listState.firstVisibleItemScrollOffset,
        )
    }
    LaunchedEffect(replyState) {
        when (val r = replyState) {
            is ThreadDetailViewModel.ReplyState.Success -> {
                sendingDraftKey?.let { drafts.remove(it) }
                sendingDraftKey = null
                dismissComposer()
                AppToast.show("回复成功")
                viewModel.consumeReplyState()
                
                viewModel.refreshAfterReply()
            }
            is ThreadDetailViewModel.ReplyState.Error -> {
                AppToast.show(r.message)
                viewModel.consumeReplyState()
            }
            else -> Unit
        }
    }
    
    LaunchedEffect(editForm) {
        when (val f = editForm) {
            is ThreadDetailViewModel.EditFormState.Ready -> {
                
                editText = TextFieldValue(f.text, selection = TextRange(f.text.length))
                
                viewModel.consumeEditForm()
                keyboardShown = false
                composerVisible = true
            }
            is ThreadDetailViewModel.EditFormState.Error -> {
                AppToast.show(f.message)
                viewModel.consumeEditForm()
            }
            else -> Unit
        }
    }
    
    LaunchedEffect(editTarget) {
        if (editTarget == null && composerVisible) dismissComposer()
    }
    
    
    
    var threadEdited by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (threadEdited) {
            threadEdited = false
            viewModel.refreshFirstPage()
        }
    }
    LaunchedEffect(followState) {
        when (val f = followState) {
            
            is ThreadDetailViewModel.FollowState.Done ->
                if (f.fromUser) AppToast.show(if (f.following) "已关注" else "已取消关注")
            is ThreadDetailViewModel.FollowState.Error -> AppToast.show(f.message)
            else -> Unit
        }
    }
    LaunchedEffect(actionMessage) {
        val message = actionMessage ?: return@LaunchedEffect
        AppToast.show(message)
        viewModel.consumeActionMessage()
    }

    LaunchedEffect(pendingScrollAnchor) {
        val anchor = pendingScrollAnchor ?: return@LaunchedEffect
        
        withFrameNanos { }
        val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
        listState.scrollToItem(anchor.index.coerceAtMost(lastIndex), anchor.offset)
        viewModel.consumePendingScrollAnchor()
    }

    val page = (state as? UiState.Success)?.data

    
    val uploadCredentials = remember(page) {
        val uid = page?.uploadUid
        val hash = page?.uploadHash
        if (uid != null && hash != null) uid to hash else null
    }

    
    
    var atUsers by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(composerVisible) {
        if (composerVisible && atUsers == null) {
            atUsers = runCatching { container.repository.fetchAtUsers() }.getOrDefault(emptyList())
            atUsers?.let { viewModel.noteAtUsers(it) }
        }
    }

    
    
    
    BackHandler(enabled = composerVisible) {
        if (keyboardShown) {
            keyboardShown = false
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            dismissComposer()
        }
    }
    val following = (followState as? ThreadDetailViewModel.FollowState.Done)?.following ?: false
    
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val openImage: (List<String>, Int) -> Unit = { urls, index ->
        if (urls.isNotEmpty()) preview = urls to index
    }
    
    var videoUrl by remember { mutableStateOf<String?>(null) }
    val openVideo: (String) -> Unit = { url -> videoUrl = url }
    
    
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            hasNext && loadMoreState is ThreadDetailViewModel.LoadMoreState.Idle &&
                !loadingMore && total > 0 && last >= total - 6
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String } }
            .distinctUntilChanged()
            .collect { viewModel.noteVisiblePids(it.toSet()) }
    }

    val todo: (String) -> Unit = { msg -> AppToast.show(msg) }
    
    val downloadAttachment: (String, String) -> Unit = { fileName, url ->
        scope.launch {
            val saved = withContext(Dispatchers.IO) { saveAttachment(context, fileName, url) != null }
            AppToast.show(if (saved) "已下载：$fileName" else "下载失败，请稍后再试")
        }
    }
    
    val requireLogin: (String) -> Boolean = { hint ->
        if (loggedIn) {
            true
        } else {
            AppToast.show(hint, LOGIN_HINT_DURATION_MS)
            false
        }
    }
    
    var actionTarget by remember { mutableStateOf<Post?>(null) }
    
    var atPickerVisible by remember { mutableStateOf(false) }
    
    var composerFocusRequest by remember { mutableIntStateOf(0) }
    
    var rewardOpen by remember { mutableStateOf(false) }
    
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    
    var deleteTarget by remember { mutableStateOf<Post?>(null) }
    
    val reportThread: () -> Unit = { if (requireLogin(LOGIN_HINT_REPORT)) reportTarget = ReportTarget(null) }
    val rewardThread: () -> Unit = { if (requireLogin(LOGIN_HINT_REWARD)) rewardOpen = true }
    
    
    var downloadBusy by remember { mutableStateOf(false) }
    val onDownload: () -> Unit = {
        
        val frame = viewModel.snapshotForDownload()
        if (frame == null) {
            AppToast.show("帖子还没加载出来，稍后再试")
        } else {
            scope.launch {
                downloadBusy = true
                val entry = try {
                    saveThreadDownload(
                        context = context,
                        page = frame,
                        downloadStore = container.downloadStore,
                        archiveStore = container.archiveStore,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                downloadBusy = false
                val savedReplies = frame.posts.count { !it.isFirstPost }
                AppToast.show(
                    when {
                        entry == null -> "保存失败，请稍后重试"
                        savedReplies == 0 -> "已保存正文到 下载/MTF（暂无评论）"
                        else -> "已保存到 下载/MTF（含已加载的 $savedReplies 楼评论）"
                    },
                )
            }
        }
    }

    
    
    
    
    
    val replyBarTopBar = rememberUpdatedState(colors.topBar)
    val replyBarBackdrop = rememberLayerBackdrop {
        drawRect(replyBarTopBar.value)
        drawContent()
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            
            containerColor = colors.topBar,
            
            
            
            
            contentColor = bodyTextColor(),
            topBar = {
                
                
                val op = page?.posts?.firstOrNull { it.isFirstPost }
                    ?: page?.posts?.firstOrNull()
                ThreadDetailTopBar(
                    post = op,
                    following = following,
                    
                    
                    followEnabled = page?.opUid != null,
                    
                    
                    canEditThread = myUid.isNotBlank() && myUid == op?.authorUid &&
                        !isEditExpired(op?.timeText),
                    showMore = true,
                    downloadBusy = downloadBusy,
                    onBack = onBack,
                    onFollow = { if (requireLogin(LOGIN_HINT_FOLLOW)) viewModel.toggleFollow() },
                    onReport = reportThread,
                    onReward = rewardThread,
                    onDownload = onDownload,
                    onEditThread = {
                        val fid = page?.fid
                        if (op != null && !fid.isNullOrBlank()) {
                            threadEdited = true
                            onEditThread(tid, op.pid, fid, page?.forumName.orEmpty())
                        } else {
                            AppToast.show("帖子信息还没加载好，稍后再试")
                        }
                    },
                    onUserClick = onUserClick,
                    onDoubleTap = { scope.launch { listState.scrollToTop() } },
                )
            },
            bottomBar = {
                
                if (page != null && !composerVisible) {
                    ReplyBar(
                        backdrop = replyBarBackdrop,
                        replyCount = page.totalReplies ?: 0,
                        liked = liked,
                        likeCount = likeCount,
                        favorited = favorited,
                        favoriteCount = favoriteCount,
                        onClick = {
                            if (requireLogin(LOGIN_HINT_REPLY)) {
                                viewModel.setReplyTarget(null)
                                composerVisible = true
                            }
                        },
                        onComments = {
                            if (requireLogin(LOGIN_HINT_REPLY)) scope.launch { listState.animateScrollToItem(1) }
                        },
                        onLike = { if (requireLogin(LOGIN_HINT_LIKE)) viewModel.toggleLike() },
                        onLikeLongPress = { viewModel.loadLikeUsers() },
                        onFavorite = { if (requireLogin(LOGIN_HINT_FAVORITE)) viewModel.toggleFavorite() },
                        onShare = { shareThread(context, page.subject, tid) },
                    )
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    
                    
                    
            ) {
            when (val current = state) {
                is UiState.Loading -> LoadingBox(Modifier.fillMaxSize())
                is UiState.Error -> ErrorBox(current.message, { viewModel.load() }, Modifier.fillMaxSize())
                is UiState.Success -> {
                    val data = current.data
                    if (data.posts.isEmpty()) {
                        EmptyBox("没有内容", Modifier.fillMaxSize())
                    } else {
                        val op = data.posts.firstOrNull { it.isFirstPost } ?: data.posts.firstOrNull()
                        
                        
                        
                        val pageImages by produceState(initialValue = emptyList<String>(), data.posts) {
                            val urls = withContext(Dispatchers.Default) {
                                data.posts.flatMap { collectImageUrls(it.contentHtml) }
                            }
                            value = urls
                            
                            
                            ImageAspectCache.prefetch(context, context.imageLoader, urls)
                        }
                        
                        val waterPids = waterPidsState
                        val foldedCount = waterPids.size
                        
                        val visibleReplies = remember(data.posts, waterPids, foldExpanded, blockedUids) {
                            val replies = data.posts.filter { p ->
                                p !== op && (p.authorUid.isNullOrBlank() || p.authorUid !in blockedUids)
                            }
                            if (foldExpanded) replies else replies.filter { it.pid !in waterPids }
                        }
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            
                            
                            
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = padding.calculateTopPadding()),
                        ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(
                                bottom = replyBarContentPadding(),
                            ),
                        ) {
                            if (op != null) {
                                item(key = "header") {
                                    DetailHeader(
                                        post = op,
                                        subject = data.subject,
                                        forumName = data.forumName,
                                        onImageClick = openImage,
                                        onVideoClick = openVideo,
                                        pageImages = pageImages,
                                        onUnlock = {
                                            
                                            
                                            if (requireLogin(LOGIN_HINT_REPLY)) {
                                                viewModel.setReplyTarget(null)
                                                composerVisible = true
                                            }
                                        },
                                        onLinkClick = { url ->
                                            openForumLink(
                                                context,
                                                url,
                                                container.httpClient.host,
                                                onThreadClick,
                                                onUserClick,
                                            )
                                        },
                                        onAttachmentClick = downloadAttachment,
                                    )
                                }
                            }
                            
                            
                            
                            stickyHeader(key = "comment-header") {
                                Column(Modifier.fillMaxWidth().background(colors.topBar)) {
                                    CommentHeader(
                                        total = data.totalReplies,
                                        folded = foldedCount,
                                        foldExpanded = foldExpanded,
                                        opOnly = opOnly,
                                        orderType = orderType,
                                        onToggleFold = { viewModel.toggleFoldExpanded() },
                                        onOpOnly = {
                                            viewModel.setReplyFilter(true, orderType, anchorNow())
                                        },
                                        onOrder = {
                                            
                                            
                                            
                                            
                                            viewModel.setReplyFilter(
                                                opOnly = false,
                                                orderType = it,
                                                anchor = ScrollAnchor(
                                                    index = if (op != null) 1 else 0,
                                                    offset = 0,
                                                ),
                                            )
                                        },
                                    )
                                    HorizontalDivider(color = colors.divider)
                                }
                            }
                            if (visibleReplies.isEmpty()) {
                                item(key = "no-comment") {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("还没有评论", color = colors.textHint, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            items(visibleReplies, key = { it.pid }) { reply ->
                                ReplyBlock(
                                    reply = reply,
                                    onReply = {
                                        if (requireLogin(LOGIN_HINT_REPLY)) {
                                            viewModel.setReplyTarget(reply)
                                            composerVisible = true
                                        }
                                    },
                                    onImageClick = openImage,
                                    onVideoClick = openVideo,
                                    onUserClick = onUserClick,
                                    pageImages = pageImages,
                                    onLongPress = { actionTarget = it },
                                    onThreadClick = onThreadClick,
                                    onAttachmentClick = downloadAttachment,
                                )
                            }
                            item(key = "pager") {
                                when (loadMoreState) {
                                    is ThreadDetailViewModel.LoadMoreState.Loading -> Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(Modifier.size(22.dp))
                                    }
                                    is ThreadDetailViewModel.LoadMoreState.End -> Box(
                                        Modifier.fillMaxWidth().padding(20.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "已经到底了",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = colors.textHint,
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
        
        
        
        
        AnimatedVisibility(
            visible = composerVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = ScrimFadeMillis)),
            exit = fadeOut(animationSpec = tween(durationMillis = ScrimFadeMillis)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
                    
                    
                    
                    
                    
                    .pointerInput(Unit) {
                        detectTapGestures { dismissComposer() }
                    },
            )
        }
        
        
        
        
        AnimatedVisibility(
            visible = composerVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = ComposerSlideMillis, easing = FastOutSlowInEasing),
            ) { it } + fadeIn(animationSpec = tween(durationMillis = ComposerSlideMillis)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = ComposerSlideMillis, easing = FastOutSlowInEasing),
            ) { it } + fadeOut(animationSpec = tween(durationMillis = ComposerSlideMillis)),
        ) {
            val editingPost = editTarget
            ReplyComposer(
                value = if (editingPost != null) editText else drafts[replyTarget?.pid] ?: TextFieldValue(""),
                onValueChange = { next ->
                    if (editingPost != null) editText = next else drafts[replyTarget?.pid] = next
                },
                target = if (editingPost != null) null else replyTarget,
                editing = editingPost != null,
                sending = replyState is ThreadDetailViewModel.ReplyState.Sending ||
                    editForm is ThreadDetailViewModel.EditFormState.Loading,
                smileyPacks = smileyPacks,
                recentCodes = recentCodes,
                onSmileyUsed = { code -> scope.launch { container.smileyStore.addRecent(code) } },
                uploadCredentials = uploadCredentials,
                existingAttachments = editAttachments,
                onUploadImage = { file ->
                    
                    val credentials = uploadCredentials ?: viewModel.ensureUploadCredentials()
                    if (credentials == null) {
                        AppToast.show("上传凭据获取失败，请返回帖子重进试试")
                        null
                    } else {
                        container.repository.uploadReplyImage(file, credentials.first, credentials.second)
                    }
                },
                atUsers = atUsers.orEmpty(),
                onOpenAtPicker = { atPickerVisible = true },
                focusRequest = composerFocusRequest,
                onSend = {
                    val current = editTarget
                    if (current != null) {
                        if (editText.text.isNotBlank()) viewModel.submitEdit(editText.text)
                    } else {
                        val draft = drafts[replyTarget?.pid]
                        if (requireLogin(LOGIN_HINT_REPLY) && draft != null && draft.text.isNotBlank()) {
                            sendingDraftKey = replyTarget?.pid
                            viewModel.reply(draft.text)
                        }
                    }
                },
                onDismiss = dismissComposer,
            )
        }
    }

    
    BackHandler(enabled = atPickerVisible) { atPickerVisible = false }
    if (atPickerVisible) {
        AtUserPickerPage(
            onDismiss = {
                atPickerVisible = false
                
                composerFocusRequest++
            },
            onPick = { name ->
                atPickerVisible = false
                val key = replyTarget?.pid
                drafts[key] = (drafts[key] ?: TextFieldValue("")).insertAtSelection("@$name ")
                viewModel.noteAtUsers(listOf(name))
                composerFocusRequest++
            },
        )
    }

    preview?.let { (urls, index) ->
        ImagePreview(urls = urls, initialIndex = index, onDismiss = { preview = null })
    }

    videoUrl?.let { url ->
        VideoPlayerScreen(url = url, onBack = { videoUrl = null })
    }

    if (rewardOpen) {
        RewardDialog(
            onDismiss = { rewardOpen = false },
            onSubmit = { amount, message ->
                rewardOpen = false
                viewModel.reward(null, amount, message)
            },
        )
    }

    reportTarget?.let { target ->
        ReportDialog(
            isThread = target.pid == null,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, message ->
                reportTarget = null
                viewModel.report(target.pid, reason, message)
            },
        )
    }

    deleteTarget?.let { target ->
        MtfConfirmDialog(
            title = "删除回复",
            message = "确定要删除这条回复吗？删除后无法恢复。",
            confirmText = "确认删除",
            cancelText = "取消",
            onDismissRequest = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                viewModel.deleteReply(target.pid)
            },
        )
    }

    likeUsers?.let { users ->
        MtfContentDialog(
            title = "点赞用户 (${users.size})",
            onDismissRequest = { viewModel.clearLikeUsers() },
        ) {
            if (users.isEmpty()) {
                Text(
                    "暂无点赞用户",
                    color = colors.textHint,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(users, key = { it.uid }) { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.clearLikeUsers()
                                    onUserClick(user.uid)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                        ) {
                            AsyncImage(
                                model = smallImageRequest(user.avatarUrl),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.quoteBackground)
                                    .dimImageInDark(),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                user.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                                color = bodyTextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    actionTarget?.let { target ->
        val isOwn = myUid.isNotBlank() && myUid == target.authorUid
        
        val canStick = myUid.isNotBlank() && myUid == page?.opUid
        MtfContentDialog(
            title = "${target.authorName.ifBlank { "匿名" }} · 评论操作",
            onDismissRequest = { actionTarget = null },
        ) {
            MenuAction("回复") {
                actionTarget = null
                if (requireLogin(LOGIN_HINT_REPLY)) {
                    viewModel.setReplyTarget(target)
                    composerVisible = true
                }
            }
            MenuAction("复制") {
                actionTarget = null
                val text = plainTextOf(target.contentHtml)
                if (text.isBlank()) {
                    todo("无可复制内容")
                } else {
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("回复内容", text))
                    todo("已复制回复内容")
                }
            }
            if (isOwn) {
                
                if (!isEditExpired(target.timeText)) {
                    MenuAction("编辑") {
                        actionTarget = null
                        viewModel.beginEdit(target)
                    }
                }
                MenuAction("删除") {
                    actionTarget = null
                    deleteTarget = target
                }
            } else {
                MenuAction("举报") {
                    actionTarget = null
                    if (requireLogin(LOGIN_HINT_REPORT)) reportTarget = ReportTarget(target.pid)
                }
            }
            if (canStick) {
                MenuAction(if (target.sticky) "取消置顶" else "置顶") {
                    actionTarget = null
                    viewModel.stickReply(target.pid, !target.sticky)
                }
            }
        }
    }
}


private const val LOGIN_HINT_REPLY = "你不登录咋回复???"
private const val LOGIN_HINT_LIKE = "嗯?暂无点赞资格"
private const val LOGIN_HINT_FAVORITE = "我说收藏成功了你信吗?"
private const val LOGIN_HINT_REPORT = "666你是有操作的"
private const val LOGIN_HINT_REWARD = "tui~啥也不是!"
private const val LOGIN_HINT_FOLLOW = "你干啥嘞？"


private const val LOGIN_HINT_DURATION_MS = 2000L


private data class ReportTarget(val pid: String?)


private val REPORT_REASONS = listOf("广告垃圾", "恶意灌水", "违规内容", "重复发帖", "其他")




@Composable
private fun ReportDialog(
    isThread: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, message: String) -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary
    var reason by remember { mutableStateOf("其他") }
    var message by remember { mutableStateOf("") }

    MtfContentDialog(
        title = if (isThread) "举报本帖" else "举报该评论",
        subtitle = "请选择举报原因并简要说明，共同维护良好社区氛围",
        onDismissRequest = onDismiss,
    ) {
        Column {
            REPORT_REASONS.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { reason = option }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                ) {
                    RadioButton(selected = option == reason, onClick = { reason = option })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = bodyTextColor(),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = message,
                onValueChange = { message = it },
                textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.quoteBackground)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (message.isEmpty()) {
                        Text("补充说明（可不填）", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = colors.textHint)
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.quoteBackground)
                        .clickable(onClick = onDismiss),
                ) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        color = bodyTextColor(),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isDark) {
                                Modifier
                                    .border(1.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .background(primary.copy(alpha = 0.22f))
                            } else {
                                Modifier.background(primary)
                            },
                        )
                        .clickable { onSubmit(reason, message) },
                ) {
                    Text(
                        "提交举报",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) primary else MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}


@Composable
private fun RewardDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary
    val amounts = listOf(1, 5, 10, 50, 100)
    var amount by remember { mutableStateOf(amounts.first()) }
    var message by remember { mutableStateOf("") }

    MtfContentDialog(
        title = "打赏作者",
        subtitle = "感谢作者的优质分享，支持创作",
        onDismissRequest = onDismiss,
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                amounts.forEach { value ->
                    val active = value == amount
                    Text(
                        value.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = if (active) (if (isDark) primary else MaterialTheme.colorScheme.onPrimary) else bodyTextColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (active && isDark) {
                                    Modifier
                                        .border(1.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(50))
                                        .background(primary.copy(alpha = 0.22f))
                                } else {
                                    Modifier.background(if (active) primary else colors.quoteBackground)
                                },
                            )
                            .clickable { amount = value }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            BasicTextField(
                value = message,
                onValueChange = { message = it },
                textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.quoteBackground)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (message.isEmpty()) {
                        Text("留言（可不填）", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = colors.textHint)
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.quoteBackground)
                        .clickable(onClick = onDismiss),
                ) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        color = bodyTextColor(),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isDark) {
                                Modifier
                                    .border(1.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .background(primary.copy(alpha = 0.22f))
                            } else {
                                Modifier.background(primary)
                            },
                        )
                        .clickable { onSubmit(amount, message.ifBlank { null }) },
                ) {
                    Text(
                        "确认打赏",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) primary else MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}


@Composable
private fun MenuAction(label: String, onClick: () -> Unit) {
    val colors = LocalMtfColors.current
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
        color = bodyTextColor(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}




private fun Modifier.userClick(
    uid: String?,
    onUserClick: (String) -> Unit,
): Modifier =
    if (uid.isNullOrBlank()) this else clickable { onUserClick(uid) }

private fun shareThread(context: Context, subject: String?, tid: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$subject https://bbs.binmt.cc/thread-$tid-1-1.html")
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}







@Composable
internal fun ThreadDetailTopBar(
    post: Post?,
    following: Boolean,
    followEnabled: Boolean,
    
    canEditThread: Boolean = false,
    showMore: Boolean,
    downloadBusy: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onReport: () -> Unit,
    onReward: () -> Unit,
    onDownload: () -> Unit,
    onEditThread: () -> Unit = {},
    onUserClick: (String) -> Unit,
    onDoubleTap: () -> Unit,
) {
    val colors = LocalMtfColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            
            .onDoubleTapToTop(onDoubleTap)
            .statusBarsPadding()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = TopBarVerticalPadding,
                bottom = TopBarVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(com.aazhou.mtforum.R.drawable.ic_back),
            contentDescription = "返回",
            tint = bodyTextColor(),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(7.dp),
        )
        Spacer(Modifier.width(6.dp))
        AsyncImage(
            model = smallImageRequest(post?.avatarUrl),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.quoteBackground)
                .dimImageInDark()
                .userClick(post?.authorUid, onUserClick),
        )
        Spacer(Modifier.width(8.dp))
        
        Column(Modifier.weight(1f)) {
            Text(
                post?.authorName?.ifBlank { "匿名" }.orEmpty(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.userClick(post?.authorUid, onUserClick),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                Text(
                    post?.timeText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                )
                if (!post?.location.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        post?.location.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textHint,
                        maxLines = 1,
                    )
                }
            }
        }
        if (followEnabled) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (following) "已关注" else "关注",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Medium,
                color = if (following) colors.textSecondary else accentTextColor(),
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (following) colors.quoteBackground else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onFollow)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        
        
        if (showMore) {
            
            Spacer(Modifier.width(8.dp))
            MoreMenu(
                downloadBusy = downloadBusy,
                canEditThread = canEditThread,
                onReport = onReport,
                onReward = onReward,
                onDownload = onDownload,
                onEditThread = onEditThread,
            )
        }
    }
}


@Composable
private fun MoreMenu(
    downloadBusy: Boolean,
    canEditThread: Boolean = false,
    onReport: () -> Unit,
    onReward: () -> Unit,
    onDownload: () -> Unit,
    onEditThread: () -> Unit = {},
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    var open by remember { mutableStateOf(false) }
    var wasBusy by remember { mutableStateOf(false) }
    LaunchedEffect(downloadBusy) {
        if (wasBusy && !downloadBusy) open = false
        wasBusy = downloadBusy
    }
    Box {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = { open = true }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.aazhou.mtforum.R.drawable.ic_more),
                contentDescription = "更多",
                tint = bodyTextColor(),
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = colors.surface,
            shadowElevation = 6.dp,
            border = if (isDark) androidx.compose.foundation.BorderStroke(0.5.dp, colors.divider) else null,
            modifier = Modifier.width(104.dp),
        ) {
            val itemStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            val itemPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            if (canEditThread) {
                DropdownMenuItem(
                    text = { Text("编辑", style = itemStyle, color = bodyTextColor()) },
                    contentPadding = itemPadding,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(com.aazhou.mtforum.R.drawable.ic_post_edit),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        open = false
                        onEditThread()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("打赏", style = itemStyle, color = bodyTextColor()) },
                contentPadding = itemPadding,
                leadingIcon = {
                    Icon(
                        painter = painterResource(com.aazhou.mtforum.R.drawable.ic_reward_gift),
                        contentDescription = null,
                        tint = colors.gold,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    open = false
                    onReward()
                },
            )
            DropdownMenuItem(
                text = { Text("举报", style = itemStyle, color = bodyTextColor()) },
                contentPadding = itemPadding,
                leadingIcon = {
                    Icon(
                        painter = painterResource(com.aazhou.mtforum.R.drawable.ic_report_shield),
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    open = false
                    onReport()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (downloadBusy) "下载中…" else "下载",
                        style = itemStyle,
                        color = bodyTextColor(),
                    )
                },
                contentPadding = itemPadding,
                leadingIcon = {
                    if (downloadBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(com.aazhou.mtforum.R.drawable.ic_download),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                enabled = !downloadBusy,
                onClick = {
                    onDownload()
                },
            )
        }
    }
}





@Composable
internal fun DetailHeader(
    post: Post,
    subject: String?,
    forumName: String?,
    onImageClick: (List<String>, Int) -> Unit,
    onVideoClick: ((String) -> Unit)? = null,
    pageImages: List<String>,
    onUnlock: () -> Unit,
    onLinkClick: ((url: String) -> Unit)? = null,
    onAttachmentClick: ((fileName: String, url: String) -> Unit)? = null,
) {
    val colors = LocalMtfColors.current
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = subject.orEmpty(),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 29.sp),
            fontWeight = FontWeight.Medium,
            color = bodyTextColor(),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        
        if (!post.editedText.isNullOrBlank() || !forumName.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!post.editedText.isNullOrBlank()) {
                    Text(
                        text = post.editedText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontStyle = FontStyle.Italic,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!forumName.isNullOrBlank()) {
                    Text(
                        text = forumName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .border(
                                0.1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.69f),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            color = colors.divider,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(2.dp))
        PostContent(
            html = post.contentHtml,
            onImageClick = onImageClick,
            onVideoClick = onVideoClick,
            pageImages = pageImages,
            bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            onLinkClick = onLinkClick,
            onAttachmentClick = onAttachmentClick,
        )
        if (post.hasHiddenContent) {
            HiddenContentCard(onUnlock = onUnlock)
        }
    }
}


@Composable
private fun HiddenContentCard(onUnlock: () -> Unit) {
    val colors = LocalMtfColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .then(if (LocalIsDarkTheme.current) Modifier else Modifier.border(1.dp, colors.divider, RoundedCornerShape(12.dp)))
            .clickable(onClick = onUnlock)
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        ) {
            Icon(
                painterResource(com.aazhou.mtforum.R.drawable.ic_lock_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "包含隐藏内容",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "回复本帖后自动刷新解锁",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        
        Text(
            "去回复",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            fontWeight = FontWeight.Medium,
            color = bodyTextColor(),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(
                    0.1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.69f),
                    RoundedCornerShape(50),
                )
                .clickable(onClick = onUnlock)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}


@Composable
internal fun CommentHeader(
    total: Int?,
    folded: Int,
    foldExpanded: Boolean,
    opOnly: Boolean,
    orderType: Int,
    sortable: Boolean = true,
    onToggleFold: () -> Unit,
    onOpOnly: (Boolean) -> Unit,
    onOrder: (Int) -> Unit,
) {
    val colors = LocalMtfColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "全部评论",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.5.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (total != null) {
                Text(
                    "($total)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (folded > 0) {
                Text(
                    if (foldExpanded) "收起已折叠" else "已折叠 $folded 条",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.quoteBackground)
                        .clickable(onClick = onToggleFold)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        if (sortable) {
            SegmentedControl(
                selected = when {
                    opOnly -> 0
                    orderType == 1 -> 2
                    else -> 1
                },
                onSelect = { index ->
                    when (index) {
                        0 -> onOpOnly(true)
                        1 -> onOrder(0)
                        else -> onOrder(1)
                    }
                },
            )
        }
    }
}


@Composable
private fun SegmentedControl(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalMtfColors.current
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.quoteBackground)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("楼主", "正序", "倒序").forEachIndexed { index, label ->
            val active = index == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (active) colors.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 9.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    color = if (active) bodyTextColor() else colors.textSecondary,
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReplyBlock(
    reply: Post,
    onReply: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onVideoClick: ((String) -> Unit)? = null,
    onUserClick: (String) -> Unit,
    pageImages: List<String>,
    onLongPress: (Post) -> Unit,
    onThreadClick: (String) -> Unit,
    onAttachmentClick: ((fileName: String, url: String) -> Unit)? = null,
) {
    val colors = LocalMtfColors.current
    val context = LocalContext.current
    val siteHost = context.container().httpClient.host
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { onLongPress(reply) })
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = smallImageRequest(reply.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .dimImageInDark()
                    .userClick(reply.authorUid, onUserClick),
            )
            Spacer(Modifier.width(9.dp))
            
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reply.sticky) {
                        Text(
                            "置顶",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 2.dp),
                        )
                    }
                    if (!reply.floorLabel.isNullOrBlank()) {
                        Text(
                            reply.floorLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(end = 2.dp),
                        )
                    }
                    Text(
                        reply.authorName.ifBlank { "匿名" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        
                        
                        color = bodyTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.userClick(reply.authorUid, onUserClick),
                    )
                    if (reply.isOp) {
                        Spacer(Modifier.width(5.dp))
                        AuthorBadge("楼主")
                    }
                    if (!reply.editedText.isNullOrBlank()) {
                        Spacer(Modifier.width(1.dp))
                        Text(
                            reply.editedText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                PostContent(
                    html = reply.contentHtml,
                    quote = reply.quote,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    pageImages = pageImages,
                    bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { onLongPress(reply) }),
                    onLinkClick = { url ->
                        openForumLink(context, url, siteHost, onThreadClick, onUserClick)
                    },
                    onAttachmentClick = onAttachmentClick,
                    selectable = false,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(45.dp))
            TimeLine(reply)
            Spacer(Modifier.weight(1f))
            Icon(
                painterResource(com.aazhou.mtforum.R.drawable.ic_reply),
                contentDescription = "回复",
                tint = colors.iconSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onReply),
            )
        }
    }
}


@Composable
private fun AuthorBadge(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        fontWeight = FontWeight.Medium,
        color = accentTextColor(),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun TimeLine(post: Post) {
    val colors = LocalMtfColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            post.timeText.orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = colors.textSecondary,
        )
        if (!post.location.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                post.location,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textHint,
            )
        }
    }
}


@Composable
private fun ReplyBar(
    backdrop: LayerBackdrop,
    replyCount: Int,
    liked: Boolean,
    likeCount: Int,
    favorited: Boolean,
    favoriteCount: Int,
    onClick: () -> Unit,
    onComments: () -> Unit,
    onLike: () -> Unit,
    onLikeLongPress: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = LocalMtfColors.current
    
    
    val glassColor =
        if (LocalIsDarkTheme.current) Color(0xFF121212).copy(0.75f) else Color(0xFFFAFAFA).copy(0.75f)
    val glassShape = AbsoluteRoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = ReplyBarTopPadding, bottom = BottomBarBottomMargin),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(ReplyBarCapsuleHeight)
                    .clickable(onClick = onClick)
                    .clip(glassShape)
                    .then(
                    if (LocalIsDarkTheme.current) {
                        Modifier.background(Color(0xFF1E1E1E).copy(alpha = 0.75f), glassShape)
                    } else {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { glassShape },
                            effects = { vibrancy(); blur(16.dp.toPx()) },
                            onDrawSurface = { drawRect(glassColor) },
                        )
                    }
                )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(com.aazhou.mtforum.R.drawable.ic_comment_pen),
                    contentDescription = null,
                    tint = bodyTextColor(),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "写评论",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyTextColor(),
                )
            }
            Spacer(Modifier.width(8.dp))
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(ReplyBarCapsuleHeight)
                    .clip(glassShape)
                    .background(if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.Transparent) 
                    .then(
                    if (LocalIsDarkTheme.current) {
                        Modifier.background(Color(0xFF1E1E1E).copy(alpha = 0.75f), glassShape)
                    } else {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { glassShape },
                            effects = { vibrancy(); blur(16.dp.toPx()) },
                            onDrawSurface = { drawRect(glassColor) },
                        )
                    }
                )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarAction(onClick = onComments, modifier = Modifier.weight(1f)) {
                    BarCount(
                        icon = {
                            Icon(
                                painterResource(com.aazhou.mtforum.R.drawable.ic_reply),
                                contentDescription = "查看评论",
                                tint = bodyTextColor(),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        count = replyCount,
                        color = colors.textSecondary,
                    )
                }
                BarAction(onClick = onLike, onLongClick = onLikeLongPress, modifier = Modifier.weight(1f)) {
                    BarCount(
                        icon = {
                            Icon(
                                painterResource(com.aazhou.mtforum.R.drawable.ic_like_thumb),
                                contentDescription = "点赞",
                                tint = if (liked) MaterialTheme.colorScheme.primary else bodyTextColor(),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        count = likeCount,
                        color = if (liked) MaterialTheme.colorScheme.primary else colors.textSecondary,
                    )
                }
                BarAction(onClick = onFavorite, modifier = Modifier.weight(1f)) {
                    BarCount(
                        icon = {
                            Icon(
                                painterResource(com.aazhou.mtforum.R.drawable.ic_favorite_outline),
                                contentDescription = "收藏",
                                tint = if (favorited) MaterialTheme.colorScheme.primary else bodyTextColor(),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        count = favoriteCount,
                        color = if (favorited) MaterialTheme.colorScheme.primary else colors.textSecondary,
                    )
                }
                BarAction(onClick = onShare, modifier = Modifier.weight(1f)) {
                    BarCount(
                        icon = {
                            Icon(
                                painterResource(com.aazhou.mtforum.R.drawable.ic_share),
                                contentDescription = "分享",
                                tint = bodyTextColor(),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        caption = "分享",
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}


@Composable
private fun BarCount(
    icon: @Composable () -> Unit,
    count: Int?,
    color: Color,
) {
    BarCount(icon = icon, caption = count?.toString(), color = color)
}


@Composable
private fun BarCount(
    icon: @Composable () -> Unit,
    caption: String?,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = color,
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarAction(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        icon()
    }
}


private val ScrimFadeMillis = 350





private val ComposerSlideMillis = 250
