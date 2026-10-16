package com.aazhou.mtforum.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.distinctUntilChanged






fun Modifier.onDoubleTapToTop(onDoubleTap: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }










suspend fun LazyListState.scrollToTop() {
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) return
    scrollToItem(0)
}

/** 视口是否贴在顶部（首条完整可见且无偏移）。 */
val LazyListState.isAtTop: Boolean
    get() = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

/**
 * 把「视口是否贴顶」持续回报给数据层（仅在真正变化时回调）。
 *
 * 归位判据只有在数据替换**之前**读才有意义：替换后 LazyList 已按 item key 把旧首条锚回
 * 原处，[LazyListState.firstVisibleItemIndex] 变成「头部新插了几条」，不再代表用户位置。
 * 故这个真值只能由界面在替换前一路记着，供数据层在改写状态时取用。
 */
@Composable
fun LazyListState.reportViewportAtTop(onChange: (Boolean) -> Unit) {
    LaunchedEffect(this) {
        snapshotFlow { isAtTop }
            .distinctUntilChanged()
            .collect(onChange)
    }
}










/**
 * 列表内容整体替换后回到最新。
 *
 * 「要不要跳」已由数据层在替换前判定（替换前视口贴顶才置位归位信号），到这里无条件执行：
 * 顶部时不能提前返回——下拉刷新正是视口贴顶时发起，而下拉想看的恰是新插在头部的那几条；
 * LazyList 会把旧首条按 key 锚定回原位，新内容被顶到视口上方，只有 [LazyListState.scrollToItem]
 * 才能把锚点丢掉，提前返回就会跳过它，表现为「刷完还得手动往上滑」。
 */
suspend fun LazyListState.scrollToNewest() {
    scrollToItem(0)
}

suspend fun ScrollState.scrollToTop() {
    scrollTo(0)
}










