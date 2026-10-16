package com.aazhou.mtforum.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.aazhou.mtforum.ui.common.container
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aazhou.mtforum.data.model.PostQuote
import com.aazhou.mtforum.image.GridImageSize
import com.aazhou.mtforum.image.ImageAspectCache
import com.aazhou.mtforum.image.imageAspectRatio
import com.aazhou.mtforum.image.postImageRequest
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark
import com.aazhou.mtforum.ui.theme.accentTextColor
import com.aazhou.mtforum.ui.theme.MtfColors


private val SmileySize = 20.sp


@Composable
fun PostContent(
    html: String,
    quote: PostQuote? = null,
    onImageClick: ((urls: List<String>, index: Int) -> Unit)? = null,
    onVideoClick: ((url: String) -> Unit)? = null,
    
    onLinkClick: ((url: String) -> Unit)? = null,
    
    onAttachmentClick: ((fileName: String, url: String) -> Unit)? = null,
    pageImages: List<String> = emptyList(),
    bodyStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier,
    





    selectable: Boolean = true,
) {
    if (selectable) {
        SelectionContainer {
            PostContentBody(html, quote, onImageClick, onVideoClick, onLinkClick, onAttachmentClick, pageImages, bodyStyle, modifier)
        }
    } else {
        PostContentBody(html, quote, onImageClick, onVideoClick, onLinkClick, onAttachmentClick, pageImages, bodyStyle, modifier)
    }
}

@Composable
private fun PostContentBody(
    html: String,
    quote: PostQuote?,
    onImageClick: ((urls: List<String>, index: Int) -> Unit)?,
    onVideoClick: ((url: String) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onAttachmentClick: ((fileName: String, url: String) -> Unit)?,
    pageImages: List<String>,
    bodyStyle: TextStyle,
    modifier: Modifier,
) {
    val blocks = remember(html) { parseContentBlocks(html) }
    Column(modifier) {
        if (quote != null && quote.text.isNotBlank()) QuoteCard(quote)
        for (block in blocks) {
            ContentBlockView(
                block = block,
                bodyStyle = bodyStyle,
                onImageClick = onImageClick,
                onVideoClick = onVideoClick,
                onLinkClick = onLinkClick,
                onAttachmentClick = onAttachmentClick,
                pageImages = pageImages,
            )
        }
    }
}


@Composable
private fun ContentBlockView(
    block: ContentBlock,
    bodyStyle: TextStyle,
    onImageClick: ((urls: List<String>, index: Int) -> Unit)?,
    onVideoClick: ((url: String) -> Unit)?,
    onLinkClick: ((url: String) -> Unit)?,
    onAttachmentClick: ((fileName: String, url: String) -> Unit)?,
    pageImages: List<String>,
) {
    val colors = LocalMtfColors.current
    when (block) {
        is ContentBlock.Quote -> {
            Text(
                text = AnnotatedString(block.text),
                style = bodyStyle.copy(fontSize = 13.sp),
                color = colors.quoteContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.quoteBackground)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        is ContentBlock.Hidden -> HiddenContentCard(block) { inner ->
            ContentBlockView(
                block = inner,
                bodyStyle = bodyStyle,
                onImageClick = onImageClick,
                onVideoClick = onVideoClick,
                onLinkClick = onLinkClick,
                onAttachmentClick = onAttachmentClick,
                pageImages = pageImages,
            )
        }
        is ContentBlock.Text -> {
            val text = block.text.trim('\n')
            if (text.isNotBlank()) {
                val linkColor = accentTextColor()
                val codeBg = colors.quoteBackground
                val style = remember(block, bodyStyle) { block.derivedStyle(bodyStyle) }
                
                
                val linkClick by rememberUpdatedState(onLinkClick)
                val body = remember(
                    text,
                    block.smiles,
                    block.spans,
                    linkColor,
                    codeBg,
                ) {
                    buildBodyText(
                        text = text,
                        smiles = block.smiles,
                        spans = block.spans,
                        baseFontSize = style.fontSize,
                        linkColor = linkColor,
                        codeBackground = codeBg,
                        onLinkClick = linkClick,
                    )
                }
                Text(
                    text = body,
                    inlineContent = remember(block.smiles) { smileyInlineContent(block.smiles) },
                    style = style,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
        is ContentBlock.Image -> SingleImage(
            url = block.url,
            onImageClick = onImageClick?.let { click ->
                
                val urls = pageImages.ifEmpty { listOf(block.url) }
                val index = urls.indexOf(block.url).coerceAtLeast(0)
                return@let { click(urls, index) }
            },
            modifier = Modifier.padding(vertical = 4.dp),
        )
        is ContentBlock.ImageGroup -> ImageGrid(
            urls = block.urls,
            onImageClick = onImageClick?.let { click ->
                { url ->
                    
                    val urls = pageImages.ifEmpty { block.urls }
                    val index = urls.indexOf(url).coerceAtLeast(0)
                    click(urls, index)
                }
            },
        )
        is ContentBlock.Video -> InlineVideo(
            url = block.url,
            poster = block.poster,
            onPlay = onVideoClick?.let { click -> { click(block.url) } },
        )
        is ContentBlock.Attachment -> PostAttachment(
            block = block,
            onClick = onAttachmentClick,
        )
        is ContentBlock.Table -> PostTable(block, bodyStyle)
        is ContentBlock.CodeBlock -> PostCodeBlock(block, bodyStyle)
    }
}





@Composable
private fun HiddenContentCard(
    block: ContentBlock.Hidden,
    inner: @Composable (ContentBlock) -> Unit,
) {
    val colors = LocalMtfColors.current
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.31f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .drawBehind {
                drawRect(color = accent, size = Size(5.dp.toPx(), size.height))
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = block.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
            ),
            color = bodyTextColor(),
        )
        for (child in block.blocks) inner(child)
    }
}








internal fun buildBodyText(
    text: String,
    smiles: Map<Char, String>,
    spans: List<StyleSpan>,
    baseFontSize: TextUnit,
    linkColor: Color,
    codeBackground: Color,
    onLinkClick: ((url: String) -> Unit)? = null,
): AnnotatedString {
    if (spans.isEmpty()) return text.toAnnotatedString(smiles)
    return buildAnnotatedString {
        text.forEachIndexed { index, char ->
            append(char)
            if (smiles.containsKey(char)) {
                addStringAnnotation(TAG_SMILEY, char.toString(), index, index + 1)
            }
        }
        for (span in spans) {
            val start = span.start.coerceIn(0, length)
            val end = span.end.coerceIn(start, length)
            if (end <= start) continue
            val decoration = when {
                span.strikethrough -> TextDecoration.LineThrough
                span.underline -> TextDecoration.Underline
                else -> null
            }
            
            
            val scale = span.fontSizeScale * headingScale(span.headingLevel)
            val spanStyle = SpanStyle(
                fontSize = if (scale != 1f) baseFontSize * scale else TextUnit.Unspecified,
                fontWeight = if (span.bold || span.headingLevel > 0) FontWeight.Medium else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                textDecoration = decoration,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                background = if (span.code) codeBackground else Color.Unspecified,
            )
            if (spanStyle != SpanStyle()) addStyle(spanStyle, start, end)
            span.linkUrl?.let { url ->
                addLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ),
                        linkInteractionListener = onLinkClick?.let { click ->
                            LinkInteractionListener { click(url) }
                        },
                    ),
                    start,
                    end,
                )
            }
        }
    }
}


