package com.aazhou.mtforum.ui.screen.verify

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aazhou.mtforum.ui.common.MtfTopBarHeight
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.network.WafSolver








@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WafVerificationScreen(url: String, onFinished: (String?) -> Unit) {
    val reported = remember { mutableStateOf(false) }

    fun reportIfSolved() {
        if (reported.value) return
        val cookie = CookieManager.getInstance().getCookie(url)
        if (cookie != null && cookie.contains(WafSolver.COOKIE_NAME)) {
            reported.value = true
            onFinished(cookie)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("人机验证") },
                actions = {
                    TextButton(onClick = { onFinished(null) }) { Text("取消") }
                    TextButton(onClick = { onFinished(CookieManager.getInstance().getCookie(url)) }) {
                        Text("完成")
                    }
                },
                expandedHeight = MtfTopBarHeight,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                "正在加载站点验证，通过后会自动继续。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = HttpClient.MOBILE_UA
                        CookieManager.getInstance().setAcceptCookie(true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                reportIfSolved()
                            }
                        }
                        loadUrl(url)
                    }
                },
            )
        }
    }
}
