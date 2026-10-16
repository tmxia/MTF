package com.aazhou.mtforum.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull





object ForumLinkResolver {

    
    private val THREAD_PATH = Regex("thread-(\\d+)-\\d+-\\d+\\.html$")

    
    private val SPACE_UID_PATH = Regex("space-uid-(\\d+)\\.html$")

    
    fun siteThreadId(url: String?, siteHost: String): String? {
        if (url.isNullOrBlank()) return null
        val target = absolutize(url, siteHost) ?: return null
        if (target.host != siteHost) return null
        THREAD_PATH.find(target.encodedPath)?.let { return it.groupValues[1] }
        
        if (target.encodedPath.endsWith("forum.php")) {
            return target.queryParameter("tid")?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        }
        return null
    }

    






    fun siteUserId(url: String?, siteHost: String): String? {
        if (url.isNullOrBlank()) return null
        val target = absolutize(url, siteHost) ?: return null
        if (target.host != siteHost) return null
        SPACE_UID_PATH.find(target.encodedPath)?.let { return it.groupValues[1] }
        if (target.encodedPath.endsWith("home.php") && target.queryParameter("mod") == "space") {
            return target.queryParameter("uid")?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        }
        return null
    }

    
    private fun absolutize(url: String, siteHost: String): HttpUrl? =
        if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
            url.toHttpUrlOrNull()
        } else {
            "https://$siteHost/${url.removePrefix("/")}".toHttpUrlOrNull()
        }
}





fun openForumLink(
    context: Context,
    url: String,
    siteHost: String,
    onThreadClick: (String) -> Unit,
    onUserClick: ((String) -> Unit)? = null,
) {
    val tid = ForumLinkResolver.siteThreadId(url, siteHost)
    if (tid != null) {
        onThreadClick(tid)
        return
    }
    val uid = ForumLinkResolver.siteUserId(url, siteHost)
    if (uid != null && onUserClick != null) {
        onUserClick(uid)
        return
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