private fun ContentBlock.Text.derivedStyle(bodyStyle: TextStyle): TextStyle {
    if (spans.isEmpty()) return bodyStyle
    val align = spans.firstNotNullOfOrNull { it.align }
    return bodyStyle.copy(
        textAlign = when (align) {
            CellAlign.Center -> TextAlign.Center
            CellAlign.End -> TextAlign.End
            CellAlign.Start -> TextAlign.Start
            null -> TextAlign.Unspecified
        },
    )
}

private fun headingScale(level: Int): Float = when (level) {
    1 -> 1.6f
    2 -> 1.4f
    3 -> 1.25f
    4 -> 1.15f
    5 -> 1.08f
    else -> 1f
}


@Composable
private fun PostTable(table: ContentBlock.Table, bodyStyle: TextStyle) {
    val colors = LocalMtfColors.current
    val cellStyle = bodyStyle.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val shape = RoundedCornerShape(8.dp)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellHorizontalPadding = 10.dp
    val dividerThickness = 0.6.dp
    val minColumn = 36.dp

    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        val columns = maxOf(table.header?.size ?: 0, table.rows.maxOfOrNull { it.size } ?: 0)
        if (columns == 0) return@BoxWithConstraints
        
        val natural = List(columns) { column ->
            val texts = buildList {
                table.header?.getOrNull(column)?.let(::add)
                table.rows.forEach { row -> row.getOrNull(column)?.let(::add) }
            }
            val maxPx = texts.maxOfOrNull { text ->
                measurer.measure(AnnotatedString(text), cellStyle).size.width
            } ?: 0
            with(density) { maxPx.toDp() }
        }
        val fixed = cellHorizontalPadding * 2 * columns + dividerThickness * (columns - 1)
        val budget = (maxWidth - fixed).coerceAtLeast(minColumn * columns)
        val widths = allocateColumnWidths(natural, budget, minColumn)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(dividerThickness, colors.divider, shape),
        ) {
            table.header?.let { header ->
                PostTableRow(header, widths, cellStyle, FontWeight.Medium, colors, headerTab = true)
                if (table.rows.isNotEmpty()) {
                    HorizontalDivider(color = colors.divider, thickness = dividerThickness)
                }
            }
            table.rows.forEachIndexed { rowIndex, row ->
                PostTableRow(row, widths, cellStyle, null, colors, headerTab = false)
                if (rowIndex < table.rows.lastIndex) {
                    HorizontalDivider(color = colors.divider, thickness = dividerThickness)
                }
            }
        }
    }
}







