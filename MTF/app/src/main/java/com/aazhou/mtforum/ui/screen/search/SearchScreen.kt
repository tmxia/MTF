package com.aazhou.mtforum.ui.screen.search

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.AtUser
import com.aazhou.mtforum.data.model.Forum
import com.aazhou.mtforum.data.model.SearchTab
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.EmptyBox
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.SwipeTabIndicator
import com.aazhou.mtforum.ui.common.ThreadCard
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.bottomBarContentPadding
import com.aazhou.mtforum.ui.common.onDoubleTapToTop
import com.aazhou.mtforum.ui.common.scrollToNewest
import com.aazhou.mtforum.ui.common.scrollToTop
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import kotlinx.coroutines.launch

private const val PREFS_SEARCH = "mtf_search_prefs"
private const val KEY_HISTORY = "history_keywords"

private val HOT_SEARCH_WORDS = listOf(
    "MT管理器", "逆向破解", "Magisk", "Xposed", "Dex修复",
    "汉化教程", "签名校验", "自动化脚本", "Arsc", "Activity",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onThreadClick: (tid: String) -> Unit,
    onUserClick: (uid: String) -> Unit,
    onForumClick: (fid: String, name: String) -> Unit = { _, _ -> },
) {

    val context = LocalContext.current
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val viewModel: SearchViewModel = appViewModel { SearchViewModel(it.repository, it.contentCache) }

    
    val forumGroups by viewModel.forumGroups.collectAsStateWithLifecycle()
    val selectedFidName by viewModel.selectedFidName.collectAsStateWithLifecycle()
    var boardMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadForumGroups()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val forumState by viewModel.forumState.collectAsStateWithLifecycle()
    val userState by viewModel.userState.collectAsStateWithLifecycle()

    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(viewModel.keyword, TextRange(viewModel.keyword.length))) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    
    var historyList by remember {
        mutableStateOf(loadSearchHistory(context))
    }

    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        historyList = loadSearchHistory(context)
    }

    val performSearch: (String) -> Unit = { query ->
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            
            val updated = (listOf(trimmed) + historyList.filterNot { it.equals(trimmed, ignoreCase = true) }).take(12)
            historyList = updated
            saveSearchHistory(context, updated)

            input = TextFieldValue(trimmed, selection = TextRange(trimmed.length))
            keyboardController?.hide()
            viewModel.submit(trimmed)
        }
    }

    
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val openImage: (List<String>, Int) -> Unit = { urls, index ->
        if (urls.isNotEmpty()) preview = urls to index
    }
    
