package com.aazhou.mtforum.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.image.GridImageSize
import com.aazhou.mtforum.image.postImageRequest
import com.aazhou.mtforum.image.smallImageRequest
import com.aazhou.mtforum.ui.common.container
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.MtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor
import com.aazhou.mtforum.ui.theme.dimImageInDark









@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ThreadCard(
    thread: Thread,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onImageClick: (List<String>, Int) -> Unit,
    onUserClick: (String) -> Unit,
    showForumName: Boolean = true,
) {
    val colors = LocalMtfColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            
            
            
            .padding(horizontal = 10.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = smallImageRequest(thread.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .dimImageInDark()
                    .userClick(thread.authorUid, onUserClick),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    thread.authorName ?: "匿名",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Medium,
                    color = bodyTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.userClick(thread.authorUid, onUserClick),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.5.dp)) {
                    Text(
                        thread.timeText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary,
                    )
                }
            }
            
            
            
            
            
            
            
            if (showForumName && !thread.forumName.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                ForumNameChip(
                    thread.forumName,
                    colors,
                    Modifier.align(Alignment.Top).widthIn(max = 96.dp),
                )
            }
        }
        Text(
            thread.subject,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
            fontWeight = FontWeight.Medium,
            color = bodyTextColor(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        if (!thread.excerpt.isNullOrBlank()) {
            Text(
                thread.excerpt,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        CardImages(thread.imageUrls, onImageClick)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 11.dp),
        ) {
            StatItem(painterResource(R.drawable.ic_card_view), formatCount(thread.viewCount))
            Spacer(Modifier.width(24.dp))
            StatItem(painterResource(R.drawable.ic_reply), formatCount(thread.replyCount))
            Spacer(Modifier.width(24.dp))
            StatItem(painterResource(R.drawable.ic_like_thumb), formatCount(thread.likeCount))
        }
    }
}


@Composable
private fun ForumNameChip(name: String, colors: MtfColors, modifier: Modifier = Modifier) {
    Text(
        name,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .border(
                0.1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.69f),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}




@Composable
private fun CardSingleImage(url: String, onFailed: () -> Unit, onClick: () -> Unit) {
    val colors = LocalMtfColors.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(10.dp)
    AsyncImage(
        model = postImageRequest(url),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onError = { state ->
            onFailed()
            val msg = state.result.throwable?.message.orEmpty()
            if (msg.contains("HTTP 404") || msg.contains("HTTP 514")) {
                
                context.container().logger.info("Image", "配图加载跳过(HTTP 4xx): $url")
            } else {
                context.container().logger.error("Image", "帖子卡片配图加载失败 url=$url", state.result.throwable)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(155.dp)
            .clip(shape)
            .background(colors.quoteBackground)
            .dimImageInDark()
            .border(1.dp, colors.divider, shape)
            .clickable(onClick = onClick),
    )
}






@Composable
private fun CardImageGrid(
    urls: List<String>,
    failedUrls: MutableMap<String, Boolean>,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val colors = LocalMtfColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (urls.size) {
            2 -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                urls.forEachIndexed { index, url ->
                    CardImage(
                        url,
                        cardCellModifier(colors, height = 130.dp, shape = RoundedCornerShape(6.dp)) {
                            onImageClick(urls, index)
                        },
                        onFailed = { failedUrls[url] = true },
                    )
                }
            }

            3 -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                urls.forEachIndexed { index, url ->
                    CardImage(
                        url,
                        cardCellModifier(colors, height = 100.dp, shape = RoundedCornerShape(6.dp)) {
                            onImageClick(urls, index)
                        },
                        onFailed = { failedUrls[url] = true },
                    )
                }
            }

            4 -> for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (column in 0 until 2) {
                        val index = row * 2 + column
                        CardImage(
                            urls[index],
                            cardCellModifier(colors, height = 98.dp, shape = RoundedCornerShape(6.dp)) {
                                onImageClick(urls, index)
                            },
                            onFailed = { failedUrls[urls[index]] = true },
                        )
                    }
                }
            }

            else -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                urls.take(3).forEachIndexed { position, url ->
                    val showBadge = position == 2 && urls.size > 3
                    val modifier = cardCellModifier(colors, height = 115.dp, shape = RoundedCornerShape(8.dp)) {
                        onImageClick(urls, position)
                    }
                    if (showBadge) {
                        Box(modifier = modifier) {
                            CardImage(url, Modifier.fillMaxSize(), onFailed = { failedUrls[url] = true })
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .background(Color(0x99000000), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    "${urls.size}图",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                )
                            }
                        }
                    } else {
                        CardImage(url, modifier, onFailed = { failedUrls[url] = true })
                    }
                }
            }
        }
    }
}


private fun RowScope.cardCellModifier(
    colors: MtfColors,
    height: Dp,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
): Modifier = Modifier
    .weight(1f)
    .height(height)
    .clip(shape)
    .background(colors.quoteBackground)
    .border(1.dp, colors.divider, shape)
    .clickable(onClick = onClick)


@Composable
private fun CardImage(url: String, modifier: Modifier, onFailed: () -> Unit) {
    val context = LocalContext.current
    AsyncImage(
        model = postImageRequest(url, maxSize = GridImageSize),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onError = { state ->
            onFailed()
            val msg = state.result.throwable?.message.orEmpty()
            if (msg.contains("HTTP 404") || msg.contains("HTTP 514")) {
                
                context.container().logger.info("Image", "配图加载跳过(HTTP 4xx): $url")
            } else {
                context.container().logger.error("Image", "帖子卡片配图加载失败 url=$url", state.result.throwable)
            }
        },
        modifier = modifier.dimImageInDark(),
    )
}




@Composable
private fun CardImages(
    urls: List<String>,
    onImageClick: (List<String>, Int) -> Unit,
) {
    
    
    val failedUrls = remember(urls) { mutableStateMapOf<String, Boolean>() }
    val liveUrls = urls.filterNot { failedUrls[it] == true }
    when (liveUrls.size) {
        0 -> Unit
        1 -> CardSingleImage(liveUrls.first(), onFailed = { failedUrls[liveUrls.first()] = true }) {
            onImageClick(liveUrls, 0)
        }
        else -> CardImageGrid(liveUrls, failedUrls, onImageClick)
    }
}


@Composable
private fun StatItem(icon: Painter, count: String?) {
    val colors = LocalMtfColors.current
    if (count.isNullOrBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.iconSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            count,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = colors.textSecondary,
        )
    }
}


private fun formatCount(raw: String?): String? {
    val count = raw?.trim()?.toIntOrNull() ?: return raw?.takeIf { it.isNotBlank() }
    return when {
        count >= 10_000 -> "${count / 10_000}w"
        count >= 1_000 -> "${count / 1_000}k"
        else -> count.toString()
    }
}






private fun Modifier.userClick(
    uid: String?,
    onUserClick: (String) -> Unit,
): Modifier =
    if (uid.isNullOrBlank()) this else clickable { onUserClick(uid) }
