package com.aazhou.mtforum.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.lerp









@Composable
fun SwipeTabIndicator(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
    color: Color,
) {
    val page = pagerState.currentPage
    val fraction = pagerState.currentPageOffsetFraction
    val current = tabPositions[page]
    val from: TabPosition
    val to: TabPosition
    val progress: Float
    if (fraction >= 0f) {
        
        from = current
        to = tabPositions.getOrNull(page + 1) ?: current
        progress = fraction
    } else {
        
        from = tabPositions.getOrNull(page - 1) ?: current
        to = current
        progress = fraction + 1f
    }
    TabRowDefaults.SecondaryIndicator(
        Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset { IntOffset(lerp(from.left, to.left, progress).roundToPx(), 0) }
            .width(lerp(from.width, to.width, progress)),
        color = color,
    )
}
