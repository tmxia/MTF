package com.aazhou.mtforum.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aazhou.mtforum.R
import com.aazhou.mtforum.ui.theme.LocalIsDarkTheme







@Composable
internal fun accountIconModel(logout: Boolean): String {
    val context = LocalContext.current
    val res = if (LocalIsDarkTheme.current) {
        if (logout) R.raw.anim_logout_dark else R.raw.anim_login_dark
    } else {
        if (logout) R.raw.anim_logout else R.raw.anim_login
    }
    return "android.resource://${context.packageName}/$res"
}