@Composable
private fun PostCodeBlock(block: ContentBlock.CodeBlock, bodyStyle: TextStyle) {
    val colors = LocalMtfColors.current
    val codeStyle = bodyStyle.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    val numberStyle = codeStyle.copy(fontSize = 11.sp)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    val indent = remember(block.lines.size, numberStyle) {
        with(density) {
            (measurer.measure(
                AnnotatedString("%02d ".format(block.lines.size)),
                numberStyle,
            ).size.width.toDp() + 8.dp).toSp()
        }
    }
    val wrappedStyle = remember(codeStyle, indent) {
        codeStyle.merge(
            ParagraphStyle(textIndent = TextIndent(firstLine = 0.sp, restLine = indent)),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.quoteBackground)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentTextColor()),
        )
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            block.lines.forEachIndexed { index, line ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.textHint)) {
                            append("%02d ".format(index + 1))
                        }
                        append(line)
                    },
                    style = wrappedStyle,
                    color = bodyTextColor(),
                )
            }
        }
    }
}







private fun allocateColumnWidths(natural: List<Dp>, budget: Dp, min: Dp): List<Dp> {
    if (natural.isEmpty()) return emptyList()
    val total = natural.fold(0f) { acc, width -> acc + width.value }
    val raw = if (total <= 0f) {
        List(natural.size) { budget.value / natural.size }
    } else {
        natural.map { budget.value * it.value / total }
    }
    val widths = raw.map { maxOf(it, min.value) }.toMutableList()
    var overflow = widths.sum() - budget.value
    var guard = 0
    while (overflow > 0.5f && guard < 64) {
        val index = widths.indices.maxByOrNull { widths[it] } ?: break
        val reducible = (widths[index] - min.value).coerceAtMost(overflow)
        if (reducible <= 0f) break
        widths[index] -= reducible
        overflow -= reducible
        guard++
    }
    return widths.map { it.dp }
}


@Composable
private fun PostTableRow(
    cells: List<String>,
    widths: List<Dp>,
    cellStyle: TextStyle,
    weight: FontWeight?,
    colors: MtfColors,
    headerTab: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (headerTab) Modifier.background(colors.quoteBackground) else Modifier)
            .height(IntrinsicSize.Min),
    ) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = cell,
                style = cellStyle.copy(fontWeight = weight),
                color = bodyTextColor(),
                modifier = Modifier
                    .width(widths.getOrElse(index) { widths.last() })
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            if (index < cells.size - 1) {
                VerticalDivider(color = colors.divider, thickness = 0.6.dp)
            }
        }
    }
}


