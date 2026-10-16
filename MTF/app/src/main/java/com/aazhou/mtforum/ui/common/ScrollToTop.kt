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


val LazyListState.isAtTop: Boolean
    get() = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0








@Composable
fun LazyListState.reportViewportAtTop(onChange: (Boolean) -> Unit) {
    LaunchedEffect(this) {
        snapshotFlow { isAtTop }
            .distinctUntilChanged()
            .collect(onChange)
    }
}


















suspend fun LazyListState.scrollToNewest() {
    scrollToItem(0)
}

suspend fun ScrollState.scrollToTop() {
    scrollTo(0)
}










