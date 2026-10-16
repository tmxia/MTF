package com.aazhou.mtforum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aazhou.mtforum.ui.nav.AppRoot
import com.aazhou.mtforum.ui.theme.MtfTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    
    private var openNotice by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val themeStore = (application as MtfApp).container.themeStore
        setContent {
            val scope = rememberCoroutineScope()
            
            val customColor by themeStore.customColor
                .collectAsStateWithLifecycle(initialValue = null)
            
            val darkOverride by themeStore.darkOverride.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = darkOverride ?: isSystemInDarkTheme()
            val dimImagesInDark by themeStore.dimImagesInDark
                .collectAsStateWithLifecycle(initialValue = true)
            
            
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                MtfTheme(
                    customColor = customColor,
                    darkTheme = darkTheme,
                    dimImagesInDark = dimImagesInDark,
                ) {
                    AppRoot(
                        onToggleTheme = {
                            scope.launch { themeStore.setDarkOverride(!darkTheme) }
                        },
                        openNotice = openNotice,
                    )
                }
            }
        }
    }

    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOTICE, false) == true) openNotice = true
    }

    companion object {
        
        const val EXTRA_OPEN_NOTICE = "open_notice"
    }
}