@Composable
private fun SingleImage(url: String, onImageClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = LocalMtfColors.current
    val context = LocalContext.current
    
    
    val cachedAspect = ImageAspectCache.aspectOf(url)
    var aspectRatio by remember(url) { mutableStateOf(cachedAspect ?: 4f / 3f) }
    
    
    var loadFailed by remember(url) { mutableStateOf(false) }
    
    LaunchedEffect(url, cachedAspect) {
        if (cachedAspect != null && aspectRatio != cachedAspect) aspectRatio = cachedAspect
    }
    if (loadFailed) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        
        val maxImageWidth = maxWidth * 0.6f
        val maxImageHeight = maxWidth * 0.9f
        if (aspectRatio >= 1f) {
            
            val sizeModifier =
                if (aspectRatio >= maxImageWidth / maxImageHeight) {
                    Modifier.width(maxImageWidth).aspectRatio(aspectRatio)
                } else {
                    Modifier.height(maxImageHeight).aspectRatio(aspectRatio)
                }
            AsyncImage(
                model = postImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val drawable = state.result.drawable
                    imageAspectRatio(drawable.intrinsicWidth, drawable.intrinsicHeight)?.let {
                        aspectRatio = it
                    }
                },
                onError = { state ->
                    loadFailed = true
                    context.container().logger.error(
                        "Image",
                        "正文单图加载失败 url=$url",
                        state.result.throwable,
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .then(sizeModifier)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.quoteBackground)
                    .dimImageInDark()
                    .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                    .then(
                        if (onImageClick != null) Modifier.clickable { onImageClick() } else Modifier
                    ),
            )
        } else {
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(maxImageWidth)
                    .height(maxImageWidth * 0.75f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.quoteBackground)
                    .dimImageInDark()
                    .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                    .then(
                        if (onImageClick != null) Modifier.clickable { onImageClick() } else Modifier
                    ),
            ) {
                AsyncImage(
                    model = postImageRequest(url),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val drawable = state.result.drawable
                        imageAspectRatio(drawable.intrinsicWidth, drawable.intrinsicHeight)?.let {
                            aspectRatio = it
                        }
                    },
                    onError = { state ->
                        loadFailed = true
                        context.container().logger.error(
                            "Image",
                            "正文单图加载失败 url=$url",
                            state.result.throwable,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}


@Composable
private fun ImageGrid(urls: List<String>, onImageClick: ((String) -> Unit)?) {
    val colors = LocalMtfColors.current
    val context = LocalContext.current
    
    
    val failedUrls = remember(urls) { mutableStateMapOf<String, Boolean>() }
    val liveUrls = urls.filterNot { failedUrls[it] == true }
    if (liveUrls.isEmpty()) return
    val columns = if (liveUrls.size == 2 || liveUrls.size == 4) 2 else 3
    val cells = if (liveUrls.size > 9) liveUrls.take(9) else liveUrls
    val overflow = liveUrls.size - 9
    val cellRatio = if (columns == 2) 0.85f else 1f
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        cells.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEachIndexed { columnIndex, url ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f / cellRatio)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.quoteBackground)
                            .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                            .then(
                                if (onImageClick != null) Modifier.clickable { onImageClick(url) } else Modifier
                            ),
                    ) {
                        AsyncImage(
                            model = postImageRequest(url, maxSize = GridImageSize),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onError = { state ->
                                failedUrls[url] = true
                                context.container().logger.error(
                                    "Image",
                                    "正文图组加载失败 url=$url",
                                    state.result.throwable,
                                )
                            },
                            modifier = Modifier.fillMaxSize().dimImageInDark(),
                        )
                        
                        if (overflow > 0 && rowIndex * columns + columnIndex == 8) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.47f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "+$overflow",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}





@Composable
private fun PostAttachment(
    block: ContentBlock.Attachment,
    onClick: ((fileName: String, url: String) -> Unit)?,
) {
    val colors = LocalMtfColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.quoteBackground)
            .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick(block.fileName, block.url) } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        ) {
            Icon(
                painter = painterResource(com.aazhou.mtforum.R.drawable.ic_download),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = block.fileName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                color = bodyTextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!block.sizeText.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = block.sizeText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}





@Composable
private fun InlineVideo(url: String, poster: String?, onPlay: (() -> Unit)?) {
    val colors = LocalMtfColors.current
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val maxVideoWidth = maxWidth * 0.92f
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(maxVideoWidth)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.quoteBackground)
                .then(if (onPlay != null) Modifier.clickable { onPlay() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (poster != null) {
                AsyncImage(
                    model = postImageRequest(poster),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { state ->
                        context.container().logger.error(
                            "Image",
                            "视频封面加载失败 url=$poster",
                            state.result.throwable,
                        )
                    },
                    modifier = Modifier.fillMaxSize().dimImageInDark(),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放视频",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}


@Composable
private fun QuoteCard(quote: PostQuote) {
    val colors = LocalMtfColors.current
    val accentColor = accentTextColor()    
    var lastLineRight by remember(quote.text) { mutableStateOf(0f) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (!quote.author.isNullOrBlank()) {
            Text(
                text = "回复 ${quote.author}：",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontStyle = FontStyle.Italic,
                color = colors.textHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Medium)) {
                        append("原文 ")
                    }
                    append(quote.text)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.quoteContent,
                onTextLayout = { layout ->
                    lastLineRight = layout.getLineRight(layout.lineCount - 1)
                },
            )
            Spacer(Modifier.height(6.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            ) {
                drawLine(
                    color = colors.divider,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(lastLineRight, size.height / 2f),
                    strokeWidth = size.height,
                )
            }
            if (!quote.time.isNullOrBlank()) {
                Text(
                    text = quote.time,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}







internal fun String.toAnnotatedString(smiles: Map<Char, String>): AnnotatedString {
    if (smiles.isEmpty()) return AnnotatedString(this)
    return buildAnnotatedString {
        this@toAnnotatedString.forEachIndexed { index, char ->
            append(char)
            if (smiles.containsKey(char)) {
                addStringAnnotation(TAG_SMILEY, char.toString(), index, index + 1)
            }
        }
    }
}

private const val TAG_SMILEY = "androidx.compose.foundation.text.inlineContent"


internal fun smileyInlineContent(smiles: Map<Char, String>): Map<String, InlineTextContent> =
    smiles.entries.associate { (placeholder, url) ->
        placeholder.toString() to InlineTextContent(
            Placeholder(
                width = SmileySize,
                height = SmileySize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            ),
        ) {
            AsyncImage(
                model = smallImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                
                
                modifier = Modifier.fillMaxSize().dimImageInDark(),
            )
        }
    }
