package com.aazhou.mtforum.data.parser

import com.aazhou.mtforum.data.model.Forum
import com.aazhou.mtforum.data.model.FriendRequestItem
import com.aazhou.mtforum.data.model.FriendRequestPage
import com.aazhou.mtforum.data.model.ForumGroup
import com.aazhou.mtforum.data.model.ForumPage
import com.aazhou.mtforum.data.model.AtUser
import com.aazhou.mtforum.data.model.HomeData
import com.aazhou.mtforum.data.model.LikeResult
import com.aazhou.mtforum.data.model.LikeUser
import com.aazhou.mtforum.data.model.LoginResult
import com.aazhou.mtforum.data.model.Notice
import com.aazhou.mtforum.data.model.NoticePage
import com.aazhou.mtforum.data.model.PmPage
import com.aazhou.mtforum.data.model.PrivateMessage
import com.aazhou.mtforum.data.model.ChatMessage
import com.aazhou.mtforum.data.model.ChatConversation
import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.model.PostQuote
import com.aazhou.mtforum.data.model.ReplyResult
import com.aazhou.mtforum.data.model.CheckInStatus
import com.aazhou.mtforum.data.model.CheckInResult
import com.aazhou.mtforum.data.model.SearchResult
import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.data.model.ThreadPage
import com.aazhou.mtforum.data.model.UserProfile
import com.aazhou.mtforum.data.model.NewThreadPageData
import com.aazhou.mtforum.data.model.NewThreadResult
import com.aazhou.mtforum.data.model.EditPostPageData
import com.aazhou.mtforum.data.model.EditAttachment
import com.aazhou.mtforum.data.model.EditPostResult
import com.aazhou.mtforum.data.model.ThreadType
import com.aazhou.mtforum.data.model.FavoriteItem
import com.aazhou.mtforum.data.model.FavoritePage
import com.aazhou.mtforum.data.model.FollowUserItem
import com.aazhou.mtforum.data.model.FollowUserPage
import com.aazhou.mtforum.data.model.FriendUserItem
import com.aazhou.mtforum.data.model.FriendUserPage
import com.aazhou.mtforum.data.model.MyThreadPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element







object ForumParser {

    private const val BASE = "https://bbs.binmt.cc/"
    private val RE_TID = Regex("thread-(\\d+)")
    private val RE_FID = Regex("forum-(\\d+)-")
    private val RE_UID = Regex("uid=(\\d+)")
    private val RE_FORMHASH_INPUT = Regex("formhash[\"']?\\s+value=[\"']([0-9a-fA-F]+)")
    private val RE_FORMHASH_JS = Regex("formhash\\s*=\\s*['\"]([0-9a-fA-F]+)['\"]")
    private val RE_LOGINHASH = Regex("loginhash=([A-Za-z0-9]+)")

    
    private val RE_FORMHASH_PARAM = Regex("formhash=([0-9A-Za-z]{8,})")
    
    private val RE_UPLOAD_UID = Regex("uploadformdata:\\{uid:\"(\\d+)\"")
    private val RE_UPLOAD_HASH = Regex("uploadformdata:\\{uid:\"\\d+\",\\s*hash:\"([0-9a-fA-F]+)\"")
    private val RE_UPLOAD_PAIR = Regex("uploadformdata:\\{uid:\"(\\d+)\",\\s*hash:\"([0-9a-fA-F]+)\"")
    private val RE_QUOTE_PID = Regex("pid=(\\d+)")
    private val RE_TAG = Regex("<[^>]+>")
    private val RE_BLOCK_END = Regex(
        "</(?:p|div|li|ul|ol|blockquote|quote|h[1-6]|tr|table|pre|hr)>",
        RegexOption.IGNORE_CASE,
    )
    private val RE_WHITESPACE = Regex("\\s+")
    
    private val RE_SMILEY_CODE = Regex("\\[#[^\\]\\s]*]")
    private val RE_FLOOR_NUMBER = Regex("^(\\d+)")

    
    private const val MAX_CARD_IMAGES = 9

    
    private const val QUOTE_SNIPPET_LIMIT = 100
    private const val QUOTE_SNIPPET_DOT = " ..."

    
    private const val QUOTE_SNIPPET_MAX_LINES = 3

    





    private val RE_NON_IMAGE = Regex(
        "none\\.png|none\\.gif|blank\\.gif|loading|avatar\\.php|" +
            "/static/image/(common|filetype|smiley)/|smiley|emoticon",
        RegexOption.IGNORE_CASE,
    )

    
    private val RE_EDITED = Regex("本[帖贴]最后由\\s*.+?\\s*于\\s*.+?\\s*编辑")

    
    private val RE_ABS_DATE = Regex("\\d{4}-\\d{1,2}-\\d{1,2}")

    






    internal val RE_QUOTE_LINE_BREAK = Regex(
        """<br\s*/?>|</?(?:div|p|li|tr|h[1-6]|blockquote|table|ul|ol)\b[^>]*>""",
        RegexOption.IGNORE_CASE,
    )


    
    private val RE_EDIT_TIME = Regex("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\s+\\d{1,2}:\\d{2}(?::\\d{2})?")

    
    private val RE_EDIT_SUCCESS = Regex("succeedhandle_[A-Za-z0-9_]*\\s*\\(\\s*['\"]|编辑成功|更新成功|已经更新|post_edit_succeed")

    





    private val RE_EDIT_TIP = Regex("errorhandle_[A-Za-z0-9_]*\\s*\\(\\s*['\"]([^'\"]{1,80})['\"]")

    






    private val RE_EDIT_FORM_ACTION = Regex("action=edit")
    private val RE_EDIT_SUBMIT = Regex("editsubmit")

    
    private val RE_HIDDEN_PROMPT = Regex("查看本帖隐藏内容|回复可见|隐藏内容|回复后.{0,6}查看")

    private val JS_FID = Regex("var\\s+fid\\s*=\\s*parseInt\\(['\"](\\d+)")
    private val JS_AUTHOR_ID = Regex("authorid\\s*=\\s*['\"](\\d+)")
    
    private val RE_JS_TID = Regex("\\btid\\s*=\\s*['\"](\\d+)")

    
    private val RE_UID_VAR = Regex("uid\\s*=\\s*['\"](\\d+)['\"]")

    
    private val RE_LIKES_DAYCOUNT = Regex("daycount'?\\s*:\\s*'?(\\d+)")

    
    private val RE_REMAIN_LIKES = Regex("还能评价\\s*(\\d+)\\s*次")

    
    private val RE_RECOMMEND_COUNT = Regex("recommendc'?\\s*:\\s*'?(\\d+)")

    
    private val RE_LIKE_ERROR = Regex(
        "没有点赞权限|不能点赞|今日评价机会已用完|已用完|关闭的主题|请先登录|formhash错误|非法操作",
    )

    
    private val RE_LIKE_SUCCESS = Regex("succeedhandle|recommendv|recommendc|点赞成功|评价成功")

    






    private val LOGIN_SUCCESS_HINT = Regex("登录成功|欢迎您回来|欢迎回来|现在将转入|正在转入")

    
    private val TIP_FAILURE_KEYWORD = Regex("密码|验证码|失败|错误|次数|不存在|锁定|禁用|不正确|无效|稍后|请填写|请输入|安全提问")

    
    private val RE_TIP_MESSAGE = Regex("(?s)<p>(.*?)(?:<script|</p>)")

    
    private val FLOOR_NAMES = mapOf(
        "沙发" to 2, "椅子" to 3, "板凳" to 4, "地毯" to 5, "凉席" to 6,
        "报纸" to 7, "地板" to 8, "地下室" to 9, "下水道" to 10,
    )

    private fun doc(html: String): Document = Jsoup.parse(html, BASE)

    








    private fun String.stripInvisibleChars(): String = filterNot { it.isInvisibleChar() }.trim()

    



