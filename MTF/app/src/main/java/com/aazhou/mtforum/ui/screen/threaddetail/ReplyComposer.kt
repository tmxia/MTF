package com.aazhou.mtforum.ui.screen.threaddetail

import android.content.ContentResolver
import androidx.compose.material3.OutlinedTextField
import com.aazhou.mtforum.ui.common.MtfContentDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ripple
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.aazhou.mtforum.data.repo.QuickReplyStore
import com.aazhou.mtforum.ui.common.MtfConfirmDialog
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.EditAttachment
import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.parser.Smiley
import com.aazhou.mtforum.data.parser.SmileyPack
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.ImagePreview
import com.aazhou.mtforum.ui.common.awaitImeSettled
import com.aazhou.mtforum.ui.theme.DarkBrightTextAlpha
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min









private data class UploadedImage(val aid: String, val uri: Uri)

@Composable
internal fun ReplyComposer(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    target: Post?,
    
    editing: Boolean = false,
    sending: Boolean,
    smileyPacks: List<SmileyPack>,
    recentCodes: List<String>,
    onSmileyUsed: (String) -> Unit,
    uploadCredentials: Pair<String, String>?,
    onUploadImage: suspend (File) -> String?,
    
    existingAttachments: List<EditAttachment> = emptyList(),
    
    atUsers: List<String>,
    
    onOpenAtPicker: () -> Unit,
    
    focusRequest: Int = 0,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMtfColors.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    val quickReplyStore = remember { QuickReplyStore(context) }
    var quickPhrases by remember { mutableStateOf(quickReplyStore.getPhrases()) }

    
    var panel by remember {
        mutableStateOf(
            if (quickReplyStore.wasQuickReplyLastUsed()) ComposerPanel.QuickReply
            else ComposerPanel.None
        )
    }
    var uploading by remember { mutableStateOf(false) }

    var pendingQuickSend by remember { mutableStateOf(false) }
    var quickAddDialog by remember { mutableStateOf(false) }
    var quickNewText by remember { mutableStateOf("") }
    var quickDeleteTarget by remember { mutableStateOf<String?>(null) }

    
    var skipPanelInit by remember { mutableStateOf(true) }
    LaunchedEffect(panel) {
        if (skipPanelInit) {
            skipPanelInit = false
            return@LaunchedEffect
        }
        quickReplyStore.setQuickReplyLastUsed(panel == ComposerPanel.QuickReply)
    }

    
    LaunchedEffect(value.text, pendingQuickSend) {
        if (pendingQuickSend && value.text.isNotBlank()) {
            pendingQuickSend = false
            onSend()
        }
    }
    
    var uploaded by remember { mutableStateOf<List<UploadedImage>>(emptyList()) }
    var previewUploaded by remember { mutableStateOf(-1) }
    
    
    var pendingPreviewIndex by remember { mutableIntStateOf(-1) }
    
    var previewRestoreKeyboard by remember { mutableStateOf(false) }
    
    val previewModels: List<Any> = existingAttachments.map { it.url } + uploaded.map { it.uri }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_UPLOAD_IMAGES),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            var done = 0
            var failed = 0
            
            
            val tags = StringBuilder()
            val succeeded = mutableListOf<UploadedImage>()
            try {
                for (uri in uris) {
                    try {
                        val file = prepareUploadFile(context, uri)
                        val aid = onUploadImage(file)
                        if (aid != null) {
                            tags.append("[attachimg]").append(aid).append("[/attachimg]")
                            succeeded += UploadedImage(aid = aid, uri = uri)
                            done++
                        } else {
                            failed++
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        
                        failed++
                    }
                }
                if (succeeded.isNotEmpty()) {
                    onValueChange(value.insertAtSelection(tags.toString()))
                    uploaded = uploaded + succeeded
                }
                when {
                    failed == 0 -> Unit
                    done == 0 -> AppToast.show("上传失败，可能超出大小限制或凭据已过期")
                    else -> AppToast.show("$done 张已插入，$failed 张上传失败")
                }
            } finally {
                uploading = false
            }
        }
    }

    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    
    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val density = LocalDensity.current
    var activeInsertKind by remember { mutableStateOf<InsertKind?>(null) }
    var inlineUrlInput by remember { mutableStateOf("") }
    var inlineTitleInput by remember { mutableStateOf("") }
    var hideContentInput by remember { mutableStateOf("") }
    var hideCreditsInput by remember { mutableStateOf("") }
    var hideDaysInput by remember { mutableStateOf("") }
    var pendingSelection by remember { mutableStateOf<TextRange?>(null) }

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
    
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    var fieldHeight by remember { mutableIntStateOf(0) }
    val scrollTracker = remember { ComposerScrollTracker() }
    
    val scrollOffset = textLayout?.takeIf { it.layoutInput.text.text == value.text }?.let {
        scrollTracker.update(it, value.selection, fieldHeight)
        scrollTracker.offset
    } ?: 0f

    
    fun onComposerValueChange(next: TextFieldValue) {
        onValueChange((value.composerDeletionResult(next) ?: next).snapSelectionOutOfSmiley(smileyByCode))
    }

    fun pickSmiley(code: String) {
        onValueChange(value.insertAtSelection(code))
        if (code !in DefaultSmileyCodes) {
            onSmileyUsed(code)
        }
    }

    
    val recentRow = run {
        val nonDefaultRecent = recentCodes.filter { it !in DefaultSmileyCodes }
        val used = nonDefaultRecent.mapNotNull { smileyByCode[it] }
        val wanted = DefaultSmileyCodes.mapNotNull { smileyByCode[it] }
        val filler = wanted.filter { smiley -> nonDefaultRecent.none { it == smiley.code } }
        (used + filler).take(RecentSmileyLimit)
    }

    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars

    val imeHeight = with(density) { imeInsets.getBottom(density).toDp() }
    val navHeight = with(density) { navInsets.getBottom(density).toDp() }

    
    
    var smileySlot by remember {
        mutableStateOf(KeyboardTracker.getInitialHeight(context, density))
    }

    var keyboardHeight by remember {
        mutableStateOf(KeyboardTracker.getInitialHeight(context, density))
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

    
    
    var keyboardHandoff by remember { mutableStateOf(false) }

    fun switchToKeyboard() {
        if (panel == ComposerPanel.Smiley) {
            keyboardHandoff = true
        }
        focusRequester.requestFocus()
        keyboard?.show()
    }

    
    
    fun openImagePreview(index: Int) {
        previewRestoreKeyboard = imeHeight > 0.dp
        focusManager.clearFocus()
        keyboard?.hide()
        pendingPreviewIndex = index
    }

    
    LaunchedEffect(pendingPreviewIndex) {
        if (pendingPreviewIndex < 0) return@LaunchedEffect
        awaitImeSettled(imeInsets, density)
        previewUploaded = pendingPreviewIndex
        pendingPreviewIndex = -1
    }

    
    
    BackHandler(enabled = panel == ComposerPanel.Smiley) {
        panel = ComposerPanel.None
        onDismiss()
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
        if (imeInsets.getBottom(density) > 0 && panel == ComposerPanel.Smiley) {
            panel = ComposerPanel.None
        }
    }

    val isSmileyOpen = panel == ComposerPanel.Smiley
    
    
    
    val bottomSlotHeight = when {
        isSmileyOpen && !keyboardHandoff -> smileySlot + ComposerSubBarHeight
        keyboardHandoff -> smileySlot
        imeHeight > 0.dp -> imeHeight
        else -> navHeight
    }

    Box(
        modifier
            .fillMaxWidth()
            
            .background(if (LocalIsDarkTheme.current) colors.surface else colors.surface, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)),
    ) {
        
        
        
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ReplyComposerInputHeight)
                    .padding(horizontal = 14.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    if (value.text.isEmpty()) {
                        Text(
                            if (editing) "编辑这条内容" else if (target != null) "回复@${target.authorName}" else "写评论",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textHint,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = ::onComposerValueChange,
                        textStyle = TextStyle(color = bodyTextColor(), fontSize = 15.sp, lineHeight = 22.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = smileyVisualTransformation,
                        onTextLayout = { textLayout = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .onSizeChanged { fieldHeight = it.height }
                            .focusRequester(focusRequester),
                        
                        
                        
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth()) {
                                SmileyOverlay(
                                    text = value.text,
                                    layout = textLayout,
                                    smileyByCode = smileyByCode,
                                    scrollOffset = scrollOffset,
                                    
                                    
                                    modifier = Modifier.matchParentSize().clipToBounds(),
                                )
                                inner()
                            }
                        },
                    )
                    
                    if (panel == ComposerPanel.Smiley) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    switchToKeyboard()
                                },
                        )
                    }
                }
                
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onValueChange(value.copy(selection = TextRange(value.text.length)))
                            switchToKeyboard()
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ComposerToolButton(R.drawable.ic_smiley, "表情") {
                        if (panel == ComposerPanel.Smiley) {
                            switchToKeyboard()
                        } else {
                            keyboardHandoff = false
                            
                            
                            smileySlot = if (imeHeight > MinKeyboardHeight && imeHeight >= keyboardHeight) {
                                imeHeight
                            } else {
                                keyboardHeight
                            }
                            panel = ComposerPanel.Smiley
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                    }
                    ComposerToolButton(R.drawable.ic_image, "图片") {
                        
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    
                    ComposerToolButton(R.drawable.ic_at, "提到朋友") { onOpenAtPicker() }
                    ComposerToolButton(R.drawable.ic_quick_reply, "快速回复") {
                        panel = if (panel == ComposerPanel.QuickReply) ComposerPanel.None
                        else ComposerPanel.QuickReply
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }
                    ComposerToolButton(R.drawable.ic_add, "插入") {
                        if (panel == ComposerPanel.Smiley) {
                            keyboardHandoff = true
                        }
                        panel = if (panel == ComposerPanel.Insert) ComposerPanel.None else ComposerPanel.Insert
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (sending || uploading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    val publishReady = value.text.isNotBlank()
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val isDark = LocalIsDarkTheme.current
                    
                    val publishForeground = if (primaryColor.luminance() > 0.5f) {
                        Color(0xFF111827)
                    } else {
                        if (isDark) Color.White.copy(alpha = DarkBrightTextAlpha) else Color.White
                    }
                    val publishBackground =
                        if (publishReady) {
                            if (isDark) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.69f)
                        } else {
                            colors.textHint.copy(alpha = 0.2f)
                        }
                    Text(
                        if (editing) "保存" else "发布",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (publishReady) publishForeground else colors.textHint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(
                                publishBackground,
                                AbsoluteRoundedCornerShape(50),
                            )
                            .clickable(enabled = publishReady, onClick = onSend)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            
            
            val visibleExisting = remember(value.text, existingAttachments) {
                val tagged = RE_ATTACH_TAG_AID.findAll(value.text)
                    .map { it.groupValues[1] }
                    .toSet()
                existingAttachments.filter { it.aid in tagged }
            }
            
            
            if (uploaded.isNotEmpty() || visibleExisting.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "图片附件 (${visibleExisting.size + uploaded.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(visibleExisting, key = { _, it -> it.aid }) { index, item ->
                        AttachThumb(
                            model = item.url,
                            contentDescription = item.fileName.ifBlank { null },
                            onPreview = { openImagePreview(index) },
                            onInsert = {
                                onValueChange(value.insertAtSelection("[attachimg]${item.aid}[/attachimg]"))
                            },
                            
                            
                            onRemove = { onValueChange(value.withoutAttachTag(item.aid)) },
                        )
                    }
                    itemsIndexed(uploaded, key = { _, it -> it.aid }) { index, item ->
                        AttachThumb(
                            model = item.uri,
                            contentDescription = null,
                            onPreview = { openImagePreview(existingAttachments.size + index) },
                            onInsert = {
                                onValueChange(value.insertAtSelection("[attachimg]${item.aid}[/attachimg]"))
                            },
                            onRemove = {
                                uploaded = uploaded.filterNot { it.aid == item.aid }
                                onValueChange(value.withoutAttachTag(item.aid))
                            },
                        )
                    }
                }
            }

            
            
            val atToken = if (value.selection.collapsed) {
                atTokenAt(value.text, value.selection.start)
            } else {
                null
            }
            if (atToken != null && panel == ComposerPanel.None && activeInsertKind == null) {
                val atSuggestions = filterAtUsers(atUsers, atToken.second)
                if (atSuggestions.isNotEmpty()) {
                    AtSuggestionStrip(
                        suggestions = atSuggestions,
                        onPick = { name ->
                            
                            val end = value.selection.start
                            val merged = value.text.replaceRange(atToken.first, end, "@$name ")
                            val cursor = atToken.first + name.length + 2
                            onValueChange(value.copy(text = merged, selection = TextRange(cursor)))
                        },
                    )
                }
            }

            
            
            
            
            
            val currentInsert = activeInsertKind
            if (currentInsert == InsertKind.Hide) {
                HideInsertBar(
                    contentInput = hideContentInput,
                    creditsInput = hideCreditsInput,
                    daysInput = hideDaysInput,
                    onContentChange = { hideContentInput = it },
                    onCreditsChange = { hideCreditsInput = it },
                    onDaysChange = { hideDaysInput = it },
                    onDismiss = {
                        activeInsertKind = null
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                    onConfirm = {
                        val inserted = buildHideTag(hideContentInput, hideCreditsInput, hideDaysInput)
                        val targetRange = pendingSelection ?: value.selection
                        val start = min(targetRange.min, targetRange.max).coerceIn(0, value.text.length)
                        val end = max(targetRange.min, targetRange.max).coerceIn(0, value.text.length)
                        val merged = value.text.substring(0, start) + inserted + value.text.substring(end)
                        val cursor = start + inserted.length
                        onValueChange(value.copy(text = merged, selection = TextRange(cursor)))
                        pendingSelection = null
                        activeInsertKind = null
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (currentInsert != null) {
                InlineInsertBar(
                    kind = currentInsert,
                    urlInput = inlineUrlInput,
                    titleInput = inlineTitleInput,
                    onUrlChange = { inlineUrlInput = it },
                    onTitleChange = { inlineTitleInput = it },
                    onDismiss = {
                        activeInsertKind = null
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                    onConfirm = {
                        val inserted = when (currentInsert) {
                            InsertKind.Bold -> "[b]${inlineUrlInput.trim()}[/b]"
                            InsertKind.Italic -> "[i]${inlineUrlInput.trim()}[/i]"
                            InsertKind.Underline -> "[u]${inlineUrlInput.trim()}[/u]"
                            InsertKind.Strikethrough -> "[s]${inlineUrlInput.trim()}[/s]"
                            InsertKind.Url -> {
                                val u = inlineUrlInput.trim()
                                val t = inlineTitleInput.trim().ifEmpty { u }
                                "[url=$u]$t[/url]"
                            }
                            InsertKind.NetImage -> "[img]${inlineUrlInput.trim()}[/img]"
                            InsertKind.Audio -> "[audio]${inlineUrlInput.trim()}[/audio]"
                            InsertKind.Video -> "[media=x,500,375]${inlineUrlInput.trim()}[/media]"
                            InsertKind.Flash -> "[flash]${inlineUrlInput.trim()}[/flash]"
                            InsertKind.Quote -> "[quote]${inlineUrlInput.trim()}[/quote]"
                            InsertKind.Code -> "[code]${inlineUrlInput.trim()}[/code]"
                            InsertKind.Hide -> ""
                        }
                        val targetRange = pendingSelection ?: value.selection
                        val start = min(targetRange.min, targetRange.max).coerceIn(0, value.text.length)
                        val end = max(targetRange.min, targetRange.max).coerceIn(0, value.text.length)
                        val merged = value.text.substring(0, start) + inserted + value.text.substring(end)
                        val cursor = start + inserted.length
                        onValueChange(value.copy(text = merged, selection = TextRange(cursor)))
                        pendingSelection = null
                        activeInsertKind = null
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (panel == ComposerPanel.Insert) {
                InsertPanel(
                    onPick = { kind ->
                        val selStart = min(value.selection.min, value.selection.max)
                        val selEnd = max(value.selection.min, value.selection.max)
                        val selected = if (selEnd > selStart) value.text.substring(selStart, selEnd) else ""
                        val isTextFormat = kind in listOf(
                            InsertKind.Bold,
                            InsertKind.Italic,
                            InsertKind.Underline,
                            InsertKind.Strikethrough,
                            InsertKind.Quote,
                            InsertKind.Code,
                        )
                        if (selected.isNotEmpty() && isTextFormat) {
                            val tag = when (kind) {
                                InsertKind.Bold -> "b"
                                InsertKind.Italic -> "i"
                                InsertKind.Underline -> "u"
                                InsertKind.Strikethrough -> "s"
                                InsertKind.Quote -> "quote"
                                InsertKind.Code -> "code"
                                else -> ""
                            }
                            onValueChange(value.wrapSelectionOrInsert("[$tag]", "[/$tag]"))
                            panel = ComposerPanel.None
                            focusRequester.requestFocus()
                            keyboard?.show()
                        } else {
                            activeInsertKind = kind
                            inlineUrlInput = ""
                            inlineTitleInput = selected
                            if (kind == InsertKind.Hide) {
                                hideContentInput = selected
                                hideCreditsInput = ""
                                hideDaysInput = ""
                            }
                            pendingSelection = value.selection
                            panel = ComposerPanel.None
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComposerSubBarHeight)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                )
            } else if (panel == ComposerPanel.QuickReply) {
                QuickReplyPanel(
                    phrases = quickPhrases,
                    onPick = { phrase ->
                        onValueChange(TextFieldValue(phrase))
                        pendingQuickSend = true
                    },
                    onLongPress = { phrase -> quickDeleteTarget = phrase },
                    onAdd = {
                        quickNewText = ""
                        quickAddDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComposerSubBarHeight)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                )
            } else if (panel == ComposerPanel.None || keyboardHandoff) {
                RecentSmileyRow(
                    smileys = recentRow,
                    onPick = { code -> pickSmiley(code) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComposerSubBarHeight)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }

            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomSlotHeight),
            ) {
                
                
                if (panel == ComposerPanel.Smiley && !keyboardHandoff) {
                    SmileyPanel(
                        packs = smileyPacks,
                        recentCodes = recentCodes,
                        onPick = { code -> pickSmiley(code) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    
    if (previewUploaded in previewModels.indices) {
        ImagePreview(
            urls = previewModels,
            initialIndex = previewUploaded,
            onDismiss = {
                previewUploaded = -1
                
                
                if (previewRestoreKeyboard) switchToKeyboard()
            },
        )
    }
        
        if (quickAddDialog) {
            QuickReplyAddDialog(
                text = quickNewText,
                onTextChange = { quickNewText = it },
                onConfirm = {
                    val t = quickNewText.trim()
                    if (t.isNotEmpty()) {
                        quickReplyStore.addPhrase(t)
                        quickPhrases = quickReplyStore.getPhrases()
                    }
                    quickAddDialog = false
                    quickNewText = ""
                },
                onDismiss = {
                    quickAddDialog = false
                    quickNewText = ""
                },
            )
        }

        
        quickDeleteTarget?.let { target ->
            com.aazhou.mtforum.ui.common.MtfConfirmDialog(
                title = "删除短语",
                message = "确定删除「$target」吗？",
                confirmText = "删除",
                confirmColor = androidx.compose.ui.graphics.Color(0xFFEF4444),
                maxWidth = 240.dp,
                onDismissRequest = { quickDeleteTarget = null },
                onConfirm = {
                    quickReplyStore.removePhrase(target)
                    quickPhrases = quickReplyStore.getPhrases()
                    quickDeleteTarget = null
                },
            )
        }
}

private enum class ComposerPanel { None, Smiley, Insert, QuickReply }








internal fun smileyCodeStartBefore(text: String, cursor: Int): Int {
    if (cursor <= 0 || cursor > text.length) return -1
    val open = text.lastIndexOf('[', (cursor - 1).coerceAtLeast(0))
    if (open < 0) return -1
    val close = text.indexOf(']', open + 1)
    if (close < 0 || close + 1 > text.length) return -1
    
    return if (cursor <= close + 1) open else -1
}


private fun smileyRangesOf(text: String, smileyByCode: Map<String, Smiley>): List<Pair<IntRange, String>> {
    val found = mutableListOf<Pair<IntRange, String>>()
    var index = 0
    while (index < text.length) {
        val close = if (text[index] == '[') text.indexOf(']', index + 1) else -1
        if (close > index + 1) {
            val code = text.substring(index, close + 1)
            if (smileyByCode.containsKey(code)) {
                found += (index until close + 1) to code
                index = close + 1
                continue
            }
        }
        index++
    }
    return found
}










internal fun formatRichComposerText(
    source: String,
    smileyByCode: Map<String, Smiley>,
    colors: com.aazhou.mtforum.ui.theme.MtfColors,
    primaryColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        append(source)
        if (source.isEmpty()) return@buildAnnotatedString

        val capsuleBg = colors.textHint.copy(alpha = 0.08f)
        val tagBadgeBg = colors.textHint.copy(alpha = 0.16f)
        val tagTextColor = colors.textHint.copy(alpha = 0.65f)

        
        fun styleTagPair(
            entireRange: IntRange,
            openRange: IntRange,
            closeRange: IntRange,
            contentRange: IntRange,
            contentStyle: SpanStyle? = null,
        ) {
            
            addStyle(SpanStyle(background = capsuleBg), entireRange.first, entireRange.last + 1)
            
            val tagStyle = SpanStyle(
                color = tagTextColor,
                background = tagBadgeBg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
            addStyle(tagStyle, openRange.first, openRange.last + 1)
            addStyle(tagStyle, closeRange.first, closeRange.last + 1)
            
            if (contentStyle != null && !contentRange.isEmpty()) {
                addStyle(contentStyle, contentRange.first, contentRange.last + 1)
            }
        }

        
        
        
        
        if (smileyByCode.isNotEmpty() && source.contains('[')) {
            val smileyRanges = smileyRangesOf(source, smileyByCode)
            for ((range, code) in smileyRanges) {
                addStyle(
                    SpanStyle(color = Color.Transparent, letterSpacing = smileySlotSpacing(code)),
                    range.first,
                    range.last + 1,
                )
            }
        }

        
        val urlRegex = Regex("""(\[url(?:=[^\]]*)?\])([\s\S]*?)(\[/url\])""", RegexOption.IGNORE_CASE)
        for (match in urlRegex.findAll(source)) {
            val entireRange = match.range
            val openGroup = match.groups[1]!!
            val contentGroup = match.groups[2]!!
            val closeGroup = match.groups[3]!!

            styleTagPair(
                entireRange = entireRange,
                openRange = openGroup.range,
                closeRange = closeGroup.range,
                contentRange = contentGroup.range,
                contentStyle = SpanStyle(
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        
        val boldRegex = Regex("""(\[b\])([\s\S]*?)(\[/b\])""", RegexOption.IGNORE_CASE)
        for (match in boldRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(fontWeight = FontWeight.Bold),
            )
        }

        
        val italicRegex = Regex("""(\[i\])([\s\S]*?)(\[/i\])""", RegexOption.IGNORE_CASE)
        for (match in italicRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(fontStyle = FontStyle.Italic),
            )
        }

        
        val underlineRegex = Regex("""(\[u\])([\s\S]*?)(\[/u\])""", RegexOption.IGNORE_CASE)
        for (match in underlineRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(textDecoration = TextDecoration.Underline),
            )
        }

        
        val strikeRegex = Regex("""(\[s\])([\s\S]*?)(\[/s\])""", RegexOption.IGNORE_CASE)
        for (match in strikeRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(textDecoration = TextDecoration.LineThrough),
            )
        }

        
        val quoteRegex = Regex("""(\[quote\])([\s\S]*?)(\[/quote\])""", RegexOption.IGNORE_CASE)
        for (match in quoteRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(color = colors.textSecondary, fontStyle = FontStyle.Italic),
            )
        }

        
        val codeRegex = Regex("""(\[code\])([\s\S]*?)(\[/code\])""", RegexOption.IGNORE_CASE)
        for (match in codeRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = colors.textHint.copy(alpha = 0.14f),
                ),
            )
        }

        
        val hideRegex = Regex("""(\[hide(?:=[^\]]*)?\])([\s\S]*?)(\[/hide\])""", RegexOption.IGNORE_CASE)
        for (match in hideRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[3]!!.range,
                contentRange = match.groups[2]!!.range,
                contentStyle = SpanStyle(color = colors.textSecondary),
            )
        }

        
        val mediaRegex = Regex("""(\[(img|audio|media[^\\]]*|flash|attachimg)\])([\s\S]*?)(\[/(?:img|audio|media|flash|attachimg)\])""", RegexOption.IGNORE_CASE)
        for (match in mediaRegex.findAll(source)) {
            styleTagPair(
                entireRange = match.range,
                openRange = match.groups[1]!!.range,
                closeRange = match.groups[4]!!.range,
                contentRange = match.groups[3]!!.range,
                contentStyle = SpanStyle(color = colors.textSecondary, fontSize = 13.sp),
            )
        }
    }
}












private val RE_ATTACH_TAG_AID = Regex("""\[attachimg\](\d+)\[/attachimg\]""", RegexOption.IGNORE_CASE)






internal class ComposerScrollTracker {
    
    var offset by mutableFloatStateOf(0f)
        private set

    private var previousCursorRect: Rect? = null
    private var previousSelection: TextRange = TextRange.Zero

    fun update(layout: TextLayoutResult, selection: TextRange, viewportHeight: Int) {
        val maximum = (layout.size.height - viewportHeight).coerceAtLeast(0).toFloat()
        
        val follow = when {
            selection.start != previousSelection.start -> selection.start
            selection.end != previousSelection.end -> selection.end
            else -> selection.min
        }
        previousSelection = selection
        val cursorRect = layout.getCursorRect(follow.coerceIn(0, layout.layoutInput.text.length))
        val previous = previousCursorRect
        if (previous == null || cursorRect.top != previous.top || cursorRect.left != previous.left) {
            val endBound = offset + viewportHeight
            val cursorHeight = cursorRect.bottom - cursorRect.top
            val shift = when {
                cursorRect.bottom > endBound -> cursorRect.bottom - endBound
                cursorRect.top < offset && cursorHeight > viewportHeight -> cursorRect.bottom - endBound
                cursorRect.top < offset -> cursorRect.top - offset
                else -> 0f
            }
            offset += shift
            previousCursorRect = cursorRect
        }
        offset = offset.coerceIn(0f, maximum)
    }
}





internal fun TextFieldValue.composerDeletionResult(next: TextFieldValue): TextFieldValue? {
    val shrunkByOne = next.text.length == text.length - 1 &&
        next.selection.collapsed && selection.collapsed
    if (!shrunkByOne) return null
    
    handleTagPairDeletion(this, next)?.let { return it }
    
    val codeStart = smileyCodeStartBefore(text, selection.start)
    if (codeStart >= 0) {
        val codeEnd = text.indexOf(']', codeStart) + 1
        return copy(text = text.removeRange(codeStart, codeEnd), selection = TextRange(codeStart))
    }
    return null
}







internal fun TextFieldValue.snapSelectionOutOfSmiley(smileyByCode: Map<String, Smiley>): TextFieldValue {
    if (smileyByCode.isEmpty() || !selection.collapsed) return this
    val cursor = selection.start
    for ((range, _) in smileyRangesOf(text, smileyByCode)) {
        if (cursor > range.first && cursor <= range.last) {
            
            val end = range.last + 1
            val target = if (cursor - range.first <= end - cursor) range.first else end
            return copy(selection = TextRange(target))
        }
    }
    return this
}









internal fun smileySlotSpacing(code: String): TextUnit {
    val naturalEm = code.sumOf { ch ->
        when {
            ch == '[' || ch == ']' -> 0.4
            ch.code > 0x2E80 -> 1.0
            else -> 0.55
        }
    }.toFloat()
    return ((1.8f - naturalEm) / code.length).em
}

@Composable
internal fun SmileyOverlay(
    text: String,
    layout: TextLayoutResult?,
    smileyByCode: Map<String, Smiley>,
    scrollOffset: Float = 0f,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    if (layout == null || smileyByCode.isEmpty()) return
    
    
    val source = layout.layoutInput.text.text
    val ranges = remember(source, smileyByCode) { smileyRangesOf(source, smileyByCode) }
    if (ranges.isEmpty()) return
    val density = LocalDensity.current
    
    
    
    
    Box(modifier) {
        ranges.forEach { (range, code) ->
            val smiley = smileyByCode[code] ?: return@forEach
            if (range.first >= layout.layoutInput.text.length) return@forEach
            val first = layout.getBoundingBox(range.first)
            val last = if (range.last < layout.layoutInput.text.length && range.last > range.first) {
                layout.getBoundingBox(range.last)
            } else {
                first
            }
            
            val box = if (last.top != first.top) first else Rect(first.left, first.top, last.right, last.bottom)
            val line = layout.getLineForOffset(range.first)
            val lineTop = layout.getLineTop(line)
            val lineBottom = layout.getLineBottom(line)
            with(density) {
                val wanted = 22.dp.toPx()
                
                
                val side = min(wanted, lineBottom - lineTop)
                if (side <= 0f) return@forEach
                
                
                val left = box.left + ((box.width - side) / 2f).coerceAtLeast(0f)
                
                
                val top = lineTop + ((lineBottom - lineTop - side) / 2f).coerceAtLeast(0f) - scrollOffset
                key(range.first to code) {
                    AsyncImage(
                        model = smiley.url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .absoluteOffset(left.toDp(), top.toDp())
                            .requiredSize(side.toDp())
                            .dimImageInDark(),
                    )
                }
            }
        }
    }
}


internal enum class InsertKind(val label: String, val iconRes: Int? = null) {
    Bold("粗体", R.drawable.ic_format_bold),
    Italic("斜体", R.drawable.ic_format_italic),
    Underline("下划线", R.drawable.ic_format_underline),
    Strikethrough("删除线", R.drawable.ic_format_strikethrough),
    Url("文字链接"),
    NetImage("网络图片"),
    Audio("MP3 音乐"),
    Video("网络视频"),
    Flash("Flash"),
    Quote("引用"),
    Code("代码"),
    Hide("隐藏内容"),
}








internal fun buildHideTag(content: String, credits: String, days: String): String {
    val c = credits.trim()
    val d = days.trim()
    val attr = when {
        c.isEmpty() && d.isEmpty() -> ""
        c.isNotEmpty() && d.isEmpty() -> "=$c"
        c.isEmpty() -> "=d$d"
        else -> "=d$d,$c"
    }
    return "[hide$attr]$content[/hide]"
}


internal fun TextFieldValue.insertAtSelection(
    text: String,
    cursorOffsetInInserted: Int = text.length,
): TextFieldValue {
    val start = min(selection.min, selection.max).coerceIn(0, this.text.length)
    val end = max(selection.min, selection.max).coerceIn(0, this.text.length)
    val merged = this.text.substring(0, start) + text + this.text.substring(end)
    val cursor = start + cursorOffsetInInserted.coerceIn(0, text.length)
    return copy(text = merged, selection = TextRange(cursor))
}





@Composable
private fun AttachThumb(
    model: Any,
    contentDescription: String?,
    onPreview: () -> Unit,
    onInsert: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalMtfColors.current
    Box(
        Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background)
            
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onPreview),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "删除这张图片",
                tint = Color.White,
                modifier = Modifier.size(11.dp),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onInsert)
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "插入",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}


internal fun TextFieldValue.withoutAttachTag(aid: String): TextFieldValue {
    val tag = "[attachimg]$aid[/attachimg]"
    val at = text.indexOf(tag)
    if (at < 0) return this
    val merged = text.removeRange(at, at + tag.length)
    return copy(text = merged, selection = TextRange(at.coerceIn(0, merged.length)))
}





@Composable
internal fun AtSuggestionStrip(
    suggestions: List<String>,
    onPick: (String) -> Unit,
) {
    val colors = LocalMtfColors.current
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = AtSuggestionStripHeight)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(suggestions, key = { it }) { name ->
            Text(
                "@$name",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = bodyTextColor(),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.quoteBackground)
                    .clickable { onPick(name) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private val AtSuggestionStripHeight = 44.dp






internal fun atTokenAt(text: String, cursor: Int): Pair<Int, String>? {
    if (cursor < 0 || cursor > text.length) return null
    var start = cursor
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    val token = text.substring(start, cursor)
    if (!token.startsWith('@')) return null
    return start to token.drop(1)
}


internal fun filterAtUsers(users: List<String>, keyword: String): List<String> {
    if (users.isEmpty()) return emptyList()
    if (keyword.isEmpty()) return users
    val prefix = users.filter { it.startsWith(keyword, ignoreCase = true) }
    val rest = users.filter { it !in prefix && it.contains(keyword, ignoreCase = true) }
    return prefix + rest
}









internal fun normalizeAtMentions(message: String, knownNames: Collection<String>): String {
    val names = knownNames.distinct().filter { it.isNotEmpty() }.sortedByDescending { it.length }
    if (names.isEmpty()) return message
    val sb = StringBuilder(message)
    var i = 0
    while (i < sb.length) {
        if (sb[i] == '@') {
            val name = names.firstOrNull { sb.startsWith(it, i + 1, ignoreCase = true) }
            if (name != null) {
                val after = i + 1 + name.length
                
                if (after < sb.length && !sb[after].isAtSeparator()) {
                    sb.insert(after, ' ')
                    i = after + 1
                } else {
                    i = after
                }
                continue
            }
        }
        i++
    }
    return sb.toString()
}


private fun Char.isAtSeparator(): Boolean =
    this == ' ' || this == '\t' || this == '\n' || this == '\r' || this == '\u000B' || this == '\u000C'





internal fun TextFieldValue.wrapSelectionOrInsert(
    prefix: String,
    suffix: String,
): TextFieldValue {
    val start = min(selection.min, selection.max).coerceIn(0, text.length)
    val end = max(selection.min, selection.max).coerceIn(0, text.length)
    val selected = text.substring(start, end)
    return if (selected.isNotEmpty()) {
        val merged = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
        copy(text = merged, selection = TextRange(start + prefix.length + selected.length + suffix.length))
    } else {
        val merged = text.substring(0, start) + prefix + suffix + text.substring(end)
        copy(text = merged, selection = TextRange(start + prefix.length))
    }
}


private fun TextFieldValue.wrapUrl(): TextFieldValue {
    val start = min(selection.min, selection.max).coerceIn(0, text.length)
    val end = max(selection.min, selection.max).coerceIn(0, text.length)
    val selected = text.substring(start, end)
    return if (selected.isNotEmpty()) {
        val merged = text.substring(0, start) + "[url=]" + selected + "[/url]" + text.substring(end)
        copy(text = merged, selection = TextRange(start + 5))
    } else {
        val merged = text.substring(0, start) + "[url=][/url]" + text.substring(end)
        copy(text = merged, selection = TextRange(start + 5))
    }
}

@Composable
internal fun ComposerToolButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Icon(
        painterResource(iconRes),
        contentDescription = label,
        tint = LocalMtfColors.current.textSecondary,
        modifier = Modifier
            .size(40.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(6.dp),
    )
}


@Composable
internal fun SmileyPanel(
    packs: List<SmileyPack>,
    recentCodes: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (packs.isEmpty()) {
        Text(
            "表情加载失败",
            style = MaterialTheme.typography.bodySmall,
            color = LocalMtfColors.current.textHint,
            modifier = modifier.padding(vertical = 12.dp),
        )
        return
    }
    
    
    val pagerState = rememberPagerState(initialPage = 1) { packs.size + 1 }
    val scope = rememberCoroutineScope()
    val tabState = rememberLazyListState()
    val smileyByCode = remember(packs) { packs.flatMap { it.items }.associateBy { it.code } }
    val recent = recentCodes.filter { it !in DefaultSmileyCodes }.mapNotNull { smileyByCode[it] }
    
    
    fun emojisOf(page: Int): List<Smiley> = when (page) {
        0 -> recent.ifEmpty {
            DefaultSmileyCodes.mapNotNull { smileyByCode[it] }
                .ifEmpty { packs.firstOrNull()?.items.orEmpty() }
        }
        else -> packs[page - 1].items
    }
    
    LaunchedEffect(pagerState.currentPage) {
        tabState.animateScrollToItem(pagerState.currentPage)
    }
    BoxWithConstraints(modifier) {
        
        
        val gridHeight = maxHeight - SmileyPackBarHeight - 4.dp
        val cellHeight = ((gridHeight - 8.dp - 6.dp * (SmileyGridRows - 1)) / SmileyGridRows)
            .coerceAtLeast(24.dp)
        val iconSize = cellHeight.coerceAtMost(34.dp)
        Column(Modifier.fillMaxSize()) {
            LazyRow(
                state = tabState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
            ) {
                item {
                    SmileyPackTab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_recent),
                            contentDescription = "最近使用",
                            tint = LocalMtfColors.current.textSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                itemsIndexed(packs) { index, pack ->
                    SmileyPackTab(
                        selected = pagerState.currentPage == index + 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index + 1) } },
                    ) {
                        AsyncImage(
                            model = pack.items.first().url,
                            contentDescription = pack.name,
                            modifier = Modifier
                                .size(24.dp)
                                .dimImageInDark(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(SmileyGridColumns),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(
                        items = emojisOf(page),
                        key = { it.code },
                    ) { smiley ->
                        SmileyCell(smiley, onPick, Modifier.height(cellHeight), iconSize)
                    }
                }
            }
        }
    }
}


@Composable
private fun SmileyPackTab(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (selected) {
                    LocalMtfColors.current.textHint.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                AbsoluteRoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}


@Composable
private fun RecentSmileyRow(
    smileys: List<Smiley>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        
        
        smileys.forEach { smiley ->
            key(smiley.code) {
                SmileyCell(smiley, onPick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmileyCell(
    smiley: Smiley,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                indication = ripple(bounded = true, radius = 16.dp),
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onPick(smiley.code) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = smiley.url,
            contentDescription = smiley.code,
            modifier = Modifier
                .size(iconSize)
                .dimImageInDark(),
        )
    }
}


@Composable
internal fun InsertPanel(onPick: (InsertKind) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(InsertKind.values()) { kind ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable { onPick(kind) }
                    .background(LocalMtfColors.current.background, AbsoluteRoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                if (kind.iconRes != null) {
                    Icon(
                        painter = painterResource(kind.iconRes),
                        contentDescription = null,
                        tint = LocalMtfColors.current.textSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Text(
                    kind.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalMtfColors.current.textSecondary,
                )
            }
        }
    }
}


@Composable
internal fun InlineInsertBar(
    kind: InsertKind,
    urlInput: String,
    titleInput: String,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMtfColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val urlFocusRequester = remember { FocusRequester() }
    val isUrl = kind == InsertKind.Url
    val ready = if (isUrl) urlInput.isNotBlank() else urlInput.isNotBlank()

    LaunchedEffect(kind) {
        delay(60)
        urlFocusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = kind.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            BasicTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                singleLine = true,
                textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, AbsoluteRoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .focusRequester(urlFocusRequester),
                decorationBox = { inner ->
                    if (urlInput.isEmpty()) {
                        Text(
                            text = when (kind) {
                                InsertKind.Bold -> "请输入加粗文本"
                                InsertKind.Italic -> "请输入斜体文本"
                                InsertKind.Underline -> "请输入下划线文本"
                                InsertKind.Strikethrough -> "请输入删除线文本"
                                InsertKind.Url -> "请输入链接网址 (https://...)"
                                InsertKind.NetImage -> "请输入图片网址"
                                InsertKind.Audio -> "请输入音频网址"
                                InsertKind.Video -> "请输入视频网址"
                                InsertKind.Flash -> "请输入 Flash 网址"
                                InsertKind.Quote -> "请输入引用内容"
                                InsertKind.Code -> "请输入代码"
                                InsertKind.Hide -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textHint,
                        )
                    }
                    inner()
                },
            )
            val primaryColor = MaterialTheme.colorScheme.primary
            Text(
                text = "插入",
                style = MaterialTheme.typography.labelMedium,
                color = if (ready) Color.White else colors.textHint,
                modifier = Modifier
                    .background(
                        if (ready) primaryColor.copy(alpha = 0.69f) else colors.textHint.copy(alpha = 0.2f),
                        AbsoluteRoundedCornerShape(50),
                    )
                    .clickable(enabled = ready, onClick = onConfirm)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textHint,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        if (isUrl) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "文字",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
                BasicTextField(
                    value = titleInput,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (titleInput.isEmpty()) {
                            Text("显示文字（选填，留空则同网址）", style = MaterialTheme.typography.bodySmall, color = colors.textHint)
                        }
                        inner()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, AbsoluteRoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}


@Composable
internal fun HideInsertBar(
    contentInput: String,
    creditsInput: String,
    daysInput: String,
    onContentChange: (String) -> Unit,
    onCreditsChange: (String) -> Unit,
    onDaysChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMtfColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val contentFocusRequester = remember { FocusRequester() }
    val ready = contentInput.isNotBlank()

    LaunchedEffect(Unit) {
        delay(60)
        contentFocusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "隐藏内容",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            BasicTextField(
                value = contentInput,
                onValueChange = onContentChange,
                singleLine = true,
                textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, AbsoluteRoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .focusRequester(contentFocusRequester),
                decorationBox = { inner ->
                    if (contentInput.isEmpty()) {
                        Text(
                            text = "请输入要隐藏的信息内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textHint,
                        )
                    }
                    inner()
                },
            )
            val primaryColor = MaterialTheme.colorScheme.primary
            Text(
                text = "插入",
                style = MaterialTheme.typography.labelMedium,
                color = if (ready) Color.White else colors.textHint,
                modifier = Modifier
                    .background(
                        if (ready) primaryColor.copy(alpha = 0.69f) else colors.textHint.copy(alpha = 0.2f),
                        AbsoluteRoundedCornerShape(50),
                    )
                    .clickable(enabled = ready, onClick = onConfirm)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textHint,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "积分大于",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            BasicTextField(
                value = creditsInput,
                onValueChange = { if (it.all(Char::isDigit)) onCreditsChange(it) },
                singleLine = true,
                textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                decorationBox = { inner ->
                    if (creditsInput.isEmpty()) {
                        Text("可不填", style = MaterialTheme.typography.bodySmall, color = colors.textHint)
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, AbsoluteRoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "可见天数",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            BasicTextField(
                value = daysInput,
                onValueChange = { if (it.all(Char::isDigit)) onDaysChange(it) },
                singleLine = true,
                textStyle = TextStyle(color = bodyTextColor(), fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                decorationBox = { inner ->
                    if (daysInput.isEmpty()) {
                        Text("可不填", style = MaterialTheme.typography.bodySmall, color = colors.textHint)
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, AbsoluteRoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "不设置积分和天数，默认为回复可见",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}








private val COMPOSER_TAG_PAIR_REGEX = Regex(
    """(\[(url(?:=[^\]]*)?|audio|img|media(?:=[^\]]*)?|flash|quote|code|attachimg|hide(?:=[^\]]*)?|b|i|u|s)\])([\s\S]*?)(\[/(?:url|audio|img|media|flash|quote|code|attachimg|hide|b|i|u|s)\])""",
    RegexOption.IGNORE_CASE,
)






internal fun handleTagPairDeletion(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue? {
    val oldText = oldValue.text
    val deletedIndex = newValue.selection.start
    if (deletedIndex < 0 || deletedIndex >= oldText.length) return null

    val matches = COMPOSER_TAG_PAIR_REGEX.findAll(oldText)
    for (match in matches) {
        val openGroup = match.groups[1] ?: continue
        val contentGroup = match.groups[3] ?: continue
        val closeGroup = match.groups[4] ?: continue
        val entireRange = match.range

        val openRange = openGroup.range
        val contentRange = contentGroup.range
        val closeRange = closeGroup.range

        
        
        
        
        val isDeletingCloseTag = deletedIndex in closeRange
        val isDeletingOpenTag = deletedIndex in openRange
        val isDeletingEmptyPair = contentRange.isEmpty() && (deletedIndex == openRange.last || deletedIndex == closeRange.first)

        if (isDeletingCloseTag || isDeletingOpenTag || isDeletingEmptyPair) {
            
            val resultText = oldText.removeRange(entireRange.first, entireRange.last + 1)
            return TextFieldValue(resultText, selection = TextRange(entireRange.first))
        }
    }
    return null
}


internal suspend fun prepareUploadFile(context: android.content.Context, uri: Uri): File =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openImageInput(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("图片解码失败")
        }
        val longEdge = max(bounds.outWidth, bounds.outHeight)
        val cache = File(context.cacheDir, "reply_upload.jpg")
        
        val rawSize = try {
            resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        } catch (_: Exception) {
            -1L
        }
        if (rawSize in 1..MAX_UPLOAD_BYTES && longEdge in 1 until MAX_UPLOAD_EDGE) {
            resolver.openImageInput(uri).use { input ->
                cache.outputStream().use { output -> input.copyTo(output) }
            }
            return@withContext cache
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = if (longEdge > MAX_UPLOAD_EDGE) longEdge / MAX_UPLOAD_EDGE else 1
        }
        val decoded = resolver.openImageInput(uri)
            .use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("图片解码失败")
        val scale = min(
            MAX_UPLOAD_EDGE.toFloat() / decoded.width,
            MAX_UPLOAD_EDGE.toFloat() / decoded.height,
        ).coerceAtMost(1f)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }
        
        var quality = 90
        while (true) {
            cache.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (cache.length() <= MAX_UPLOAD_BYTES) break
            if (quality <= MIN_UPLOAD_QUALITY) {
                throw IllegalStateException("这张图压不到 1MB 以内，换一张小一点的试试")
            }
            quality -= 15
        }
        cache
    }







private suspend fun ContentResolver.openImageInput(uri: Uri): InputStream {
    repeat(OPEN_URI_ATTEMPTS) { attempt ->
        openImageStream(uri)?.let { return it }
        if (attempt != OPEN_URI_ATTEMPTS - 1) delay(OPEN_URI_RETRY_DELAY_MS)
    }
    throw IllegalStateException("读不到所选图片（${uri.scheme}://${uri.authority}）")
}

private fun ContentResolver.openImageStream(uri: Uri): InputStream? {
    val stream = try {
        openInputStream(uri)
    } catch (_: Exception) {
        null
    }
    if (stream != null) return stream
    
    
    val fd = try {
        openFileDescriptor(uri, "r")
    } catch (_: Exception) {
        null
    }
    return fd?.let { ParcelFileDescriptor.AutoCloseInputStream(it) }
}


private const val MAX_UPLOAD_BYTES = 1024 * 1024L
private const val MAX_UPLOAD_EDGE = 1500
private const val MIN_UPLOAD_QUALITY = 15


internal const val MAX_UPLOAD_IMAGES = 9


private const val OPEN_URI_ATTEMPTS = 3
private const val OPEN_URI_RETRY_DELAY_MS = 150L


private val ReplyComposerInputHeight = 84.dp


internal const val RecentSmileyLimit = 8


internal val ComposerSubBarHeight = 36.dp


private const val SmileyGridColumns = 6
private const val SmileyGridRows = 7


private val SmileyPackBarHeight = 46.dp


internal val MinKeyboardHeight = 100.dp


internal const val STABLE_KEYBOARD_FRAMES = 6


internal const val HANDOFF_GIVEUP_FRAMES = 30


private val DefaultKeyboardHeight = 280.dp


internal object KeyboardTracker {
    private const val PREF_NAME = "mtf_keyboard_pref"
    private const val KEY_HEIGHT_PX = "keyboard_height_px"
    @Volatile
    private var cachedHeightPx: Int = 0

    fun getInitialHeight(context: Context, density: androidx.compose.ui.unit.Density): Dp {
        if (cachedHeightPx > 0) {
            return with(density) { cachedHeightPx.toDp() }
        }
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val saved = sp.getInt(KEY_HEIGHT_PX, 0)
        if (saved > 0) {
            cachedHeightPx = saved
            return with(density) { saved.toDp() }
        }
        return DefaultKeyboardHeight
    }

    fun save(context: Context, heightPx: Int) {
        if (heightPx > 0 && heightPx != cachedHeightPx) {
            cachedHeightPx = heightPx
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_HEIGHT_PX, heightPx)
                .apply()
        }
    }
}


private val EmojiRowToolbarGap = 2.dp


private val ComposerPublishColor = Color(0xFF97CDAD)







internal val DefaultSmileyCodes =
    listOf("[#呵呵]", "[#滑稽]", "[#哈哈]", "[#吐舌]", "[#啊]", "[#酷]", "[#怒]", "[#开心]")





@Composable
private fun QuickReplyAddDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary
    val canAdd = text.isNotBlank()

    MtfContentDialog(
        title = "添加短语",
        onDismissRequest = onDismiss,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    "例如：谢谢分享！",
                    color = colors.textHint,
                    fontSize = 14.sp,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            textStyle = TextStyle(color = bodyTextColor(), fontSize = 15.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primary,
                unfocusedBorderColor = colors.divider,
                focusedContainerColor = colors.quoteBackground,
                unfocusedContainerColor = colors.quoteBackground,
            ),
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                

                                .background(primary.copy(alpha = if (canAdd) 0.22f else 0.08f))
                        } else {
                            Modifier.background(
                                if (canAdd) primary else primary.copy(alpha = 0.4f)
                            )
                        },
                    )
                    .clickable(enabled = canAdd, onClick = onConfirm),
            ) {
                Text(
                    "添加",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) primary else MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}





@Composable
private fun QuickReplyPanel(
    phrases: List<String>,
    onPick: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            phrases.forEach { phrase ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.quoteBackground)
                        .then(if (LocalIsDarkTheme.current) Modifier else Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(14.dp)))
                        .combinedClickable(
                            onClick = { onPick(phrase) },
                            onLongClick = { onLongPress(phrase) },
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        phrase,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = bodyTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(primary.copy(alpha = if (isDark) 0.22f else 0.12f))
                .clickable(onClick = onAdd),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "添加短语",
                tint = primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
