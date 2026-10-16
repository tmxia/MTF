package com.aazhou.mtforum.ui.common

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics












@Composable
internal fun Modifier.maskAccessibility(): Modifier {
    val view = LocalView.current
    
    SideEffect { view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    return this.clearAndSetSemantics {}
}