    private fun Char.isInvisibleChar(): Boolean {
        val cp = code
        return cp in 0xE000..0xF8FF ||
            cp == 0x200B || cp == 0x200C || cp == 0x200D || cp == 0x2060 ||
            cp == 0xFEFF || cp == 0x00AD || cp == 0x180E
    }

    
    private fun editedBadge(marker: String?): String? =
        RE_EDIT_TIME.find(marker.orEmpty())?.value?.let { "$it 编辑" }

    
    fun parseForumList(html: String): List<ForumGroup> {
        val document = doc(html)
        val groups = mutableListOf<ForumGroup>()

        
        
        
        val todayByFid = HashMap<String, Int>()
        for (block in document.select("div.comiis_forumlist")) {
            for (a in block.select("div[id^=sub_forum_] li a")) {
                val fid = RE_FID.find(a.attr("href"))?.groupValues?.get(1) ?: continue
                val n = a.selectFirst("em span")?.text()?.trim()?.toIntOrNull()
                if (n != null && n > 0) {
                    
                    todayByFid[fid] = n
                }
            }
        }

        
        for (block in document.select("div.comiis_forumlist")) {
            val name = block.selectFirst("div.comiis_bbs_show h2 a")?.text()?.trim()
                ?: block.selectFirst("div.comiis_bbs_show h2")?.text()?.trim()
                ?: continue
            val forums = mutableListOf<Forum>()
            for (a in block.select("div[id^=sub_forum_] li a")) {
                val fid = RE_FID.find(a.attr("href"))?.groupValues?.get(1) ?: continue
                val forumName = a.selectFirst("img[alt]")?.attr("alt")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: a.selectFirst("p")?.text()?.trim()
                    ?: a.text().trim()
                if (forumName.isEmpty()) continue
                forums.add(Forum(
                    fid = fid,
                    name = forumName,
                    icon = a.selectFirst("em img")?.attr("src"),
                    todayPosts = todayByFid[fid] ?: 0,
                ))
            }
            if (forums.isNotEmpty()) groups.add(ForumGroup(name, forums))
        }
        return groups
    }

    



    fun parsePortalFeed(xml: String): List<Thread> {
        val html = RE_CDATA.find(xml)?.groupValues?.get(1) ?: return emptyList()
        return parseForumPage(html, 1).threads
    }

    
    fun parseForumPage(html: String, page: Int): ForumPage {
        val document = doc(html)
        
        
        val announcements = mutableListOf<Thread>()
        for (a in document.select("li.b_t:not(.forumlist_li) a[href*=thread-]")) {
            val tid = RE_TID.find(a.attr("href"))?.groupValues?.get(1) ?: continue
            announcements.add(
                Thread(
                    tid = tid,
                    subject = a.text().stripInvisibleChars().removePrefix("置顶").trim(),
                    isAnnouncement = true,
                )
            )
        }

        val threads = mutableListOf<Thread>()
        for (li in document.select("li.forumlist_li")) {
            
            
            val titleA = li.selectFirst("div.mmlist_li_box h2 a[href*=thread-]")
            val threadA = titleA ?: li.selectFirst("div.list_body a[href*=thread-]") ?: continue
            val tid = RE_TID.find(threadA.attr("href"))?.groupValues?.get(1) ?: continue
            val authorA = li.selectFirst("a.top_user")
            val (replyCount, viewCount, likeCount) = readCardStats(li)
            threads.add(
                Thread(
                    tid = tid,
                    subject = threadA.text().stripInvisibleChars(),
                    authorName = authorA?.text()?.trim(),
                    authorUid = RE_UID.find(authorA?.attr("href") ?: "")?.groupValues?.get(1),
                    avatarUrl = li.selectFirst("img.top_tximg")?.attr("src"),
                    level = li.selectFirst("span.top_lev")?.text()?.trim(),
                    timeText = li.selectFirst("div.forumlist_li_time span")?.text()?.trim(),
                    replyCount = replyCount,
                    viewCount = viewCount,
                    
                    excerpt = if (titleA != null) {
                        li.selectFirst("div.list_body a")?.text()?.stripInvisibleChars()
                    } else null,
                    forumName = li.selectFirst("div.comiis_xznalist_bk a[href*=forum-]")?.let { a ->
                        a.ownText().trim().ifEmpty { a.text().trim() }
                    }?.ifEmpty { null }
                        
                        
                        ?: li.selectFirst("div.forumlist_li_time a[href*=forum-]")?.text()
                            ?.substringAfter("来自")?.trim()?.takeIf { it.isNotEmpty() },
                    imageUrls = collectThreadImages(li),
                    likeCount = likeCount,
                )
            )
        }

        return ForumPage(
            forumName = document.selectFirst("div.comiis_view_header1 h1")?.text()?.trim(),
            announcements = announcements.distinctBy { it.tid },
            threads = threads.distinctBy { it.tid },
            page = page,
            hasNext = hasNextPage(document),
        )
    }

    





    private fun readCardStats(li: Element): Triple<String?, String?, String?> {
        val box = li.selectFirst("div[class*=znalist_bottom]") ?: return Triple(null, null, null)
        val numbers = box.select("li span.comiis_tm")
        if (numbers.isNotEmpty()) {
            return Triple(
                box.selectFirst("li a[href*=thread-] span.comiis_tm")?.text()?.trim(),
                numbers.lastOrNull()?.text()?.trim(),
                numbers.firstOrNull()?.text()?.trim(),
            )
        }
        val items = box.select("li")
        fun statText(label: String): String? =
            items.firstOrNull { it.text().contains(label) }
                ?.text()?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
        return Triple(statText("评论"), statText("阅读"), statText("赞"))
    }

    





    private fun collectThreadImages(li: Element): List<String> {
        val urls = LinkedHashSet<String>()
        val containers = listOf(
            ".comiis_pyqlist_imgs img, .comiis_pyqlist_img img",
            ".mmlist_li_box img",
        )
        for (selector in containers) {
            for (img in li.select(selector)) {
                if (urls.size >= MAX_CARD_IMAGES) break
                val url = listOf("comiis_loadimages", "data-original", "data-src", "file", "src")
                    .firstNotNullOfOrNull { attr ->
                        img.attr(attr).trim().takeIf { it.isNotEmpty() }
                    }
                if (url != null && isCardImage(url)) urls.add(url)
            }
            if (urls.isNotEmpty()) break
        }
        return urls.toList()
    }

    
    private fun isCardImage(url: String): Boolean {
        val lower = url.lowercase()
        return RE_NON_IMAGE.containsMatchIn(lower).not()
    }

    
    fun parseThreadPage(html: String, page: Int): ThreadPage {
        val document = doc(html)
        val subject = document.title().removeSuffix("- MT论坛").trim()
        val tid = extractTid(document, html)
        val postNodes = document.select("div.comiis_postli[id^=pid]")
        
        
        
        val anchorIndex = postNodes.indexOfFirst {
            floorNumberOf(it.selectFirst("span.f_d.y")?.text()?.trim()) != null
        }
        val anchorFloor = anchorIndex.takeIf { it >= 0 }?.let {
            floorNumberOf(postNodes[it].selectFirst("span.f_d.y")?.text()?.trim())
        }
        val authorId = JS_AUTHOR_ID.find(html)?.groupValues?.get(1)
        val firstPostIndex = postNodes.indexOfFirst { it.selectFirst("div.comiis_postli_topv1") != null }
        
        
        val parsed = postNodes.map { parsePost(it, authorId) }
        var stickySeen = 0
        val posts = postNodes.mapIndexed { index, li ->
            val post = parsed[index]
            val label = li.selectFirst("span.f_d.y")?.text()?.stripInvisibleChars()
            val explicit = floorNumberOf(label)
            val floor = when {
                post.isFirstPost -> 0
                
                
                
                post.sticky -> STICKY_FLOOR
                
                explicit != null -> explicit
                
                
                page == 1 && firstPostIndex >= 0 ->
                    FIRST_REPLY_FLOOR + index - firstPostIndex - 1 - stickySeen
                
                anchorFloor != null -> anchorFloor + (index - anchorIndex)
                else -> null
            }
            if (index > firstPostIndex && post.sticky) stickySeen++
            post.copy(
                floor = floor,
                floorLabel = floorLabelOf(label, floor),
            )
        }
        
        val totalReplies = document.selectFirst("div.comiis_pltit h2 span")
            ?.text()?.trim()?.toIntOrNull()
        
        val likeCount = document.selectFirst("em.comiis_recommend_num")
            ?.text()?.let { numberFromText(it) }
        val favoriteCount = document.selectFirst("span.comiis_favorite_a_num")
            ?.text()?.let { numberFromText(it) }
        val replyUrl = document.selectFirst("form#fastpostform")?.attr("action")
        val noticeAuthor = document.selectFirst("form#fastpostform input[name=noticeauthor]")
            ?.attr("value")?.trim()?.ifBlank { null }
        val action = parseActionState(document, html)
        val follow = parseFollowState(document)
        return ThreadPage(
            tid = tid,
            subject = subject,
            
            forumName = document.selectFirst("a.kmtit")?.text()?.trim(),
            posts = posts,
            page = page,
            totalPages = parseTotalPages(document, tid),
            formhash = parseFormhash(html),
            replyUrl = replyUrl,
            noticeAuthor = noticeAuthor,
            fid = JS_FID.find(html)?.groupValues?.get(1)
                ?: Regex("fid=(\\d+)").find(replyUrl ?: "")?.groupValues?.get(1),
            opUid = authorId,
            totalReplies = totalReplies,
            likeCount = likeCount,
            favoriteCount = favoriteCount,
            isLiked = action.liked,
            likedStateKnown = action.likedKnown,
            isFavorited = action.favorited,
            favoritedStateKnown = action.favoritedKnown,
            isFollowingOp = follow.first,
            followingStateKnown = follow.second,
            uploadUid = RE_UPLOAD_UID.find(html)?.groupValues?.get(1),
            uploadHash = RE_UPLOAD_HASH.find(html)?.groupValues?.get(1),
        )
    }

    






