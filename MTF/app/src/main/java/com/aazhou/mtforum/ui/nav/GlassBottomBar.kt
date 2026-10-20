package com.aazhou.mtforum.ui.nav

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.aazhou.mtforum.ui.common.BottomBarBottomMargin
import com.aazhou.mtforum.ui.common.BottomBarHeight
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.roundToInt


data class GlassTab(val label: String, val icon: Painter)








@Composable
fun GlassBottomBar(
    backdrop: LayerBackdrop,
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    
    
    val isLightTheme = !LocalIsDarkTheme.current
    val accentColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface
    
    
    val glassBrush = if (isLightTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(0.60f),           
                Color(0xFFE8E8E8).copy(0.40f)      
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E2E2E).copy(0.60f),     
                Color(0xFF121212)      
            )
        )
    }
    


    val tabsCount = tabs.size

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomBarBottomMargin),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidth = constraints.maxWidth.toFloat() / tabsCount

        var currentIndex by remember { mutableIntStateOf(selectedIndex) }
        
        
        val selectedIndexState = rememberUpdatedState(selectedIndex)
        val onSelectState = rememberUpdatedState(onSelect)
        val onReselectState = rememberUpdatedState(onReselect)

        
        LaunchedEffect(selectedIndex) { currentIndex = selectedIndex }

        
        val barGesture = Modifier.pointerInput(tabWidth, tabsCount) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun posToIndex(x: Float): Int =
                    (x / tabWidth - 0.5f).roundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = posToIndex(down.position.x)
                var pointer = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointer }
                    if (change == null || change.changedToUpIgnoreConsumed()) break
                    currentIndex = posToIndex(change.position.x)
                    change.consume()
                    pointer = change.id
                }
                val idx = currentIndex
                onSelectState.value(idx)
                
                if (idx == selectedIndexState.value) onReselectState.value(idx)
            }
        }

        Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (isLightTheme) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(22.dp) },
                                effects = {
                                    vibrancy()
                                    blur(64f.dp.toPx())
                                },
                                onDrawSurface = { drawRect(brush = glassBrush) },
                            )
                        } else {
                            Modifier.background(Color(0xFF1E1E1E)) 
                        }
                    )
                    .then(barGesture)
                .height(BottomBarHeight)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val tint = if (index == currentIndex) accentColor else contentColor
                GlassTabItem {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint)
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.GlassTabItem(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxHeight()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