val listStates = SearchTab.values().map { _k ->
        rememberSaveable(key = _k.name, saver = LazyListState.Saver) { LazyListState() }
    }
    val searchTabs = SearchTab.values()
    val pagerState = rememberPagerState(initialPage = 0) { searchTabs.size }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = rememberCoroutineScope()

    
    LaunchedEffect(input.text) {
        
        if (input.text.isEmpty()) {
            viewModel.clearSearchState()
            scope.launch {
                pagerState.scrollToPage(0)
                listStates.forEach { it.scrollToItem(0) }
            }
        }
            }

            BackHandler(enabled = true) {
        if (input.text.isNotEmpty()) {
            input = androidx.compose.ui.text.input.TextFieldValue("", androidx.compose.ui.text.TextRange(0))
            viewModel.clearSearchState()
            scope.launch { 
                pagerState.scrollToPage(0) 
                listStates.forEach { it.scrollToItem(0) } 
            }
        } else {
            onBack()
        }
    }


    

    









    val resetScrollTick by viewModel.resetScrollTick.collectAsStateWithLifecycle()

    
    LaunchedEffect(Unit) { viewModel.refreshOnEnter() }
    

    val initialTick = remember { resetScrollTick }

    LaunchedEffect(resetScrollTick) {

        if (resetScrollTick > initialTick) {

            listStates[pagerState.currentPage].scrollToNewest()

        }

    }

    
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(searchTabs[pagerState.currentPage])
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.topBar)
                    .statusBarsPadding(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "返回",
                            tint = bodyTextColor(),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.quoteBackground)
                            .then(
                                if (isDark) Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                            .padding(horizontal = 12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_new),
                            contentDescription = null,
                            tint = colors.textHint,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = input,
                            onValueChange = { newVal ->
                    input = newVal
                    if (newVal.text.isEmpty()) {
                        viewModel.clearSearchState()
                    }
                },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = bodyTextColor(),
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { performSearch(input.text) }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (input.text.isEmpty()) {
                                        Text(
                                            "输入关键词搜索帖子...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                            color = colors.textHint,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        AnimatedVisibility(
                            visible = input.text.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { 
                                input = androidx.compose.ui.text.input.TextFieldValue("", androidx.compose.ui.text.TextRange(0))
                                viewModel.clearSearchState()
                                focusManager.clearFocus()
                            },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "清空",
                                    tint = colors.textHint,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    
                    val primary = MaterialTheme.colorScheme.primary
                    val dark = LocalIsDarkTheme.current
                    
                    
                    val foreground =
                        if (primary.luminance() > 0.5f) Color(0xFF111827) else Color.White
                    
                    val buttonForeground = if (dark) foreground.copy(alpha = 0.5f) else foreground
                    val buttonBackground =
                        if (dark) primary.copy(alpha = 0.5f) else primary.copy(alpha = 0.69f)
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(buttonBackground)
                            .clickable { performSearch(input.text) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "搜索",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = buttonForeground,
                        )
                    }
                }

        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.quoteBackground)
                    .clickable { boardMenuOpen = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_list),
                    contentDescription = null,
                    tint = colors.textHint,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "范围：$selectedFidName",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textHint,
                    maxLines = 1,
                )
                Spacer(Modifier.width(2.dp))
                Text(text = "▾", fontSize = 10.sp, color = colors.textHint)
            }
        }

        
        if (boardMenuOpen) {
            Dialog(onDismissRequest = { boardMenuOpen = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.topBar)
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "选择板块",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = bodyTextColor(),
                        )
                        IconButton(
                            onClick = { boardMenuOpen = false },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "关闭",
                                tint = colors.textHint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        
                        BoardChip(
                            name = "全部板块",
                            selected = selectedFidName == "全部板块",
                            onClick = {
                                viewModel.selectFid(null, "全部板块")
                                boardMenuOpen = false
                            },
                        )
                        Spacer(Modifier.height(14.dp))

                        
                        forumGroups
                            .filter { it.name != "我关注的" && it.forums.isNotEmpty() }
                            .forEach { group ->
                                Text(
                                    text = group.name,
                                    fontSize = 12.sp,
                                    color = colors.textHint,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                group.forums.chunked(3).forEach { rowForums ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowForums.forEach { forum ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                BoardChip(
                                                    name = forum.name,
                                                    selected = selectedFidName == forum.name,
                                                    onClick = {
                                                        viewModel.selectFid(forum.fid, forum.name)
                                                        boardMenuOpen = false
                                                    },
                                                )
                                            }
                                        }
                                        
                                        repeat(3 - rowForums.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                    }
                }
            }
        }

                val accent = accentTextColor()
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = colors.topBar,
                    contentColor = accent,
                    indicator = { tabPositions ->
                        SwipeTabIndicator(pagerState, tabPositions, accent)
                    },
                ) {
                    searchTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    tab.label,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Medium else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (pagerState.currentPage == index) accent else colors.textSecondary,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onDoubleTapToTop {
                    scope.launch { listStates[pagerState.currentPage].scrollToTop() }
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                
                beyondViewportPageCount = 2,
            ) { page ->
                when (searchTabs[page]) {
                    SearchTab.Thread -> {
                        when (val current = state) {
                    null -> {
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            if (historyList.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_recent),
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "搜索历史",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                            color = bodyTextColor(),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                historyList = emptyList()
                                                clearSearchHistory(context)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            "清空",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = colors.textHint,
                                        )
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                ) {
                                    historyList.forEach { tag ->
                                        SearchChip(
                                            text = tag,
                                            onClick = { performSearch(tag) },
                                            onLongClick = {
                                                val updated = historyList.filterNot { it == tag }
                                                historyList = updated
                                                saveSearchHistory(context, updated)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "已删除「$tag」",
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            },
                                        )
                                    }
                                }
                            }

                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_posts),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "热门搜索",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                    color = bodyTextColor(),
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            ) {
                                HOT_SEARCH_WORDS.forEach { tag ->
                                    SearchChip(
                                        text = tag,
                                        isHighlight = tag == "MT管理器" || tag == "逆向破解",
                                        onClick = { performSearch(tag) },
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(current.message, { viewModel.submit(input.text) })
                    is UiState.Success -> {
                        val result = current.data
                        if (result.threads.isEmpty()) {
                            EmptyBox("没有找到相关主题，换个词试试")
                        } else {
                            val shouldLoadMore by remember {
                                derivedStateOf {
                                    val last = listStates[page].layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    val total = listStates[page].layoutInfo.totalItemsCount
                                    total > 0 && last >= total - 3
                                }
                            }
                            LaunchedEffect(shouldLoadMore) {
                                if (shouldLoadMore) viewModel.loadMore()
                            }
                            LazyColumn(
                                state = listStates[page],
                                contentPadding = PaddingValues(top = 2.5.dp, bottom = bottomBarContentPadding()),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(result.threads, key = { it.tid }) { thread ->
                                    ThreadCard(
                                        thread = thread,
                                        onClick = { onThreadClick(thread.tid) },
                                        onImageClick = openImage,
                                        onUserClick = onUserClick,
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
                    SearchTab.Forum -> {
                        when (val current = forumState) {
                            null -> EmptyBox("输入关键词搜索圈子")
                            is UiState.Loading -> LoadingBox()
                            is UiState.Error -> ErrorBox(current.message, { viewModel.submit(input.text) })
                            is UiState.Success -> {
                                val forums = current.data
                                if (forums.isEmpty()) {
                                    EmptyBox("没有找到相关圈子")
                                } else {
                                    LazyColumn(
                                        state = listStates[page],
                                        contentPadding = PaddingValues(top = 2.5.dp, bottom = bottomBarContentPadding()),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        items(forums, key = { it.fid }) { forum ->
                                            ForumResultRow(
                                                forum = forum,
                                                onClick = { onForumClick(forum.fid, forum.name) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SearchTab.User -> {
                        when (val current = userState) {
                            null -> EmptyBox("输入关键词搜索用户")
                            is UiState.Loading -> LoadingBox()
                            is UiState.Error -> ErrorBox(current.message, { viewModel.submit(input.text) })
                            is UiState.Success -> {
                                val users = current.data
                                if (users.isEmpty()) {
                                    EmptyBox("没有找到相关用户")
                                } else {
                                    LazyColumn(
                                        state = listStates[page],
                                        contentPadding = PaddingValues(top = 2.5.dp, bottom = bottomBarContentPadding()),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        items(users, key = { it.uid }) { user ->
                                            UserResultRow(
                                                user = user,
                                                onClick = { onUserClick(user.uid) },
                                            )
                                        }
                                    }
                                }
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

@Composable
private fun SearchChip(
    text: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isHighlight) primary.copy(alpha = if (isDark) 0.18f else 0.10f)
                else colors.surface,
            )
            .then(
                if (isDark && !isHighlight) Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(16.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = if (isHighlight) primary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun loadSearchHistory(context: Context): List<String> {
    val sp = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
    val raw = sp.getString(KEY_HISTORY, "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split("###|||###").filter { it.isNotBlank() }
}

private fun saveSearchHistory(context: Context, list: List<String>) {
    val sp = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
    
    sp.edit().putString(KEY_HISTORY, list.joinToString("###|||###")).commit()
}

private fun clearSearchHistory(context: Context) {
    val sp = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
    sp.edit().remove(KEY_HISTORY).commit()
}


@Composable
private fun ForumResultRow(
    forum: Forum,
    onClick: () -> Unit,
) {
    val colors = LocalMtfColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.quoteBackground),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null,
                    tint = colors.iconSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                forum.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = bodyTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
private fun UserResultRow(
    user: AtUser,
    onClick: () -> Unit,
) {
    val colors = LocalMtfColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = smallImageRequest(user.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.quoteBackground)
                    .dimImageInDark(),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = bodyTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (user.subtitle.isNotBlank()) {
                    Text(
                        user.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val primary = MaterialTheme.colorScheme.primary
    val bg = if (selected) primary.copy(alpha = 0.15f) else colors.quoteBackground
    val fg = if (selected) primary else bodyTextColor()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            fontSize = 12.5.sp,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
