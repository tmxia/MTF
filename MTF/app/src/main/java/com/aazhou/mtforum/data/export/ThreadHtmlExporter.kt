package com.aazhou.mtforum.data.export

import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.model.ThreadPage
import com.aazhou.mtforum.ui.common.imageUrl
import com.aazhou.mtforum.ui.common.isSmiley
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document







internal fun buildThreadHtml(page: ThreadPage, localSrc: (String) -> String?): String {
    val title = page.subject?.takeIf { it.isNotBlank() } ?: "MTF 帖子"
    val posts = page.posts
    val op = posts.firstOrNull { it.isFirstPost } ?: posts.firstOrNull()
    val replies = posts.filter { it !== op }
    val body = buildString {
        append("<header class=\"meta\">")
        append("<div class=\"forum\">").append(escapeHtml(page.forumName.orEmpty())).append("</div>")
        append("<div class=\"saved\">由 MTF 客户端保存于 ")
            .append(formatSavedAt(System.currentTimeMillis())).append("</div>")
        append("</header>")
        append("<h1>").append(escapeHtml(title)).append("</h1>")
        if (op != null) {
            append(postHtml(op, isOpPost = true, localSrc))
        }
        append("<h2 class=\"replies-title\">全部评论（").append(replies.size).append("）</h2>")
        replies.forEach { append(postHtml(it, isOpPost = false, localSrc)) }
        append("<footer class=\"source\">原文：<a href=\"")
            .append(escapeHtml("https://bbs.binmt.cc/thread-${page.tid}-1-1.html"))
            .append("\">https://bbs.binmt.cc/thread-").append(escapeHtml(page.tid))
            .append("-1-1.html</a></footer>")
    }
    return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${escapeHtml(title)}</title>
        <style>
        $PAGE_CSS
        </style>
        </head>
        <body>
        <main>
        $body
        </main>
        </body>
        </html>
    """.trimIndent()
}

private fun postHtml(post: Post, isOpPost: Boolean, localSrc: (String) -> String?): String = buildString {
    append("<section class=\"post").append(if (isOpPost) " op" else "").append("\">")
    append("<div class=\"author\">")
    append("<span class=\"name\">").append(escapeHtml(post.authorName.ifBlank { "匿名" })).append("</span>")
    if (post.isOp && !isOpPost) append("<span class=\"badge\">楼主</span>")
    val meta = listOfNotNull(
        post.floorLabel?.takeIf { it.isNotBlank() }?.let { escapeHtml(it) },
        post.timeText?.takeIf { it.isNotBlank() }?.let { escapeHtml(it) },
        post.location?.takeIf { it.isNotBlank() }?.let { escapeHtml(it) },
    ).joinToString(" · ")
    if (meta.isNotBlank()) append("<span class=\"sub\">").append(meta).append("</span>")
    append("</div>")
    post.quote?.let { quote ->
        append("<blockquote>")
        append("<div class=\"quote-meta\">")
        if (!quote.author.isNullOrBlank()) {
            append("回复 ").append(escapeHtml(quote.author)).append("：")
        }
        quote.time?.takeIf { it.isNotBlank() }?.let { append("（").append(escapeHtml(it)).append("）") }
        append("</div>")
        append("<div class=\"quote-text\">").append(escapeHtml(quote.text)).append("</div>")
        append("</blockquote>")
    }
    if (post.editedText != null) {
        append("<div class=\"edited\">").append(escapeHtml(post.editedText)).append("</div>")
    }
    append("<div class=\"content\">").append(sanitizeContentHtml(post.contentHtml, localSrc)).append("</div>")
    if (post.hasHiddenContent) {
        append("<div class=\"hidden\">本帖含回复可见内容，保存时未解锁。</div>")
    }
    append("</section>")
}








internal fun sanitizeContentHtml(html: String, localSrc: (String) -> String?): String {
    if (html.isBlank()) return ""
    val doc: Document = Jsoup.parseBodyFragment(html)
    doc.select("script,style,iframe,object,embed,form,input,button,link,meta").remove()
    stripEventAttributes(doc)
    doc.select("img").forEach { img ->
        val smiley = isSmiley(img)
        val original = img.attr("src")
        val realUrl = imageUrl(img)
        val replacement = realUrl?.let(localSrc)
        val hadSmilieId = img.hasAttr("smilieid")
        img.clearAttributes()
        img.attr("src", replacement ?: original)
        if (smiley) {
            
            
            
            img.addClass("smiley")
            if (hadSmilieId) img.attr("smilieid", "1")
        } else {
            img.attr("alt", "")
        }
    }
    doc.select("a[href]").forEach { a ->
        val href = a.attr("href")
        if (href.isNotBlank() && !href.startsWith("javascript:", true)) {
            a.attr("target", "_blank")
            a.attr("rel", "noopener")
        }
    }
    return doc.body().html()
}








internal fun collectContentImageUrls(html: String): List<String> {
    if (html.isBlank()) return emptyList()
    return Jsoup.parseBodyFragment(html).select("img").mapNotNull { imageUrl(it) }
}


private fun stripEventAttributes(doc: Document) {
    doc.allElements.forEach { element ->
        val eventKeys = element.attributes()
            .filter { it.key.startsWith("on", ignoreCase = true) }
            .map { it.key }
        eventKeys.forEach { element.removeAttr(it) }
    }
}


internal fun dataUriOf(url: String, bytes: ByteArray): String {
    val mime = when {
        url.contains(".png", true) -> "image/png"
        url.contains(".gif", true) -> "image/gif"
        url.contains(".webp", true) -> "image/webp"
        else -> "image/jpeg"
    }
    return "data:$mime;base64," + Base64.getEncoder().encodeToString(bytes)
}

private fun escapeHtml(text: String): String = escapeHtmlValue(text)

private val savedAtFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun formatSavedAt(millis: Long): String = savedAtFormat.format(Date(millis))


internal fun escapeHtmlValue(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private val PAGE_CSS = """
    :root { color-scheme: light; }
    * { box-sizing: border-box; }
    body { margin: 0; background: #f2f2f6; color: #0f172a;
           font: 15px/1.7 -apple-system, "PingFang SC", "Noto Sans CJK SC", sans-serif; }
    main { max-width: 720px; margin: 0 auto; padding: 16px 14px 48px; }
    header.meta { color: #475569; font-size: 12px; display: flex; justify-content: space-between; }
    h1 { font-size: 20px; line-height: 1.45; margin: 10px 0 14px; }
    h2.replies-title { font-size: 15px; margin: 18px 2px 8px; }
    section.post { background: #fff; border-radius: 12px; padding: 14px 16px; margin-bottom: 10px; }
    section.post.op { margin-top: 4px; }
    .author { display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px; margin-bottom: 6px; }
    .name { font-weight: 700; font-size: 14px; }
    .badge { color: #2563eb; background: rgba(37,99,235,.08); border-radius: 4px;
             font-size: 10px; padding: 1px 5px; }
    .sub { color: #94a3b8; font-size: 11px; }
    .edited { color: #475569; font-size: 11px; font-style: italic; margin-bottom: 4px; }
    .content { word-break: break-word; }
    .content img { max-width: 100%; height: auto; border-radius: 6px; vertical-align: middle; }
    .content img.smiley { width: 1.4em; height: 1.4em; max-width: 1.4em; border-radius: 0; vertical-align: -0.2em; }
    .content table { border-collapse: collapse; max-width: 100%; }
    .content td, .content th { border: 1px solid #e2e8f0; padding: 4px 8px; font-size: 13px; }
    blockquote { margin: 8px 0; padding: 8px 10px; background: #f1f5f9; border-radius: 6px;
                 color: #475569; font-size: 13px; }
    .quote-meta { font-size: 11px; color: #94a3b8; margin-bottom: 2px; }
    blockquote p { margin: 0; }
    .hidden { margin-top: 8px; padding: 8px 10px; border: 1px dashed #e2e8f0; border-radius: 6px;
              color: #475569; font-size: 12px; }
    footer.source { margin-top: 20px; color: #94a3b8; font-size: 11px; text-align: center; }
    footer.source a { color: #64748b; }
""".trimIndent()