    private fun parseFollowState(document: Document): Pair<Boolean, Boolean> {
        val link = document.selectFirst("a[href*=ac=follow]") ?: return false to false
        val href = link.attr("href")
        val text = link.text()
        if (href.contains("op=del") || text.contains("取消关注")) return true to true
        if (href.contains("op=add")) return false to true
        if (text.contains("已关注")) return true to true
        return false to true
    }

    
    private data class ActionState(
        val liked: Boolean = false,
        val likedKnown: Boolean = false,
        val favorited: Boolean = false,
        val favoritedKnown: Boolean = false,
    )

    





    private fun parseActionState(document: Document, html: String): ActionState {
        val uid = RE_UID_VAR.find(html)?.groupValues?.get(1)
        if (uid == null || uid == "0") return ActionState()
        var liked = false
        var likedKnown = false
        val recommend = document.selectFirst(
            "a.comiis_recommend_addkey, a.comiis_recommend_new, .comiis_recommend_addkey",
        )
        if (recommend != null) {
            val icon = recommend.selectFirst("i.comiis_recommend_color")
            val classes = recommend.className() + " " + (icon?.className() ?: "")
            val iconHtml = icon?.html().orEmpty()
            liked = classes.contains("f_a") || iconHtml.contains("e654")
            likedKnown = true
        }
        var favorited = false
        var favoritedKnown = false
        val favoriteIcon = document.selectFirst("#comiis_favorite_a i.comiis_favorite_a_color")
        if (favoriteIcon != null) {
            val classes = favoriteIcon.className()
            val iconHtml = favoriteIcon.html()
            favorited = classes.contains("f_a") || iconHtml.contains("e64c")
            favoritedKnown = true
        }
        return ActionState(liked, likedKnown, favorited, favoritedKnown)
    }

    private fun parsePost(li: Element, authorId: String?): Post {
        val authorA = li.selectFirst("a.top_user")
        
        val timeEl = li.selectFirst("span.kmtime") ?: li.selectFirst("span.comiis_tm")
        val contentEl = li.selectFirst("div.comiis_a")
        
        
        var quote: PostQuote? = null
        for (el in contentEl?.select("div.comiis_quote, div.quote").orEmpty()) {
            val parsed = parseQuote(el)
            if (parsed != null) {
                quote = parsed
                el.remove()
                break
            }
        }
        val uid = RE_UID.find(authorA?.attr("href") ?: "")?.groupValues?.get(1)
        
        val firstPost = li.selectFirst("div.comiis_postli_topv1") != null
        
        
        
        
        
        val hiddenNodes = if (firstPost) {
            contentEl?.select("div.locked, div.comiis_quote")?.filter {
                it.selectFirst("blockquote") == null &&
                    it.selectFirst("h2.f_a") == null &&
                    !it.text().contains("发表于") &&
                    RE_HIDDEN_PROMPT.containsMatchIn(it.text())
            }.orEmpty()
        } else {
            emptyList()
        }
        hiddenNodes.forEach { it.remove() }
        
        val rawHtml = extractContentHtml(li, contentEl)
        val editedText = RE_EDITED.find(rawHtml)?.value?.trim()
        return Post(
            pid = li.id().removePrefix("pid"),
            authorName = authorA?.text()?.trim() ?: "",
            authorUid = uid,
            avatarUrl = li.selectFirst("img.top_tximg")?.attr("src"),
            level = li.selectFirst("a.top_lev")?.text()?.trim(),
            timeText = timeEl?.ownText()?.trim(),
            
            location = (timeEl?.selectFirst("code.comiis_iplocality font")?.text()
                ?: timeEl?.selectFirst("code.comiis_iplocality")?.text())
                ?.replace(Regex("^\\s*来自\\s*"), "")
                ?.trim(),
            contentHtml = RE_EDITED.replace(rawHtml, "").trim(),
            isOp = li.selectFirst("span.top_lev.bg_c") != null || (authorId != null && uid == authorId),
            isFirstPost = firstPost,
            quote = quote,
            editedText = editedBadge(editedText),
            hasHiddenContent = hiddenNodes.isNotEmpty(),
            
            sticky = li.selectFirst("span.f_d.y span.f_g")?.text()?.contains("置顶") == true,
        )
    }

    






    private fun extractContentHtml(li: Element, contentEl: Element?): String {
        val container = contentEl?.parent() ?: li.selectFirst("div.comiis_messages")
        if (container == null) return contentEl?.html().orEmpty()
        val html = StringBuilder()
        for (child in container.children()) {
            when {
                child === contentEl -> html.append(child.html())
                child.tagName() == "ul" && child.hasClass("comiis_img_list") -> html.append(child.outerHtml())
                child.hasClass("comiis_img_one") -> html.append(child.outerHtml())
            }
        }
        return html.toString()
    }

    








    private fun parseQuote(quote: Element): PostQuote? {
        val pid = RE_QUOTE_PID.find(quote.html())?.groupValues?.get(1)
        
        
        
        
        val lines = quote.html()
            .split(RE_QUOTE_LINE_BREAK)
            .map { Jsoup.parse(it).text().trim() }
            .filter { it.isNotEmpty() }
        val metaLine = lines.firstOrNull()?.removePrefix("回复")?.trim().orEmpty()
        if (!metaLine.contains("发表于")) return null
        
        val timePart = metaLine.substringAfter("发表于")
            .substringBefore("来自")
            .trim().trimEnd(']').trim()
        return PostQuote(
            author = metaLine.substringBefore("发表于").trim().ifEmpty { null },
            
            
            
            time = timePart.takeIf { RE_ABS_DATE.containsMatchIn(it) },
            text = lines.drop(1).joinToString("\n"),
            pid = pid,
        )
    }

    
    private fun floorNumberOf(label: String?): Int? {
        val text = label?.trim() ?: return null
        if (text.isEmpty()) return null
        
        RE_FLOOR_NUMBER.find(text)?.groupValues?.get(1)?.let { return it.toInt() }
        return FLOOR_NAMES[text]
    }

    



    private fun floorLabelOf(label: String?, floor: Int?): String? {
        val text = label.orEmpty()
        if (text.isNotEmpty() && FLOOR_NAMES.containsKey(text)) return text
        return floor?.let { "$it#" }
    }

    



    private const val FIRST_REPLY_FLOOR = 2

    






    private const val STICKY_FLOOR = 1


    
    fun parseFormhash(html: String): String? =
        RE_FORMHASH_INPUT.find(html)?.groupValues?.get(1)
            ?: RE_FORMHASH_JS.find(html)?.groupValues?.get(1)

    





    fun parseFormhashAny(html: String): String? {
        if (html.isBlank()) return null
        Jsoup.parse(html).selectFirst("input[name=formhash]")?.attr("value")
            ?.takeIf { it.isNotBlank() }?.let { return it }
        return parseFormhash(html)
            ?: RE_FORMHASH_PARAM.find(html)?.groupValues?.get(1)
    }

    
    fun extractLoginHash(html: String): String? =
        RE_LOGINHASH.find(html)?.groupValues?.get(1)

    












