package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.Notice
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.parser.ForumParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


data class NoticeListPage(
    val notices: List<Notice> = emptyList(),
    
    val totalPages: Int = 1,
    val loggedOut: Boolean = false,
)










class NoticeRepo(private val http: HttpClient) {

    suspend fun notices(view: String, type: String? = null, page: Int = 1): NoticeListPage =
        withContext(Dispatchers.IO) {
            val typeQuery = if (type == null) "" else "&type=$type"
            val html = http.get(
                http.url(
                    "home.php?mod=space&do=notice&view=$view$typeQuery&page=$page&mobile=2",
                ),
            )
        val parsed = ForumParser.parseNotices(html)
        if (parsed.loggedOut) return@withContext NoticeListPage(loggedOut = true)
        NoticeListPage(
            notices = parsed.notices.distinctBy { it.id },
            totalPages = noticeTotalPages(Jsoup.parse(html), page, parsed.notices.size),
        )
    }

    








    private fun noticeTotalPages(document: Document, page: Int, noticeCount: Int): Int {
        val pagers = document.select("div.comiis_multi_box, div.pg, div.comiis_page")
        if (pagers.isEmpty()) {
            
            
            return if (noticeCount >= FULL_PAGE_HINT) page + 1 else 1
        }
        var total = 1
        var hasNextLink = false
        for (pager in pagers) {
            
            val text = buildString {
                append(pager.text())
                for (titled in pager.select("[title]")) {
                    append(' ').append(titled.attr("title"))
                }
            }
            for (match in RE_TOTAL_PAGES.findAll(text)) {
                total = maxOf(total, match.groupValues[1].toIntOrNull() ?: 1)
            }
            for (option in pager.select("select option")) {
                total = maxOf(total, option.attr("value").trim().toIntOrNull() ?: 1)
            }
            for (link in pager.select("a[href]")) {
                val linked = pageFromHref(link.attr("href")) ?: continue
                total = maxOf(total, linked)
                if (linked > page) hasNextLink = true
            }
        }
        
        if (hasNextLink) total = maxOf(total, page + 1)
        return total.coerceAtLeast(1)
    }
}


private const val FULL_PAGE_HINT = 10


private val RE_TOTAL_PAGES = Regex("""共\s*(\d+)\s*页""")


private val RE_PAGE_PARAM = Regex("""[?&]page=(\d+)""")


private fun pageFromHref(href: String): Int? =
    RE_PAGE_PARAM.find(href)?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1)
