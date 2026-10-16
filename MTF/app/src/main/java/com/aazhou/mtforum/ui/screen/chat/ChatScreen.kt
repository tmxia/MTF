package com.aazhou.mtforum.ui.screen.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.ChatMessage
import com.aazhou.mtforum.data.parser.SmileyPack
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.ErrorBox
import com.aazhou.mtforum.ui.common.LoadingBox
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.ui.common.PostContent
import com.aazhou.mtforum.ui.common.appViewModel
import com.aazhou.mtforum.ui.common.awaitImeSettled
import com.aazhou.mtforum.ui.common.collectImageUrls
import com.aazhou.mtforum.ui.common.container
import com.aazhou.mtforum.ui.screen.threaddetail.ComposerScrollTracker
import com.aazhou.mtforum.ui.screen.threaddetail.HANDOFF_GIVEUP_FRAMES
import com.aazhou.mtforum.ui.screen.threaddetail.KeyboardTracker
import com.aazhou.mtforum.ui.screen.threaddetail.MinKeyboardHeight
import com.aazhou.mtforum.ui.screen.threaddetail.STABLE_KEYBOARD_FRAMES
import com.aazhou.mtforum.ui.screen.threaddetail.SmileyOverlay
import com.aazhou.mtforum.ui.screen.threaddetail.SmileyPanel
import com.aazhou.mtforum.ui.screen.threaddetail.composerDeletionResult
import com.aazhou.mtforum.ui.screen.threaddetail.formatRichComposerText
import com.aazhou.mtforum.ui.screen.threaddetail.insertAtSelection
import com.aazhou.mtforum.ui.screen.threaddetail.prepareUploadFile
import com.aazhou.mtforum.ui.screen.threaddetail.snapSelectionOutOfSmiley
import com.aazhou.mtforum.ui.theme.DarkBrightTextAlpha
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uid: String,
    targetName: String? = null,
    onBack: () -> Unit,
    onUserClick: ((String) -> Unit)? = null,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val viewModel: ChatViewModel = appViewModel {
        ChatViewModel(uid, targetName, it.repository, it.sessionStore, it.contentCache)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val displayName by viewModel.targetName.collectAsStateWithLifecycle()
    
    val targetOnline = (state as? UiState.Success)?.data?.targetOnline
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val myAvatar by viewModel.myAvatarUrl.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var smileyPanelVisible by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    var inputLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val inputScrollTracker = remember { ComposerScrollTracker() }
    var inputFieldHeight by remember { mutableIntStateOf(0) }
    
    val inputScrollOffset = inputLayout
        ?.takeIf { it.layoutInput.text.text == inputText.text }
        ?.let {
            inputScrollTracker.update(it, inputText.selection, inputFieldHeight)
            inputScrollTracker.offset
        } ?: 0f
    var inputFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeHeight = with(density) { imeInsets.getBottom(density).toDp() }
    
    
    var smileySlot by remember { mutableStateOf(KeyboardTracker.getInitialHeight(context, density)) }
    var keyboardHeight by remember { mutableStateOf(KeyboardTracker.getInitialHeight(context, density)) }
    
    
    var keyboardHandoff by remember { mutableStateOf(false) }
    var smileyPacks by remember { mutableStateOf<List<SmileyPack>>(emptyList()) }
    val recentCodes by context.container().smileyStore.recent
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    val smileyByCode = remember(smileyPacks) {
        smileyPacks.flatMap { it.items }.associateBy { it.code }
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val smileyVisualTransformation = remember(smileyByCode, colors, primaryColor) {
        VisualTransformation { text ->
            TransformedText(
                formatRichComposerText(text.text, smileyByCode, colors, primaryColor),
                OffsetMapping.Identity,
            )
        }
    }
    val listState = rememberLazyListState()
    
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var pendingPreview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val failedSend by viewModel.failedSend.collectAsStateWithLifecycle()

    
    
    LaunchedEffect(failedSend) {
        val failed = failedSend ?: return@LaunchedEffect
        if (inputText.text.isBlank()) inputText = TextFieldValue(failed, TextRange(failed.length))
        viewModel.consumeFailedSend()
    }

    
    
    LaunchedEffect(pendingPreview) {
        val target = pendingPreview ?: return@LaunchedEffect
        awaitImeSettled(imeInsets, density)
        preview = target
        pendingPreview = null
    }

    LaunchedEffect(Unit) {
        smileyPacks = runCatching { context.container().repository.smileyPacks() }
            .getOrDefault(emptyList())
    }

    
    
    
    LaunchedEffect(Unit) {
        var lastPx = -1
        var stableFrames = 0
        while (true) {
            withFrameNanos { }
            val px = imeInsets.getBottom(density)
            if (px > 0) {
                if (px == lastPx) stableFrames++ else { lastPx = px; stableFrames = 0 }
                if (stableFrames >= STABLE_KEYBOARD_FRAMES) {
                    val height = with(density) { px.toDp() }
                    if (height > MinKeyboardHeight) {
                        keyboardHeight = height
                        KeyboardTracker.save(context, px)
                    }
                    lastPx = -1
                    stableFrames = 0
                }
            } else {
                lastPx = -1
                stableFrames = 0
            }
        }
    }

    
    
    
    LaunchedEffect(keyboardHandoff) {
        if (!keyboardHandoff) return@LaunchedEffect
        var lastPx = -1
        var stableFrames = 0
        var idleFrames = 0
        while (keyboardHandoff) {
            withFrameNanos { }
            val px = imeInsets.getBottom(density)
            if (px > 0) {
                idleFrames = 0
                if (px == lastPx) stableFrames++ else { lastPx = px; stableFrames = 0 }
                if (stableFrames >= STABLE_KEYBOARD_FRAMES) break
            } else {
                lastPx = -1
                stableFrames = 0
                idleFrames++
                if (idleFrames >= HANDOFF_GIVEUP_FRAMES) break
            }
        }
        keyboardHandoff = false
        val stableIme = with(density) { imeInsets.getBottom(density).toDp() }
        if (stableIme > 0.dp) {
            
            
            smileySlot = stableIme
            smileyPanelVisible = false
        }
    }

    
    
    
    val bottomSlotHeight = when {
        smileyPanelVisible || keyboardHandoff -> smileySlot
        imeHeight > 0.dp -> imeHeight
        else -> 0.dp
    }

    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val file = prepareUploadFile(context, uri)
                viewModel.uploadChatImage(file) { url ->
                    if (url != null) {
                        
                        inputText = inputText.insertAtSelection("[img]$url[/img]")
                    }
                }
            } catch (e: Exception) {
                AppToast.show(e.message ?: "图片处理失败")
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        contentColor = bodyTextColor(),
        topBar = {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = bodyTextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        
                        if (targetOnline != null) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (targetOnline) "在线" else "离线",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (targetOnline) Color(0xFF22C55E) else colors.textHint,
                            )
                        }
                    }
                },
                actions = { },
                expandedHeight = MtfTopBarHeight,
            )
        },
        bottomBar = {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(colors.topBar)
                    .border(
                        width = 0.5.dp,
                        color = colors.divider,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    )
                    .navigationBarsPadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 38.dp, max = 120.dp)
                                .padding(horizontal = 2.dp, vertical = 6.dp),
                        ) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = { next ->
                                    
                                    
                                    inputText = (inputText.composerDeletionResult(next) ?: next)
                                        .snapSelectionOutOfSmiley(smileyByCode)
                                },
                                textStyle = TextStyle(color = bodyTextColor(), fontSize = 15.sp, lineHeight = 21.sp),
                                cursorBrush = SolidColor(primaryColor),
                                visualTransformation = smileyVisualTransformation,
                                onTextLayout = { inputLayout = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(inputFocusRequester)
                                    
                                    
                                    .onFocusChanged {
                                        inputFocused = it.isFocused
                                        if (it.isFocused && smileyPanelVisible) keyboardHandoff = true
                                    }
                                    .onSizeChanged { inputFieldHeight = it.height },
                                maxLines = 5,
                                decorationBox = { innerTextField ->
                                    Box(Modifier.fillMaxWidth()) {
                                        SmileyOverlay(
                                            text = inputText.text,
                                            layout = inputLayout,
                                            smileyByCode = smileyByCode,
                                            scrollOffset = inputScrollOffset,
                                            modifier = Modifier.matchParentSize().clipToBounds(),
                                        )
                                        if (inputText.text.isEmpty()) {
                                            Text(
                                                "发送私信消息...",
                                                color = colors.textHint,
                                                fontSize = 14.5.sp,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                        }

                        
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = primaryColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ChatToolButton(
                                iconRes = R.drawable.ic_smiley,
                                contentDescription = "表情",
                                isActive = smileyPanelVisible,
                                onClick = {
                                    if (smileyPanelVisible) {
                                        keyboardHandoff = true
                                        inputFocusRequester.requestFocus()
                                        keyboard?.show()
                                    } else {
                                        keyboardHandoff = false
                                        smileySlot = if (imeHeight > MinKeyboardHeight && imeHeight >= keyboardHeight) {
                                            imeHeight
                                        } else {
                                            keyboardHeight
                                        }
                                        smileyPanelVisible = true
                                        focusManager.clearFocus()
                                        keyboard?.hide()
                                    }
                                },
                            )
                            ChatToolButton(
                                iconRes = R.drawable.ic_image,
                                contentDescription = "图片",
                                isActive = false,
                                onClick = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        val canSend = inputText.text.isNotBlank() && !isSending
                        
                        
                        val sendForeground = if (primaryColor.luminance() > 0.5f) {
                            Color(0xFF111827)
                        } else {
                            if (isDark) Color.White.copy(alpha = DarkBrightTextAlpha) else Color.White
                        }
                        val sendBackground =
                            if (canSend) {
                                if (isDark) primaryColor.copy(alpha = 0.5f) else primaryColor
                            } else {
                                primaryColor.copy(alpha = 0.35f)
                            }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(sendBackground)
                                .clickable(enabled = canSend) {
                                    val msg = inputText.text
                                    viewModel.sendMessage(msg) {
                                        inputText = TextFieldValue("")
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = sendForeground,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_send),
                                    contentDescription = "发送",
                                    tint = sendForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomSlotHeight),
                ) {
                    
                    
                    if (smileyPanelVisible && !keyboardHandoff) {
                        SmileyPanel(
                            packs = smileyPacks,
                            recentCodes = recentCodes,
                            onPick = { code ->
                                inputText = inputText.insertAtSelection(code)
                                scope.launch { context.container().smileyStore.addRecent(code) }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingBox(Modifier.padding(padding))
            is UiState.Error -> ErrorBox(current.message, { viewModel.load() }, Modifier.padding(padding))
            is UiState.Success -> {
                val messages = current.data.messages

                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                
                
                
                LaunchedEffect(imeHeight > 0.dp) {
                    if (!(imeHeight > 0.dp) || messages.isEmpty()) return@LaunchedEffect
                    var lastPx = -1
                    var stableFrames = 0
                    while (true) {
                        withFrameNanos { }
                        val px = imeInsets.getBottom(density)
                        if (px > 0) {
                            if (px == lastPx) stableFrames++ else { lastPx = px; stableFrames = 0 }
                            if (stableFrames >= STABLE_KEYBOARD_FRAMES) {
                                listState.scrollToItem(messages.size - 1)
                                break
                            }
                        } else {
                            break
                        }
                    }
                }

                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_notice_pm),
                                contentDescription = null,
                                tint = colors.textHint,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "暂无聊天记录，发条消息打个招呼吧~",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatBubbleItem(
                                message = message,
                                fallbackMyAvatar = myAvatar,
                                onAvatarClick = {
                                    if (!message.isSelf && onUserClick != null) {
                                        onUserClick(uid)
                                    }
                                },
                                onImageClick = { urls, index ->
                                    if (imeHeight > 0.dp) {
                                        focusManager.clearFocus()
                                        keyboard?.hide()
                                    }
                                    pendingPreview = urls to index
                                },
                            )
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
private fun ChatBubbleItem(
    message: ChatMessage,
    fallbackMyAvatar: String?,
    onAvatarClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!message.isSelf) {
            
            AsyncImage(
                model = smallImageRequest(message.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.quoteBackground)
                    .clickable(onClick = onAvatarClick)
                    .dimImageInDark(),
            )
            Spacer(Modifier.width(8.dp))
        }

        
        Column(horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        if (message.isSelf) {
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            )
                        } else {
                            RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            )
                        },
                    )
                    .then(
                        if (message.isSelf) {
                            
                            
                            Modifier.background(
                                if (isDark) primaryColor.copy(alpha = 0.5f)
                                else primaryColor.copy(alpha = 0.69f),
                            )
                        } else {
                            Modifier
                                .then(
                                    if (isDark) {
                                        Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .background(colors.surface)
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                ChatMessageContent(message = message, onImageClick = onImageClick)
            }
            if (message.sending) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = colors.textHint,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "发送中",
                        fontSize = 11.sp,
                        color = colors.textHint,
                    )
                }
            } else if (!message.time.isNullOrBlank()) {
                Text(
                    text = message.time,
                    fontSize = 11.sp,
                    color = colors.textHint,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (message.isSelf) {
            Spacer(Modifier.width(8.dp))
            
            AsyncImage(
                model = smallImageRequest(message.avatarUrl ?: fallbackMyAvatar),
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.quoteBackground)
                    .dimImageInDark(),
            )
        }
    }
}








@Composable
private fun ChatMessageContent(message: ChatMessage, onImageClick: (List<String>, Int) -> Unit) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primaryColor = MaterialTheme.colorScheme.primary
    
    
    val foreground =
        if (primaryColor.luminance() > 0.5f) {
            if (isDark) Color(0xFF111827).copy(alpha = DarkBrightTextAlpha) else Color(0xFF111827)
        } else {
            if (isDark) Color.White.copy(alpha = DarkBrightTextAlpha) else Color.White
        }
    val bubbleTextColor = if (message.isSelf) foreground else bodyTextColor()
    val html = message.contentHtml
    if (html.isNullOrBlank()) {
        Text(
            text = message.text,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = bubbleTextColor,
        )
        return
    }
    val images = remember(html) { collectImageUrls(html) }
    CompositionLocalProvider(LocalContentColor provides bubbleTextColor) {
        PostContent(
            html = html,
            onImageClick = onImageClick,
            
            
            pageImages = images,
            bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
            selectable = false,
        )
    }
}


@Composable
private fun ChatToolButton(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isActive) primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (isActive) primaryColor else colors.iconSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