    fun replyQuoteNotice(
        author: String,
        time: String?,
        contentHtml: String,
        quotePid: String,
        tid: String,
    ): String {
        val meta = "$author 发表于 ${time.orEmpty()}".trimEnd()
        val url = "forum.php?mod=redirect&goto=findpost&pid=$quotePid&ptid=$tid"
        return "[quote][size=2][url=$url][color=#999999]$meta[/color][/url][/size]\n" +
            "${replyQuoteSnippet(contentHtml)}[/quote]"
    }

    






    fun replyQuoteSnippet(contentHtml: String): String {
        val lines = RE_BLOCK_END.replace(contentHtml, "\n")
            .split("\n")
            .map { line ->
                val text = Jsoup.parse(line.replace(RE_TAG, " ")).text()
                RE_SMILEY_CODE.replace(text, "")
                    .replace('\u00A0', ' ')
                    .replace(RE_WHITESPACE, " ")
                    .trim()
            }
            .filter { it.isNotEmpty() }
        val snippet = lines.joinToString("\n")
        val cut = if (snippet.length <= QUOTE_SNIPPET_LIMIT) {
            snippet
        } else {
            snippet.take(QUOTE_SNIPPET_LIMIT).trimEnd() + QUOTE_SNIPPET_DOT
        }
        return cut.split("\n").take(QUOTE_SNIPPET_MAX_LINES).joinToString("\n")
    }

    
    fun extractLoginFormhash(html: String): String? =
        doc(html).selectFirst("input#formhash")?.attr("value")?.ifEmpty { null }

    
    fun extractDeleteHash(html: String, pid: String): String? {
        Regex("formhash=([0-9a-zA-Z]+)[^\"'<>]{0,200}?pid=$pid").find(html)
            ?.let { return it.groupValues[1] }
        Regex("pid=$pid[^\"'<>]{0,200}?formhash=([0-9a-zA-Z]+)").find(html)
            ?.let { return it.groupValues[1] }
        return null
    }

    
    fun parseLoginResult(html: String): LoginResult {
        val document = doc(html)
        val captcha = document.selectFirst("img[src*=seccode], img[src*=secqaa]")?.attr("src")
        val stillLoginForm = document.selectFirst("form#loginform") != null
        if (stillLoginForm) {
            val message = document.select("div.comiis_tip, div.alert_error, div.alert_info")
                .firstOrNull { it.text().isNotBlank() }?.text()?.trim()
            return LoginResult(success = false, message = message, needsCaptcha = captcha != null, captchaUrl = captcha)
        }
        
        
        
        
        
        if (extractTitle(html)?.contains("提示信息") != true) return LoginResult(success = true)
        val message = extractTipText(html)
        val succeeded = message != null && LOGIN_SUCCESS_HINT.containsMatchIn(message)
        return LoginResult(
            success = succeeded,
            message = message ?: "登录失败，请重试",
            needsCaptcha = captcha != null,
            captchaUrl = captcha,
        )
    }

    
    fun extractTitle(html: String): String? =
        Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    







    fun extractTipText(html: String): String? {
        val document = doc(html)
        document.selectFirst("div.comiis_password_top p")?.text()?.trim()
            ?.ifEmpty { null }?.let { return it }
        document.selectFirst("#messagetext p, #messagetext")?.text()?.trim()
            ?.ifEmpty { null }?.let { return it }
        document.selectFirst("div.comiis_tip p, div.comiis_tip")?.text()?.trim()
            ?.ifEmpty { null }?.let { return it }
        val full = messagePageText(html) ?: return null
        val hit = TIP_FAILURE_KEYWORD.find(full) ?: return full.take(120)
        val start = (hit.range.first - 30).coerceAtLeast(0)
        val end = (hit.range.last + 60).coerceAtMost(full.length)
        return full.substring(start, end).trim()
    }

    
    fun extractMessageRedirect(html: String): String? =
        Regex("location\\.href\\s*=\\s*['\"]([^'\"]+)['\"]").find(html)?.groupValues?.get(1)

    





