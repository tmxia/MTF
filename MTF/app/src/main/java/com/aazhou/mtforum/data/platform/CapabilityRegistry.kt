package com.aazhou.mtforum.data.platform

import android.os.Build













object CapabilityRegistry {

    enum class Capability {
        
        WEBVIEW,
    }

    private val overrides = mutableMapOf<Capability, Boolean>()

    @Synchronized
    fun isAvailable(cap: Capability): Boolean =
        overrides.getOrPut(cap) { probe(cap) }

    @Synchronized
    fun markUnavailable(cap: Capability) {
        overrides[cap] = false
    }

    @Synchronized
    fun reset(cap: Capability? = null) {
        if (cap == null) overrides.clear() else overrides.remove(cap)
    }

    private fun probe(cap: Capability): Boolean = when (cap) {
        Capability.WEBVIEW -> runCatching {
            Class.forName("android.webkit.WebView", false, CapabilityRegistry::class.java.classLoader)
            true
        }.getOrDefault(false)
    }
}
