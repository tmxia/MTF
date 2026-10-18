package com.aazhou.mtforum.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aazhou.mtforum.AppContainer
import com.aazhou.mtforum.R
import com.aazhou.mtforum.data.model.HistoryItem
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.aazhou.mtforum.ui.theme.LocalMtfColors
import com.aazhou.mtforum.ui.theme.bodyTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenThread: (String) -> Unit,
    onOpenForum: (String, String) -> Unit = { _, _ -> },
) {
    val viewModel: HistoryViewModel = com.aazhou.mtforum.ui.common.appViewModel {
        HistoryViewModel(it.historyStore)
    }
    val items by viewModel.items.collectAsStateWithLifecycle()
    val colors = LocalMtfColors.current

    Scaffold(
        containerColor = colors.background,
        contentColor = bodyTextColor(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.topBar),
                title = {
                    Text(
                        "浏览历史",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = bodyTextColor(),
                    )
                },
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
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clear() }) {
                            Text("清空", color = bodyTextColor())
                        }
                    }
                },
                expandedHeight = MtfTopBarHeight,
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无浏览记录", color = colors.textHint, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(items, key = { it.tid + it.type }) { item ->
                    HistoryCard(
                        item = item,
                        onClick = {
                            if (item.type == "forum") {
                                onOpenForum(item.tid, item.title)
                            } else {
                                onOpenThread(item.tid)
                            }
                        },
                        onDelete = { viewModel.remove(item.tid, item.type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalMtfColors.current
    val isDark = LocalIsDarkTheme.current

    val isForum = item.type == "forum"
    
    val iconRes = if (isForum) R.drawable.ic_home else R.drawable.ic_favorite_outline
    val iconTint = if (isForum) colors.iconSecondary else MaterialTheme.colorScheme.primary
    val iconBg = if (isForum) colors.quoteBackground else MaterialTheme.colorScheme.primaryContainer

    Column(
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
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = if (isForum) "板块" else "帖子",
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Medium,
                    color = bodyTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildString {
                    if (!isForum && item.author.isNotEmpty()) append(item.author)
                    if (item.forumName?.isNotEmpty() == true) {
                        if (isNotEmpty()) append(" · ")
                        append(item.forumName)
                    }
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    formatTime(item.viewedAt),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textHint,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = "删除",
                    tint = colors.textHint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return ""
    val diff = System.currentTimeMillis() - ts
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        diff < 30 * day -> "${diff / day} 天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(ts))
        }
    }
}