    fun messagePageText(html: String): String? {
        val document = doc(html)
        document.select("script, style, noscript").remove()
        return document.body()?.text()?.trim()?.takeIf { it.isNotEmpty() }
    }

    
    fun loginFormFields(html: String): List<String> =
        doc(html).selectFirst("form#loginform")?.select("input[name]")
            ?.map { it.attr("name") }?.filter { it.isNotEmpty() } ?: emptyList()

    
    fun parseHome(html: String): HomeData {
        val document = doc(html)
        val hotForums = document.select("div.comiis_mh_hotbk li a[href*=forum-]").mapNotNull { a ->
            val fid = RE_FID.find(a.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val name = a.selectFirst("p")?.text()?.trim().orEmpty()
            if (name.isEmpty()) null
            else Forum(fid = fid, name = name, icon = a.selectFirst("img")?.attr("src"))
        }
        
        val ranked = document.select("div[class*=comiis_mh_txtlist] a[href*=thread-]")
        val links = if (ranked.isNotEmpty()) ranked else document.select("a[href*=thread-]")
        val hotThreads = links.mapNotNull { a ->
            val tid = RE_TID.find(a.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val subject = a.text().trim().replace(Regex("^\\d+\\s*"), "")
            if (subject.length < 4) return@mapNotNull null
            Thread(tid = tid, subject = subject)
        }.distinctBy { it.tid }.take(15)
        return HomeData(hotForums = hotForums, hotThreads = hotThreads)
    }

    
    
    fun parseUserProfile(html: String, fallbackUid: String? = null): UserProfile {
        val document = doc(html)
        val uid = fallbackUid?.takeIf { it.isNotBlank() }
            ?: Regex("discuz_uid\\s*=\\s*'(\\d+)'").find(html)?.groupValues?.get(1)
            ?: RE_UID_VAR.find(html)?.groupValues?.get(1)
        if (uid == null || uid == "0") return UserProfile(uid = uid)
        
        
        val nameEl = document.selectFirst(".comiis_space_tx h2")
        val level = document.selectFirst(".comiis_space_tx .kmlevs")?.text()?.trim()
            ?: document.selectFirst(".comiis_space_tx .user_lev")?.text()?.trim()
        val name = nameEl?.ownText()?.trim()?.ifEmpty { null }
            ?: nameEl?.text()?.trim()?.removeSuffix(level.orEmpty())?.trim()
            ?: document.selectFirst("a.sidenv_user")?.text()?.trim()
        val avatar = document.selectFirst(".comiis_space_tx .user_img img")?.attr("src")
            ?: document.selectFirst("img.sidenv_tximg, div.sidenv_user img")?.attr("src")

        val followEl = document.selectFirst("#followmod")
        val isFollowing = when {
            followEl == null -> null
            followEl.hasClass("kmok") || followEl.attr("href").contains("op=del") -> true
            followEl.attr("href").contains("op=add") -> false
            else -> null
        }

        return UserProfile(
            uid = uid,
            name = name,
            avatarUrl = avatar,
            level = level,
            groupName = document.selectFirst(".comiis_space_tx .kmlev")?.text()?.trim(),
            threads = numberByLabel(document, "帖子", "主题"),
            posts = numberByLabel(document, "回复", "回帖"),
            friends = numberByLabel(document, "好友"),
            following = numberByLabel(document, "关注"),
            followers = numberByLabel(document, "粉丝"),
            credits = numberByLabel(document, "积分"),
            gold = numberByLabel(document, "金币"),
            onlineTime = textByLabel(document, "在线时间"),
            isFollowing = isFollowing,
            formhash = parseFormhash(html),
        )
    }

    
    fun parseAjaxMessage(response: String): String? {
        
        
        val head = response.trimStart()
        if (head.startsWith("<html", ignoreCase = true) || head.startsWith("<!DOCTYPE", ignoreCase = true)) {
            return null
        }
        val pMatch = Regex("""<p>([\s\S]*?)(?:<script|</p>)""").find(response)?.groupValues?.get(1)?.trim()
        if (!pMatch.isNullOrBlank()) {
            val text = doc(pMatch).text().trim()
            if (text.isNotBlank()) return text
        }
        val dtMatch = Regex("""id=["']messagetext["'][^>]*>([\s\S]*?)</dt>""").find(response)?.groupValues?.get(1)?.trim()
        if (!dtMatch.isNullOrBlank()) {
            val text = doc(dtMatch).text().trim()
            if (text.isNotBlank()) return text
        }
        val alertMatch = Regex("""(?:popup\.open|alert)\(['"]([^'"]+)['"]""").find(response)?.groupValues?.get(1)?.trim()
        if (!alertMatch.isNullOrBlank() && !alertMatch.contains("<img") && !alertMatch.contains("http")) {
            return alertMatch
        }
        return null
    }

    






    private fun numberByLabel(root: Element, vararg labels: String): Int? {
        for (scope in profileScopes(root)) {
            for (label in labels) {
                for (holder in scope.select(":containsOwn($label)")) {
                    numberNearLabel(holder.text(), label)?.let { return it }
                    numberNearLabel((holder.parent() ?: holder).text(), label)?.let { return it }
                }
            }
        }
        return null
    }

    
    private fun numberNearLabel(text: String, label: String): Int? {
        Regex("""\d+""").find(text.substringAfter(label, ""))?.value?.toIntOrNull()
            ?.let { return it }
        return Regex("""\d+""").findAll(text.substringBefore(label, ""))
            .lastOrNull()?.value?.toIntOrNull()
    }

    
    private fun textByLabel(root: Element, vararg labels: String): String? {
        for (scope in profileScopes(root)) {
            for (label in labels) {
                for (holder in scope.select(":containsOwn($label)")) {
                    textNearLabel(holder.text(), label)?.let { return it }
                    textNearLabel((holder.parent() ?: holder).text(), label)?.let { return it }
                }
            }
        }
        return null
    }

    
    private fun textNearLabel(text: String, label: String): String? {
        val after = text.substringAfter(label, "").trim().trimStart(':', '：')
            .takeWhile { it.isDigit() || it.isWhitespace() || it in "小时分钟天秒" }
            .trim()
        if (after.isNotEmpty()) return after
        return text.substringBefore(label, "").trim()
            .takeLastWhile { it.isDigit() || it.isWhitespace() || it in "小时分钟天秒" }
            .trim()
            .ifEmpty { null }
    }

    
    private fun profileScopes(root: Element): List<Element> {
        val scopes = LinkedHashSet<Element>()
        for (selector in listOf(
            ".comiis_space_profileico",
            ".comiis_space_profilejf",
            ".comiis_space_profile",
            ".comiis_space_tx",
            ".comiis_space",
        )) {
            scopes.addAll(root.select(selector))
        }
        scopes.add(root)
        return scopes.toList()
    }





    private val RE_NOTICE_TIME = Regex(
        "\\d{4}-\\d{1,2}-\\d{1,2}(\\s+\\d{1,2}:\\d{2}(:\\d{2})?)?" +
            "|\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}" +
            "|(?:今天|昨天|前天)(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?" +
            "|\\d+\\s*(分钟|小时|天|秒)前",
    )

    
    private val RE_NOTICE_TID = Regex("""(?:ptid|tid)=(\d+)""")

    
    private val RE_NOTICE_PAGE = Regex("""[?&]page=(\d+)""")

    
    private val RE_THREAD_PAGE = Regex("""[?&]page=(\d+)|thread-\d+-(\d+)-""")


    










    




    fun parseNoticeSidebarCounts(html: String): Map<String, Int> {
        val document = doc(html)
        val counts = mutableMapOf<String, Int>()
        for (link in document.select("a[href*='do=notice']")) {
            val href = link.attr("href")
            val view = Regex("""[?&]view=([^&]+)""").find(href)?.groupValues?.get(1)
                ?.takeIf { it.isNotBlank() } ?: continue
            val type = Regex("""[?&]type=([^&]+)""").find(href)?.groupValues?.get(1)
                ?.takeIf { it.isNotBlank() }
            val key = if (type != null) "$view.$type" else view
            val count = extractNoticeCount(link) ?: continue
            if (count > 0) counts[key] = maxOf(counts[key] ?: 0, count)
        }
        return counts
    }

    private fun extractNoticeCount(link: Element): Int? {
        link.selectFirst(".kmnum, .kmnums")?.text()?.trim()?.toIntOrNull()?.let { return it }
        val title = link.attr("title")
        if (title.isNotBlank()) {
            Regex("""\((\d+)\)""").find(title)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
            Regex("""^(\d+)$""").find(title.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        }
        link.nextElementSibling()?.takeIf { it.hasClass("kmnum") || it.hasClass("kmnums") }
            ?.text()?.trim()?.toIntOrNull()?.let { return it }
        Regex("""\((\d+)\)""").find(link.text())?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return null
    }

    fun parseNotices(html: String): NoticePage {
        val document = doc(html)
        val loggedOut = document.selectFirst("form#loginform") != null

        var rows: List<Element> = document.select("div.comiis_notices_box li").filter { li ->
            li.selectFirst(".ntc_body") != null || li.select("a[href]").any {
                RE_NOTICE_TID.containsMatchIn(it.attr("href")) ||
                    RE_TID.containsMatchIn(it.attr("href"))
            }
        }
        if (rows.isEmpty()) {
            rows = document.select("li").filter { li ->
                li.select("a[href]").any {
                    RE_NOTICE_TID.containsMatchIn(it.attr("href")) ||
                        RE_TID.containsMatchIn(it.attr("href"))
                }
            }
        }
        if (rows.isEmpty()) {
            rows = document.select("li").filter { RE_NOTICE_TIME.containsMatchIn(it.text()) }
        }
        val notices = rows.mapIndexedNotNull { index, li -> noticeFromItem(li, index) }
        return NoticePage(
            loggedOut = loggedOut,
            notices = notices,
            totalPages = parseNoticeTotalPages(document),
        )
    }

    





    private fun parseNoticeTotalPages(document: Document): Int {
        val pager = document.selectFirst("div.comiis_page") ?: return 1
        pager.select("select option")
            .mapNotNull { it.attr("value").trim().toIntOrNull() }
            .maxOrNull()
            ?.let { return it.coerceAtLeast(1) }
        return pager.select("a[href]")
            .mapNotNull { RE_NOTICE_PAGE.find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
            ?.coerceAtLeast(1)
            ?: 1
    }

    
    private fun noticeFromItem(li: Element, index: Int): Notice? {
        val body = li.selectFirst(".ntc_body")
        
        val raw = (body ?: li).text()
            .replace(Regex("[\\uE000-\\uF8FF]"), "")
            .replace('\u00A0', ' ')
            .trim()
        if (raw.isEmpty()) return null
        
        
        val time = RE_NOTICE_TIME.findAll(
            (li.selectFirst("h2")?.text() ?: raw).replace('\u00A0', ' '),
        ).lastOrNull()?.value
        var text = raw
        if (time != null && text.endsWith(time)) text = text.dropLast(time.length).trim()
        
        text = text.replace(Regex("\\s*查看\\s*$"), "").trim()
            .trimEnd('·', ' ', ',', '，')
            .ifBlank { raw }
        val tid = li.select("a[href]").asSequence()
            .map { it.attr("href") }
            .mapNotNull { href ->
                RE_NOTICE_TID.find(href)?.groupValues?.get(1)
                    ?: RE_TID.find(href)?.groupValues?.get(1)
            }
            .firstOrNull()
        
        
        val noteId = li.selectFirst("a[id^=a_note_]")?.id()?.removePrefix("a_note_")
        val id = noteId ?: tid ?: "notice-$index"
        
        val avatarLink = li.selectFirst("a.notice_img")
        val avatarUrl = avatarLink?.selectFirst("img")?.attr("src")?.trim()?.takeIf { it.isNotBlank() }
        val uid = avatarLink?.attr("href")?.let { RE_UID.find(it)?.groupValues?.get(1) }
        return Notice(
            id = id,
            text = text,
            timeText = time,
            tid = tid,
            avatarUrl = avatarUrl,
            uid = uid,
        )
    }

    
    fun parseLikeUsers(html: String): List<LikeUser> {
        val box = doc(html).selectFirst("div.comiis_userlist") ?: return emptyList()
        return box.select("li a[href*=uid=]").mapNotNull { a ->
            val uid = RE_UID.find(a.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val name = a.text().replace(Regex("[\uE000-\uF8FF]"), "").trim().ifEmpty { "用户$uid" }
            LikeUser(uid = uid, name = name, avatarUrl = a.selectFirst("img")?.attr("src"))
        }
    }

    





    fun parseLikeResult(body: String, like: Boolean): LikeResult {
        val bad = RE_LIKE_ERROR.containsMatchIn(body) || body.contains("action=login", true)
        val success = body.isNotBlank() && !bad && if (like) {
            RE_LIKE_SUCCESS.containsMatchIn(body) || !body.contains("<html", true)
        } else {
            body.contains(">ok<") || body.contains("'ok'") || !body.contains("<html", true)
        }
        val remaining = RE_LIKES_DAYCOUNT.find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: RE_REMAIN_LIKES.find(body)?.groupValues?.get(1)?.toIntOrNull()
        val count = RE_RECOMMEND_COUNT.find(body)?.groupValues?.get(1)?.toIntOrNull()
        val message = RE_TIP_MESSAGE.find(body)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")
            ?.replace(Regex("您今天还能评价\\s*\\d+\\s*次"), "")
            ?.trim()
            ?.ifBlank { null }
        return LikeResult(success = success, remainingToday = remaining, serverCount = count, message = message)
    }

    
    private val RE_CDATA = Regex("(?s)<!\\[CDATA\\[(.*?)\\]\\]>")

    
    private val RE_INAJAX_XML = Regex("(?s)<root>\\s*<!\\[CDATA\\[")

    
    private val RE_ERROR_HANDLE_MSG = Regex("errorhandle_[A-Za-z0-9_]+\\s*\\(\\s*['\"]([^'\"]+)['\"]")

    







    fun parseReplyResult(body: String): ReplyResult {
        if (body.contains("errorhandle_")) return ReplyResult(false, extractReplyError(body))
        val success = body.contains("succeedhandle_") || RE_INAJAX_XML.containsMatchIn(body)
        return ReplyResult(success, if (success) null else extractReplyError(body))
    }

    




    fun parseAtUsers(body: String): List<String> =
        RE_CDATA.find(body)?.groupValues?.get(1)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    






    fun parseAtUserList(html: String): List<AtUser> =
        doc(html).select("div.comiis_userlist01 > li").mapNotNull { li ->
            val link = li.select("p.tit a[href*=\"do=profile\"]").firstOrNull()
                ?: return@mapNotNull null
            val uid = RE_UID.find(link.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val name = link.text().trim()
            if (name.isEmpty()) return@mapNotNull null
            val avatar = li.select("a.list01_limg img").firstOrNull()
                ?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
            AtUser(
                uid = uid,
                name = name,
                avatarUrl = avatar,
                subtitle = li.select("p.txt").firstOrNull()?.text()?.trim().orEmpty(),
            )
        }

    
    private fun extractReplyError(body: String): String? {
        val cdata = RE_CDATA.find(body)?.groupValues?.get(1) ?: body
        RE_ERROR_HANDLE_MSG.find(cdata)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it }
        return cdata
            .replace(Regex("(?s)<script.*?</script>"), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
    }

    private fun numberFromText(text: String): Int? =
        Regex("\\d+").find(text)?.value?.toIntOrNull()

    
    fun parseFavoriteFavid(html: String, tid: String): String? {
        val document = doc(html)
        
        for (a in document.select("a[href*=favid=]")) {
            val href = a.attr("href")
            val li = a.closest("li") ?: a
            val context = href + " " + li.html()
            if (context.contains("tid=$tid") || context.contains("thread-$tid-")) {
                Regex("favid=(\\d+)").find(context)?.groupValues?.get(1)?.let { return it }
            }
        }
        return null
    }

    
    





    fun parseSearchResult(html: String): SearchResult {
        val document = doc(html)
        val pageData = parseForumPage(html, 1)
        return SearchResult(
            threads = pageData.threads,
            nextUrl = document.selectFirst("a.nxt")?.absUrl("href")?.takeIf { it.isNotBlank() },
        )
    }

    







    fun parsePmMessages(response: String): PmPage {
        val payload = RE_CDATA.find(response)?.groupValues?.get(1) ?: response
        if (payload.contains("mod=logging") || payload.contains("action=login")) {
            return PmPage(loggedOut = true)
        }
        val document = doc(payload)
        val items = document.select("div.comiis_pmlist li, div.pm_list li")
        return PmPage(
            messages = items.mapNotNull { li ->
                val link = li.selectFirst("a[href*=\"do=pm\"]") ?: li.selectFirst("a")
                val href = link?.attr("href").orEmpty()
                val touid = Regex("""[?&]touid=(\d+)""").find(href)?.groupValues?.get(1)
                    ?: Regex("""[?&]uid=(\d+)""").find(href)?.groupValues?.get(1)
                val plid = Regex("""[?&]plid=(\d+)""").find(href)?.groupValues?.get(1)

                val h2 = li.selectFirst("h2")
                val timeText = h2?.selectFirst("span")?.text()?.trim()
                    ?: li.selectFirst("span.date, span.time")?.text()?.trim()

                
                val username = h2?.clone()?.apply { select("span").remove() }?.text()?.trim()
                    ?.ifBlank { null }
                    ?: link?.selectFirst(".name, .username")?.text()?.trim()
                    ?: "系统消息"

                val summary = li.selectFirst("p")?.text()?.trim().orEmpty()
                val avatarUrl = li.selectFirst("img")?.absUrl("src")?.takeIf { it.isNotBlank() }
                val unread = li.selectFirst(".kmnum, .kmnums, .new") != null

                PrivateMessage(
                    plid = plid,
                    touid = touid,
                    username = username,
                    avatarUrl = avatarUrl,
                    summary = summary,
                    timeText = timeText,
                    unread = unread,
                )
            },
        )
    }

    


    fun parseChatConversation(html: String, defaultTouid: String): ChatConversation {
        val document = doc(html)
        val formhash = document.selectFirst("input[name=formhash]")?.attr("value")
            ?.takeIf { it.isNotBlank() }
            ?: parseFormhash(html)

        
        
        val headName = document.selectFirst(
            "#comiis_head h2, .comiis_head h2, .header .name, .category .name, header span:not(.none)",
        )
        val headText = headName?.text().orEmpty()
        val targetName = headName?.ownText()?.trim()?.ifBlank { null }
            ?: headText.trim().removeSuffix("(在线)").removeSuffix("(离线)").trim().ifEmpty { null }
        val targetOnline = headText.takeIf { it.contains("(在线)") || it.contains("(离线)") }
            ?.contains("(在线)")
        
        
        
        
        val nodes = document.select(
            "div.comiis_self_msg, div.comiis_friend_msg, div.self_msg, div.friend_msg",
        )
            .ifEmpty {
                document.select(".msgbox li, .comiis_pmlist li")
            }
            .ifEmpty {
                document.select("div.dialog_green, div.dialog_white")
            }

        val messages = nodes.mapIndexedNotNull { index, node ->
            
            
            val isSelf = node.classNames().any { it.contains("self", ignoreCase = true) } ||
                node.selectFirst(".msg_avt.y, .msg_avt.right, .avat.y, .avat.right") != null

            
            
            val contentEl = node.selectFirst(".msg_mes")
                ?: node.selectFirst(".dialog_t, .dialog_c")
                ?: node.selectFirst("p.msg, .msg")
            val contentHtml = contentEl?.html()?.trim()
            val text = contentEl?.text()?.trim() ?: node.text().trim()
            if (text.isBlank() && !contentHtml.orEmpty().contains("<img", ignoreCase = true)) {
                return@mapIndexedNotNull null
            }

            val time = node.selectFirst(".msg_time")?.text()?.trim()
                ?: node.selectFirst(".date, .time, span.date")?.text()?.trim()

            
            
            val avatarUrl = node.selectFirst("img.msg_avt")?.absUrl("src")?.takeIf { it.isNotBlank() }
                ?: node.selectFirst(".avat img, a > img")?.absUrl("src")?.takeIf { it.isNotBlank() }
                ?: node.select("img").firstOrNull { img ->
                    val src = img.absUrl("src")
                    src.isNotBlank() &&
                        !src.contains("smiley", ignoreCase = true) &&
                        !src.contains("static/image", ignoreCase = true)
                }?.absUrl("src")

            ChatMessage(
                id = node.id().ifBlank { "${index}_${time.orEmpty()}" },
                text = text,
                contentHtml = contentHtml,
                time = time,
                isSelf = isSelf,
                avatarUrl = avatarUrl,
            )
        }

        return ChatConversation(
            touid = defaultTouid,
            targetName = targetName,
            messages = messages,
            formhash = formhash,
            targetOnline = targetOnline,
        )
    }

    private fun extractTid(document: Document, html: String): String {
        
        
        
        
        RE_JS_TID.find(html)?.groupValues?.get(1)?.let { return it }
        RE_TID.find(document.selectFirst("link[rel=canonical]")?.attr("href") ?: "")?.let {
            return it.groupValues[1]
        }
        Regex("[?&]tid=(\\d+)")
            .find(document.selectFirst("form#fastpostform")?.attr("action") ?: "")
            ?.groupValues?.get(1)?.let { return it }
        return RE_TID.find(html)?.groupValues?.get(1) ?: ""
    }

    










    private fun parseTotalPages(document: Document, tid: String): Int {
        var max = 1
        for (a in document.select("a[href*=thread-], a[href*=page]")) {
            val href = a.attr("href")
            val page = Regex("thread-\\d+-(\\d+)-1\\.html").find(href)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("[?&]page=(\\d+)").find(href)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("page-(\\d+)\\.html").find(href)?.groupValues?.get(1)?.toIntOrNull()
                ?: continue
            
            
            if (page > max && (tid.isBlank() || linkTid(href)?.let { it == tid } != false)) max = page
        }
        return max
    }

    
    private fun linkTid(href: String): String? =
        Regex("thread-(\\d+)-").find(href)?.groupValues?.get(1)
            ?: Regex("[?&]tid=(\\d+)").find(href)?.groupValues?.get(1)

    
    fun parseCheckInStatus(html: String): CheckInStatus {
        val formhash = parseFormhash(html)
        val canSign = html.contains("id=\"signresult\"")
        val hasSigned = html.contains(">已签到<") ||
            html.contains("class=\"comiis_btn bg_b f_c\">已签到") ||
            (!canSign && html.contains("已签到"))
        return CheckInStatus(hasSigned = hasSigned, formhash = formhash)
    }

    




    fun parseCheckInResponse(response: String): CheckInResult {
        val cdata = RE_CDATA.find(response)?.groupValues?.get(1)?.trim() ?: response.trim()
        if (cdata.contains("已签到") || cdata.contains("今日已签")) {
            return CheckInResult(success = true, message = if (cdata.contains("今日已签")) "今日已签到" else "签到成功")
        }
        val cleanMsg = cdata
            .replace(Regex("(?s)<script.*?</script>"), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return CheckInResult(
            success = false,
            message = cleanMsg.ifBlank { "签到失败" },
        )
    }

    private fun hasNextPage(document: Document): Boolean =
        document.selectFirst("a.nxt, a[href*=page=] i.icon-next, .pg a.nxt") != null

    
    fun parseMyThreads(html: String, page: Int): MyThreadPage {
        val document = doc(html)
        val threads = mutableListOf<Thread>()
        for (li in document.select("li.forumlist_li")) {
            val titleA = li.selectFirst("div.mmlist_li_box h2 a[href*=thread-]")
                ?: li.selectFirst("a[href*=thread-]")
                ?: continue
            val tid = RE_TID.find(titleA.attr("href"))?.groupValues?.get(1) ?: continue
            val authorA = li.selectFirst("a.top_user")
            val (replyCount, viewCount, likeCount) = readCardStats(li)
            threads.add(
                Thread(
                    tid = tid,
                    subject = titleA.text().stripInvisibleChars(),
                    authorName = authorA?.text()?.trim(),
                    authorUid = RE_UID.find(authorA?.attr("href") ?: "")?.groupValues?.get(1),
                    avatarUrl = li.selectFirst("img.top_tximg")?.attr("src"),
                    level = li.selectFirst("span.top_lev")?.text()?.trim(),
                    timeText = li.selectFirst("div.forumlist_li_time span")?.text()?.trim(),
                    replyCount = replyCount,
                    viewCount = viewCount,
                    excerpt = li.selectFirst("div.list_body a")?.text()?.stripInvisibleChars(),
                    forumName = li.selectFirst("div.comiis_xznalist_bk a[href*=forum-]")?.let { a ->
                        a.ownText().trim().ifEmpty { a.text().trim() }
                    }?.ifEmpty { null }
                        ?: li.selectFirst("div.forumlist_li_time a[href*=forum-]")?.text()
                            ?.substringAfter("来自")?.trim()?.takeIf { it.isNotEmpty() },
                    imageUrls = collectThreadImages(li),
                    likeCount = likeCount,
                )
            )
        }
        return MyThreadPage(
            threads = threads.distinctBy { it.tid },
            page = page,
            hasNext = hasNextPage(document),
        )
    }

    
    fun parseFavorites(html: String, page: Int): FavoritePage {
        val document = doc(html)
        val items = mutableListOf<FavoriteItem>()
        val listElements = document.select("ul.comiis_favoritelist li, .comiis_p12 li, ul#favorite_ul li, div.comiis_body li, li[id^=fav_], li.mysclist_li")
        for (el in listElements) {
            val deleteA = el.selectFirst("a[href*=op=delete]") ?: el.selectFirst("a[href*=favid=]")
            val favid = deleteA?.let { Regex("""favid=(\d+)""").find(it.attr("href"))?.groupValues?.get(1) }
                ?: Regex("""fav_(\d+)""").find(el.attr("id"))?.groupValues?.get(1)
                ?: ""
            val titleA = el.selectFirst("a[href*=thread-], a[href*=mod=viewthread], a[href*=forum-], a[href*=mod=forumdisplay]")
                ?: continue
            val href = titleA.attr("href")
            val isForum = href.contains("forum-") || href.contains("mod=forumdisplay")
            val id = (if (isForum) RE_FID.find(href)?.groupValues?.get(1) else RE_TID.find(href)?.groupValues?.get(1))
                ?: Regex("""[?&](?:tid|id)=(\d+)""").find(href)?.groupValues?.get(1)
                ?: ""
            val title = titleA.text().stripInvisibleChars().ifEmpty { continue }
            val dateText = el.selectFirst("span.f_d, span.xg1, div.forumlist_li_time span")?.text()?.trim()
            val desc = el.selectFirst("div.list_body, p.txt")?.text()?.stripInvisibleChars()
            items.add(
                FavoriteItem(
                    favid = favid,
                    id = id,
                    title = title,
                    description = desc,
                    dateText = dateText,
                    type = if (isForum) "forum" else "thread",
                )
            )
        }
        return FavoritePage(
            items = items.distinctBy { if (it.favid.isNotEmpty()) it.favid else it.id },
            page = page,
            hasNext = hasNextPage(document),
        )
    }

    






    private fun profileLink(li: Element): Element? =
        li.selectFirst("p.tit a[href*=do=profile]")
            ?: li.selectFirst("a.top_user")
            ?: li.select("a[href*=do=profile]").firstOrNull { it.text().isNotBlank() }

    
    fun parseFriends(html: String, page: Int): FriendUserPage {
        val document = doc(html)
        val items = mutableListOf<FriendUserItem>()
        val listElements = document.select("div.comiis_userlist01 > li, ul.comiis_friend_list > li, li[id^=comiis_friendbox_], li[id^=friend_], .comiis_p12 li")
        for (li in listElements) {
            val link = profileLink(li) ?: continue
            val href = link.attr("href")
            val uid = RE_UID.find(href)?.groupValues?.get(1)
                ?: Regex("""comiis_friendbox_(\d+)""").find(li.attr("id"))?.groupValues?.get(1)
                ?: continue
            val name = link.text().trim().ifEmpty { continue }
            val avatarUrl = li.selectFirst("img")?.absUrl("src")?.takeIf { it.isNotBlank() }
            
            
            val desc = li.selectFirst("p.txt")?.select("font")
                ?.joinToString(" ") { it.text().trim() }?.trim()?.takeIf { it.isNotEmpty() }
                ?: li.selectFirst("span.f_d")?.text()?.trim()?.takeIf { it.isNotEmpty() }
            items.add(FriendUserItem(uid = uid, username = name, avatarUrl = avatarUrl, description = desc))
        }
        return FriendUserPage(
            users = items.distinctBy { it.uid },
            page = page,
            hasNext = hasNextPage(document),
        )
    }

    
    fun parseFriendRequests(html: String, page: Int): FriendRequestPage {
        val document = doc(html)
        val rows = document.select(
            "div.comiis_userlist01 > li, li.b_t, ul.comiis_friend_list > li, .comiis_p12 li",
        ).ifEmpty {
            
            document.select("li:has(a[href*=\"do=profile\"])")
        }
        val items = rows.mapNotNull { li ->
            val link = li.selectFirst("p.tit a[href*=profile]")
                ?: li.selectFirst("a[href*=do=profile]")
                ?: return@mapNotNull null
            val uid = RE_UID.find(link.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val name = link.text().trim().ifBlank { return@mapNotNull null }
            val avatar = li.selectFirst("a.list01_limg img, img[src*=avatar]")?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
            val desc = li.selectFirst("p.txt")?.text()?.trim()?.takeIf { it.isNotEmpty() }
            FriendRequestItem(uid = uid, username = name, avatarUrl = avatar, description = desc)
        }.distinctBy { it.uid }
        return FriendRequestPage(
            requests = items,
            page = page,
            hasNext = hasNextPage(document),
            formhash = parseFormhashAny(html),
        )
    }

    
    fun parseFollowUsers(html: String, page: Int, defaultIsFollowing: Boolean): FollowUserPage {
        val document = doc(html)
        val items = mutableListOf<FollowUserItem>()
        val elements = document.select("div.comiis_userlist01 > li, li[id^=comiis_friendbox_]")
        for (li in elements) {
            val link = li.selectFirst("p.tit a[href*=do=profile]") ?: li.selectFirst("a[href*=do=profile]") ?: continue
            val href = link.attr("href")
            val uid = RE_UID.find(href)?.groupValues?.get(1)
                ?: Regex("""comiis_friendbox_(\d+)""").find(li.attr("id"))?.groupValues?.get(1)
                ?: continue
            val name = link.text().trim().ifEmpty { continue }
            val avatarUrl = li.selectFirst("a.list01_limg img, img")?.absUrl("src")?.takeIf { it.isNotBlank() }
            val txt = li.selectFirst("p.txt")?.text().orEmpty()
            val fansCount = Regex("""(\d+)\s*粉丝""").find(txt)?.groupValues?.get(1)
                ?: li.selectFirst("code[id^=followernum_]")?.text()?.trim()
            val followCount = Regex("""(\d+)\s*关注""").find(txt)?.groupValues?.get(1)
            val hasCancelA = li.selectFirst("a.user_gz[href*=op=del]") != null
            val isFollowing = if (hasCancelA) true else defaultIsFollowing
            
            
            val remark = li.selectFirst("font[id^=followbkame_]")
                ?.text()?.trim()?.takeIf { it.isNotEmpty() }
            items.add(
                FollowUserItem(
                    uid = uid,
                    username = name,
                    avatarUrl = avatarUrl,
                    fansCount = fansCount,
                    followCount = followCount,
                    isFollowing = isFollowing,
                    remark = remark,
                )
            )
        }
        return FollowUserPage(
            users = items.distinctBy { it.uid },
            page = page,
            hasNext = hasNextPage(document),
        )
    }

    private val RE_POSTTIME_INPUT = Regex("""name="posttime"\s+(?:id="posttime"\s+)?value="(\d+)"""")
    private val RE_POSTTIME_JS = Regex("""posttime\s*=\s*['"](\d+)['"]""")
    private val RE_TID_MATCH = Regex("""thread-(\d+)|tid=(\d+)""")

    





    fun parseUploadCredentials(html: String): Pair<String, String>? {
        val match = RE_UPLOAD_PAIR.find(html) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    


    fun parseNewThreadInit(html: String): NewThreadPageData {
        val formhash = parseFormhash(html).orEmpty()
        val posttime = RE_POSTTIME_INPUT.find(html)?.groupValues?.get(1)
            ?: RE_POSTTIME_JS.find(html)?.groupValues?.get(1)
        val uploadUid = RE_UPLOAD_UID.find(html)?.groupValues?.get(1)
        val uploadHash = RE_UPLOAD_HASH.find(html)?.groupValues?.get(1)

        val document = doc(html)
        val types = threadTypes(document)
        val permissionDenied = if (html.contains("抱歉，您没有权限在该版块发帖") || html.contains("您目前没有权限发帖")) {
            "抱歉，您没有权限在该版块发帖"
        } else if (html.contains("抱歉，您尚未登录，没有权限发帖")) {
            "请先登录后再发帖"
        } else {
            null
        }

        return NewThreadPageData(
            formhash = formhash,
            posttime = posttime,
            uploadUid = uploadUid,
            uploadHash = uploadHash,
            types = types,
            permissionDenied = permissionDenied,
        )
    }

    
    private fun threadTypes(document: Document): List<ThreadType> {
        val select = document.selectFirst("select[name=typeid], select#typeid") ?: return emptyList()
        return select.select("option").mapNotNull { opt ->
            val value = opt.attr("value").trim()
            val text = opt.text().trim()
            if (value.isNotEmpty() && value != "0" && text.isNotEmpty() && text != "请选择") {
                ThreadType(id = value, name = text)
            } else {
                null
            }
        }
    }

    





    fun parseEditPostInit(html: String): EditPostPageData {
        val document = doc(html)
        val subject = document.selectFirst("input#subject, input[name=subject]")?.attr("value")
        val permissionDenied = when {
            document.selectFirst("form#loginform") != null || html.contains("action=login") -> "请先登录后再编辑"
            html.contains("没有权限") -> "没有编辑这条内容的权限"
            
            
            html.contains("不能再进行编辑") -> "该内容已超过可编辑时限，不能再进行编辑"
            else -> null
        }
        return EditPostPageData(
            formhash = parseFormhash(html).orEmpty(),
            subject = subject,
            message = editMessageText(document),
            attachments = editAttachments(document),
            uploadUid = RE_UPLOAD_UID.find(html)?.groupValues?.get(1),
            uploadHash = RE_UPLOAD_HASH.find(html)?.groupValues?.get(1),
            types = threadTypes(document),
            permissionDenied = permissionDenied,
        )
    }

    




    private fun editAttachments(document: Document): List<EditAttachment> {
        return document.select("ul.comiis_post_imglist > li").mapNotNull { li ->
            if (li.hasClass("up_btn")) return@mapNotNull null
            val img = li.selectFirst("img[id^=aimg_]") ?: return@mapNotNull null
            val aid = li.selectFirst("span.del[aid]")?.attr("aid")?.trim()
                ?: img.id().removePrefix("aimg_").ifEmpty { null }
            val url = img.attr("src").trim()
            if (aid.isNullOrEmpty() || url.isEmpty()) return@mapNotNull null
            EditAttachment(aid = aid, url = url, fileName = img.attr("title").trim())
        }
    }

    
    private fun editMessageText(document: Document): String {
        val textarea = document.selectFirst("textarea[name=message], textarea#message")
            ?: document.selectFirst("textarea")
        val raw = textarea?.wholeText().orEmpty()
        if (raw.isNotBlank()) return raw
        return document.selectFirst("input[name=message]")?.attr("value").orEmpty()
    }

    







    fun parseEditPostResult(body: String, tid: String): EditPostResult {
        val stillForm =
            RE_EDIT_FORM_ACTION.containsMatchIn(body) && RE_EDIT_SUBMIT.containsMatchIn(body)
        val landedOnThread = tid.isNotBlank() && (
            body.contains("thread-$tid") ||
                body.contains("viewthread&tid=$tid") ||
                body.contains("viewthread&amp;tid=$tid")
            )
        if (!stillForm && (landedOnThread || RE_EDIT_SUCCESS.containsMatchIn(body))) {
            return EditPostResult(success = true)
        }
        val tip = RE_EDIT_TIP.find(body)?.groupValues?.getOrNull(1)?.trim()
        return EditPostResult(success = false, message = tip ?: "编辑未生效，请稍后重试")
    }

    


    fun parseNewThreadResult(body: String): NewThreadResult {
        if (body.contains("errorhandle_")) {
            return NewThreadResult(success = false, message = extractReplyError(body))
        }
        if (body.contains("succeedhandle_")) {
            val tid = RE_TID_MATCH.find(body)?.let { m ->
                m.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    ?: m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
            }
            return NewThreadResult(success = true, tid = tid)
        }
        val tid = RE_TID_MATCH.find(body)?.let { m ->
            m.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                ?: m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
        }
        if (tid != null) {
            return NewThreadResult(success = true, tid = tid)
        }
        if (body.contains("非常感谢") || body.contains("主题已经发布") || body.contains("帖子发布成功")) {
            return NewThreadResult(success = true)
        }
        val errorMsg = extractReplyError(body)
            ?: RE_TIP_MESSAGE.find(body)?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim()
            ?: if (body.contains("两次发表间隔")) "抱歉，您两次发表间隔过短，请稍后再试" else null
        return NewThreadResult(success = false, message = errorMsg ?: "发帖未成功，请检查内容或重试")
    }
}
