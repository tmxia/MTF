package com.aazhou.mtforum.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


val BottomBarHeight = 56.dp


val BottomBarBottomMargin = 6.dp


val TopBarVerticalPadding = 10.dp







val MtfTopBarHeight: Dp = TopBarVerticalPadding * 2 + 36.dp







@Composable
fun bottomBarContentPadding(): Dp =
    BottomBarHeight + BottomBarBottomMargin +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()


val ReplyBarTopPadding = 6.dp


val ReplyBarCapsuleHeight = 48.dp







@Composable
fun replyBarContentPadding(): Dp =
    ReplyBarTopPadding + ReplyBarCapsuleHeight + BottomBarBottomMargin +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
