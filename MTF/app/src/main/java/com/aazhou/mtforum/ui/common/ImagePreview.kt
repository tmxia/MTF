package com.aazhou.mtforum.ui.common

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.aazhou.mtforum.ui.theme.dimImageInDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


private const val MAX_SCALE = 5f


private const val TAP_SLOP = 24f









internal const val PREVIEW_IME_SETTLE_FRAMES = 45







internal suspend fun awaitImeSettled(imeInsets: WindowInsets, density: Density) {
    var zeroFrames = 0
    var frames = 0
    while (frames < PREVIEW_IME_SETTLE_FRAMES) {
        withFrameNanos { }
        frames++
        zeroFrames = if (imeInsets.getBottom(density) == 0) zeroFrames + 1 else 0
        if (zeroFrames >= 2) break
    }
}












@Composable
fun ImagePreview(
    urls: List<Any>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (urls.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, urls.lastIndex)) {
        urls.size
    }
    var busy by remember { mutableStateOf(false) }
    var confirmDownload by remember { mutableStateOf(false) }

    
    val onVerticalSwipe: (Int) -> Unit = { direction ->
        val next = (pagerState.currentPage + direction).coerceIn(0, urls.lastIndex)
        if (next != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(next) }
        }
    }

    BackHandler(enabled = !confirmDownload, onBack = onDismiss)

    
    
    
    
    
    
    
    
    
    Popup(
        onDismissRequest = null,
        properties = PopupProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            excludeFromSystemGesture = false,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { swipeDirectionLock(onVerticalSwipe) },
                ) {
                    ZoomableImage(
                        model = urls[page],
                        active = pagerState.currentPage == page,
                        onTap = onDismiss,
                        onLongPress = {
                            
                            if (urls[pagerState.currentPage] is String) confirmDownload = true
                        },
                    )
                }
            }

            if (urls.size > 1) {
                Text(
                    "${pagerState.currentPage + 1} / ${urls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }

            if (busy) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                )
            }
        }
    }

    if (confirmDownload) {
        val current = urls[pagerState.currentPage]
        MtfConfirmDialog(
            title = "保存到相册",
            message = "是否将当前图片保存到相册？",
            confirmText = "保存",
            onDismissRequest = { confirmDownload = false },
            onConfirm = {
                confirmDownload = false
                if (current is String) {
                    runBusy(scope, { busy = it }, "保存失败") {
                        val bytes = withContext(Dispatchers.IO) { downloadBytes(context, current) }
                        withContext(Dispatchers.IO) { saveToGallery(context, current, bytes) } != null
                    }
                }
            },
        )
    }
}











@Composable
private fun ZoomableImage(
    model: Any,
    active: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var intrinsic by remember(model) { mutableStateOf(IntSize.Zero) }
    var loading by remember(model) { mutableStateOf(true) }
    var failed by remember(model) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(model, active) {
                if (!active) return@pointerInput
                
                
                
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startedZoomed = scale > 1.01f
                        var moved = false
                        var longPressed = false
                        
                        var livePointers = 1
                        
                        
                        val longPressJob = launch {
                            delay(viewConfiguration.longPressTimeoutMillis)
                            if (!moved && !longPressed && livePointers <= 1) {
                                longPressed = true
                                onLongPress()
                            }
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointers = event.changes.count { it.pressed }
                                livePointers = pointers
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val zooming = pointers > 1 || scale > 1.01f
                                if (zooming && (zoom != 1f || pan != Offset.Zero)) {
                                    val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                                    val maxX = boxSize.width * (newScale - 1f) / 2f
                                    val maxY = boxSize.height * (newScale - 1f) / 2f
                                    scale = newScale
                                    offset = Offset(
                                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        (offset.y + pan.y).coerceIn(-maxY, maxY),
                                    )
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                                
                                
                                val downChange = event.changes.firstOrNull { it.id == down.id }
                                if (downChange != null &&
                                    (downChange.position - down.position).getDistance() > viewConfiguration.touchSlop
                                ) {
                                    moved = true
                                }
                                if (event.changes.none { it.pressed }) {
                                    val upPos = event.changes.firstOrNull { it.id == down.id }?.position
                                        ?: down.position
                                    val isTap = !moved && !longPressed &&
                                        (upPos - down.position).getDistance() < TAP_SLOP
                                    if (isTap && (failed || tapInsideImage(upPos, boxSize, intrinsic, scale, offset))) {
                                        onTap()
                                    } else if (startedZoomed && scale <= 1.01f) {
                                        
                                        offset = Offset.Zero
                                    }
                                    break
                                }
                            }
                        } finally {
                            longPressJob.cancel()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onLoading = { loading = true; failed = false },
            onSuccess = { state ->
                loading = false
                failed = false
                val drawable = state.result.drawable
                if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                    intrinsic = IntSize(drawable.intrinsicWidth, drawable.intrinsicHeight)
                }
            },
            onError = { loading = false; failed = true },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                
                .dimImageInDark(),
        )
        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
        if (failed) {
            Text(
                "图片加载失败",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}










private suspend fun PointerInputScope.swipeDirectionLock(onVerticalSwipe: (Int) -> Unit) {
    val slop = DirectionSlop.toPx()
    val distanceThreshold = size.height * 0.22f
    val velocityThreshold = SwipeVelocity.toPx()
    val tracker = VelocityTracker()
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        tracker.resetTracking()
        var axis = 0
        var total = Offset.Zero
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) {
                val velocity = tracker.calculateVelocity().y
                val far = kotlin.math.abs(total.y) > distanceThreshold
                val fast = kotlin.math.abs(velocity) > velocityThreshold
                if (axis == 2 && (far || fast)) {
                    
                    onVerticalSwipe(if (total.y > 0f) -1 else 1)
                }
                break
            }
            
            if (axis == 0 && event.changes.any { it.isConsumed }) break
            var delta = Offset.Zero
            event.changes.forEach { change ->
                if (change.pressed && !change.isConsumed) {
                    delta += change.position - change.previousPosition
                }
            }
            total += delta
            when {
                axis == 0 && kotlin.math.abs(total.y) > slop &&
                    kotlin.math.abs(total.y) > kotlin.math.abs(total.x) * 1.5f -> {
                    axis = 2
                    event.changes.forEach { it.consume() }
                }
                axis == 0 && kotlin.math.abs(total.x) > slop &&
                    kotlin.math.abs(total.x) > kotlin.math.abs(total.y) * 1.5f -> axis = 1
                axis == 2 -> event.changes.forEach { it.consume() }
            }
            event.changes.forEach { change ->
                if (change.pressed) tracker.addPosition(change.uptimeMillis, change.position)
            }
        }
    }
}


private val DirectionSlop = 32.dp


private val SwipeVelocity = 900.dp








private fun tapInsideImage(
    pos: Offset,
    box: IntSize,
    intrinsic: IntSize,
    scale: Float,
    offset: Offset,
): Boolean {
    if (box.width <= 0 || box.height <= 0) return true
    if (intrinsic.width <= 0 || intrinsic.height <= 0) return true
    val scaleFit = minOf(box.width.toFloat() / intrinsic.width, box.height.toFloat() / intrinsic.height)
    val drawW = intrinsic.width * scaleFit
    val drawH = intrinsic.height * scaleFit
    val left = (box.width - drawW) / 2f
    val top = (box.height - drawH) / 2f
    val cx = box.width / 2f
    val cy = box.height / 2f
    val localX = cx + (pos.x - cx - offset.x) / scale
    val localY = cy + (pos.y - cy - offset.y) / scale
    return localX >= left && localX <= left + drawW && localY >= top && localY <= top + drawH
}
