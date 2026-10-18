package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.ForumGroup
import com.aazhou.mtforum.data.model.AtUser
import com.aazhou.mtforum.data.model.CheckInResult
import com.aazhou.mtforum.data.model.CheckInStatus
import com.aazhou.mtforum.data.model.EditPostPageData
import com.aazhou.mtforum.data.model.EditPostResult
import com.aazhou.mtforum.data.model.ForumPage
import com.aazhou.mtforum.data.model.HomeData
import com.aazhou.mtforum.data.model.LikeResult
import com.aazhou.mtforum.data.model.LikeUser
import com.aazhou.mtforum.data.model.LoginResult
import com.aazhou.mtforum.data.model.Notice
import com.aazhou.mtforum.data.model.NoticePage
import com.aazhou.mtforum.data.model.NoticeTab
import com.aazhou.mtforum.data.model.PmPage
import com.aazhou.mtforum.data.model.PrivateMessage
import com.aazhou.mtforum.data.model.ChatConversation
import com.aazhou.mtforum.data.model.ReplyResult
import com.aazhou.mtforum.data.model.SearchResult
import com.aazhou.mtforum.data.model.ThreadPage
import com.aazhou.mtforum.data.model.UserProfile
import com.aazhou.mtforum.data.model.FavoriteItem
import com.aazhou.mtforum.data.model.FavoritePage
import com.aazhou.mtforum.data.model.FollowUserItem
import com.aazhou.mtforum.data.model.FollowUserPage
import com.aazhou.mtforum.data.model.FriendUserItem
import com.aazhou.mtforum.data.model.FriendUserPage
import com.aazhou.mtforum.data.model.MyThreadPage
import com.aazhou.mtforum.data.model.NewThreadPageData
import com.aazhou.mtforum.data.model.NewThreadResult
import com.aazhou.mtforum.data.log.LogLevel
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.network.WafSolver
import com.aazhou.mtforum.data.parser.ForumParser
import com.aazhou.mtforum.data.parser.SmileyPack
import com.aazhou.mtforum.data.parser.SmileyParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File


private val REPLY_ERROR_PATTERN = Regex("抱歉|非法操作|请先登录|需要先登录|没有权限|审核")


private val LOGIN_DIAG_HINTS = listOf(
    "密码错误", "密码不正确", "密码为空", "用户名不存在", "账号不存在", "该用户不存在",
    "需要验证码", "请填写验证码", "验证码错误", "请输入验证码", "seccode",
    "失败次数", "次数过多", "请稍后再试", "请稍后重试", "锁定", "安全提问", "answer",
)


private fun rawSnippet(body: String, key: String, radius: Int = 250): String? {
    val i = body.indexOf(key)
    if (i < 0) return null
    val start = (i - radius).coerceAtLeast(0)
    val end = (i + radius).coerceAtMost(body.length)
    return body.substring(start, end)
}


private val LIKE_ERROR_PATTERN = Regex(
    "没有点赞权限|不能点赞|今日评价机会已用完|关闭的主题|请先登录|formhash错误|非法操作|不能收藏|submit_invalid|errorhandle_",
)


private val REPORT_ERROR_PATTERN = Regex("举报理由|action=login|请先登录")






private val DELETE_FORM_ACTION = Regex("action=edit")
private val DELETE_FORM_SUBMIT = Regex("editsubmit")




enum class ForumSort(val label: String) {
    LATEST("最新"),
    HOT("热门"),
    DIGEST("精华"),
    POLL("投票"),
}


private val REWARD_SUCCESS_PATTERN =
    Regex("succeedhandle_rate|rate_success|评分成功|打赏成功|评价成功|您已成功评分")


private val REWARD_FAIL_PATTERN = Regex(
    "24小时|24 小时|已经评分|已评价|已评分|重复评分|评分失败|积分不足|余额不足|formhash错误|非法操作|没有权限|请先登录",
)


private val TOPICADMIN_SUCCESS = Regex("管理操作成功")


private val SMILEY_URL = "${HttpClient.BASE_URL}data/cache/common_smilies_var.js"


private const val SMILEY_REVALIDATE_INTERVAL_MS = 12 * 60 * 60 * 1000L


private val RE_ATTACHIMG_AID = Regex("""\[attachimg\](\d+)\[/attachimg\]""", RegexOption.IGNORE_CASE)









private val RE_EDIT_QUOTE_TAG = Regex("""\[/?(?:quote|size)\b[^\]]*]""", RegexOption.IGNORE_CASE)


internal fun protectQuoteTagsOnEdit(message: String): String =
    RE_EDIT_QUOTE_TAG.replace(message) { it.value.uppercase() }


private const val GUIDE_VIEW_PORTAL = "hot"



private const val ATTACHMENT_CDN = "https://cdn.binmt.cc/"

class ForumRepository(
    private val http: HttpClient,
    private val smileyStore: SmileyStore,
) {

    
    var blockedUidsProvider: () -> Set<String> = { emptySet() }
    private fun currentBlocked(): Set<String> = blockedUidsProvider()


    
    private var smileyCache: List<SmileyPack>? = null

    
    private fun attachAids(message: String): List<String> =
        RE_ATTACHIMG_AID.findAll(message).map { it.groupValues[1] }.distinct().toList()

    
    fun attachAidsIn(message: String): List<String> = attachAids(message)

    









    suspend fun deleteAttach(
        tid: String,
        pid: String,
        aid: String,
        formhash: String,
    ): String = withContext(Dispatchers.IO) {
        val url = http.url(
            "forum.php?mod=ajax&action=deleteattach&inajax=yes&formhash=$formhash" +
                "&aids[]=$aid&tid=$tid&pid=$pid",
        )
        val body = http.get(url)
        val cd = Regex("""<!\[CDATA\[([\s\S]*?)\]\]>""").find(body)
        cd?.groupValues?.get(1)?.trim().orEmpty()
    }


    
    suspend fun postFormhash(fid: String): String? = withContext(Dispatchers.IO) {
        ForumParser.parseFormhash(http.get(http.url("forum.php?mod=post&action=newthread&fid=$fid&mobile=2")))
    }

    
    suspend fun fetchNewThreadInit(fid: String): NewThreadPageData = withContext(Dispatchers.IO) {
        val html = http.get(http.url("forum.php?mod=post&action=newthread&fid=$fid&mobile=2"))
        ForumParser.parseNewThreadInit(html)
    }

    fun isLoggedIn(): Boolean = http.isLoggedIn()

    





    suspend fun smileyPacks(): List<SmileyPack> = withContext(Dispatchers.IO) {
        
        smileyCache ?: loadSmileyPacks().also { packs -> if (packs.isNotEmpty()) smileyCache = packs }
    }

    private suspend fun loadSmileyPacks(): List<SmileyPack> {
        val cached = smileyStore.packs() ?: run {
            
            val body = runCatching { http.get(SMILEY_URL) }.getOrNull() ?: return emptyList()
            return SmileyParser.parse(body).also { smileyStore.savePacks(it, null) }
        }
        if (System.currentTimeMillis() - cached.checkedAt < SMILEY_REVALIDATE_INTERVAL_MS) return cached.packs

        val fetched = try {
            http.getIfChanged(SMILEY_URL, cached.etag)
        } catch (e: Exception) {
            
            return cached.packs
        }
        
        if (fetched == null) {
            smileyStore.markChecked()
            return cached.packs
        }
        return SmileyParser.parse(fetched.body).also { smileyStore.savePacks(it, fetched.etag) }
    }

    
    fun sessionCookieHeader(): String = http.cookieHeader()

    suspend fun forumGroups(): List<ForumGroup> = withContext(Dispatchers.IO) {
        ForumParser.parseForumList(http.get(http.url("forum.php?forumlist=1&mobile=2")))
    }

    
    suspend fun home(): HomeData = withContext(Dispatchers.IO) {
        val data = ForumParser.parseHome(http.get(http.url("forum.php?mod=guide&view=hot&mobile=2")))
        val blocked = currentBlocked()
        if (blocked.isEmpty()) data
        else data.copy(hotThreads = data.hotThreads.filterNot { it.authorUid in blocked })
    }

    






    suspend fun guideFeed(view: String, page: Int): ForumPage = withContext(Dispatchers.IO) {
        val blocked = currentBlocked()
        if (view == GUIDE_VIEW_PORTAL) {
            
            
            
            val threads = ForumParser.parsePortalFeed(
                http.get(http.url("plugin.php?id=comiis_app_portal&pid=1&page=$page&inajax=1")),
            )
            ForumPage(
                forumName = null,
                announcements = emptyList(),
                threads = if (blocked.isEmpty()) threads else threads.filterNot { it.authorUid in blocked },
                page = page,
                hasNext = threads.isNotEmpty(),
            )
        } else {
            val fp = ForumParser.parseForumPage(
                http.get(http.url("forum.php?mod=guide&view=$view&page=$page&mobile=2")),
                page,
            )
            if (blocked.isEmpty()) fp
            else fp.copy(
                announcements = fp.announcements.filterNot { it.authorUid in blocked },
                threads = fp.threads.filterNot { it.authorUid in blocked },
            )
        }
    }

    suspend fun forumPage(fid: String, page: Int, sort: ForumSort = ForumSort.LATEST): ForumPage =
        withContext(Dispatchers.IO) {
            
            
            val path = when (sort) {
                ForumSort.LATEST -> "forum-$fid-$page.html"
                ForumSort.HOT -> "forum.php?mod=forumdisplay&fid=$fid&filter=heat&orderby=heats&page=$page"
                ForumSort.DIGEST -> "forum.php?mod=forumdisplay&fid=$fid&filter=digest&digest=1&page=$page"
                ForumSort.POLL -> "forum.php?mod=forumdisplay&fid=$fid&filter=specialtype&specialtype=poll&page=$page"
            }
            val fp = ForumParser.parseForumPage(http.get(http.url(path)), page)
            val blocked = currentBlocked()
            if (blocked.isEmpty()) fp
            else fp.copy(
                announcements = fp.announcements.filterNot { it.authorUid in blocked },
                threads = fp.threads.filterNot { it.authorUid in blocked },
            )
        }

    
    suspend fun threadPage(
        tid: String,
        page: Int,
        orderType: Int = 0,
        authorId: String? = null,
    ): ThreadPage = withContext(Dispatchers.IO) {
        val order = if (orderType == 1) "&ordertype=1" else ""
        val author = if (!authorId.isNullOrBlank()) "&authorid=$authorId" else ""
        val tp = ForumParser.parseThreadPage(
            http.get(http.url("forum.php?mod=viewthread&tid=$tid&page=$page$order$author&mobile=2")),
            page,
        )
        val blocked = currentBlocked()
        if (blocked.isEmpty()) tp
        else tp.copy(posts = tp.posts.filterNot { it.authorUid in blocked })
    }

    








    suspend fun reply(
        fid: String,
        tid: String,
        formhash: String,
        message: String,
        noticeAuthor: String? = null,
        noticeTrimStr: String? = null,
    ): ReplyResult = withContext(Dispatchers.IO) {
        
        
        
        val url = http.url(
            "forum.php?mod=post&action=reply&fid=$fid&tid=$tid&extra=&replysubmit=yes&mobile=2" +
                "&handlekey=fastpost&loc=1&inajax=1",
        )
        val params = mutableMapOf(
            "formhash" to formhash,
            "message" to message,
        )
        if (!noticeAuthor.isNullOrBlank()) params["noticeauthor"] = noticeAuthor
        if (!noticeTrimStr.isNullOrBlank()) params["noticetrimstr"] = noticeTrimStr
        
        
        for (aid in attachAids(message)) params["attachnew[$aid][description]"] = ""
        val body = http.post(url, params, referer = "https://bbs.binmt.cc/thread-$tid-1-1.html")
        val result = ForumParser.parseReplyResult(body)
        
        
        if (!result.success && !body.contains("errorhandle_") && REPLY_ERROR_PATTERN.containsMatchIn(body)) {
            ReplyResult(success = false, message = "尚未登录或会话已过期，请重新进入帖子")
        } else {
            result
        }
    }

    





    suspend fun uploadReplyImage(
        file: File,
        uid: String,
        hash: String,
    ): String? = uploadImage(file, uid, hash)?.aid

    



    data class ImageUpload(val aid: String, val path: String) {
        val url: String get() = ATTACHMENT_CDN + "data/attachment/forum/" + path
    }

    
    suspend fun uploadImage(
        file: File,
        uid: String,
        hash: String,
    ): ImageUpload? = withContext(Dispatchers.IO) {
        val body = http.upload(
            url = "${HttpClient.BASE_URL}misc.php?mod=swfupload&operation=upload&type=image&inajax=yes&infloat=yes&simple=2",
            file = file,
            fieldName = "Filedata",
            extraFields = mapOf("uid" to uid, "hash" to hash),
        )
        
        val parts = body.split('|')
        if (parts.size >= 6 && parts[0] == "DISCUZUPLOAD" && parts[2] == "0") {
            ImageUpload(aid = parts[3], path = parts[5])
        } else {
            null
        }
    }

    






    suspend fun fetchUploadCredentials(): Pair<String, String>? = withContext(Dispatchers.IO) {
        val pages = mutableListOf<String>()
        runCatching { myThreads() }.getOrNull()?.threads?.firstOrNull()?.tid
            ?.let { pages += "thread-$it-1-1.html" }
        runCatching { home() }.getOrNull()?.hotThreads?.firstOrNull()?.tid
            ?.let { pages += "thread-$it-1-1.html" }
        runCatching { forumGroups() }.getOrNull()?.firstOrNull()?.forums?.firstOrNull()?.fid
            ?.let { pages += "forum.php?mod=post&action=newthread&fid=$it&mobile=2" }
        for (page in pages) {
            val html = runCatching { http.get(http.url(page)) }.getOrNull() ?: continue
            ForumParser.parseUploadCredentials(html)?.let { return@withContext it }
        }
        null
    }

    




    suspend fun fetchAtUsers(): List<String> = withContext(Dispatchers.IO) {
        ForumParser.parseAtUsers(
            http.get("${HttpClient.BASE_URL}misc.php?mod=getatuser&inajax=1"),
        )
    }

    





    suspend fun fetchFriends(): List<AtUser> = withContext(Dispatchers.IO) {
        atUserPages { page -> "home.php?mod=space&do=friend&page=$page&mobile=2" }
    }

    
    suspend fun fetchFollows(): List<AtUser> = withContext(Dispatchers.IO) {
        atUserPages { page -> "home.php?mod=follow&do=following&page=$page&mobile=2" }
    }

    




    suspend fun searchUsers(keyword: String): List<AtUser> = withContext(Dispatchers.IO) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
        ForumParser.parseAtUserList(
            http.get(http.url("search.php?mod=user&searchsubmit=yes&srchtxt=$encoded&mobile=2")),
        )
    }

    private suspend fun atUserPages(url: (Int) -> String): List<AtUser> {
        val all = mutableListOf<AtUser>()
        var page = 1
        while (page <= MAX_AT_USER_PAGES) {
            val batch = ForumParser.parseAtUserList(http.get(http.url(url(page))))
            all += batch
            if (batch.size < AT_USER_PAGE_SIZE) break
            page++
        }
        return all
    }

    






    suspend fun setLike(tid: String, formhash: String, like: Boolean): LikeResult = withContext(Dispatchers.IO) {
        val url = if (like) {
            http.url(
                "forum.php?mod=misc&action=recommend&handlekey=recommend_add" +
                    "&do=add&tid=$tid&hash=$formhash&inajax=1&mobile=2",
            )
        } else {
            http.url("plugin.php?id=comiis_app&comiis=re_recommend&tid=$tid&inajax=1")
        }
        ForumParser.parseLikeResult(http.get(url), like)
    }

    





    suspend fun setFavorite(tid: String, formhash: String, favorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (favorite) {
            val url = http.url(
                "home.php?mod=spacecp&ac=favorite&type=thread&id=$tid" +
                    "&formhash=$formhash&handlekey=comiis&inajax=1&mobile=2",
            )
            val body = http.get(url)
            body.isNotBlank() && !LIKE_ERROR_PATTERN.containsMatchIn(body)
        } else {
            val favid = favoriteFavid(tid) ?: return@withContext false
            deleteFavorite(favid)
        }
    }

    




    private suspend fun favoriteFavid(tid: String): String? {
        val html = http.get(http.url("home.php?mod=space&do=favorite&view=me&type=all&mobile=2"))
        return ForumParser.parseFavoriteFavid(html, tid)
    }

    





    suspend fun deleteFavorite(favid: String): Boolean = withContext(Dispatchers.IO) {
        if (favid.isBlank()) return@withContext false
        val favoritePage = http.url("home.php?mod=space&do=favorite&view=me&type=all&mobile=2")
        val pageHtml = runCatching { http.get(favoritePage) }.getOrDefault("")
        val formhash = ForumParser.parseFormhashAny(pageHtml) ?: return@withContext false
        val url = http.url(
            "home.php?mod=spacecp&ac=favorite&op=delete&favid=$favid&type=all" +
                "&formhash=$formhash&deletesubmit=true&handlekey=comiis&inajax=1&mobile=2",
        )
        val body = http.post(
            url,
            mapOf(
                "formhash" to formhash,
                "deletesubmit" to "true",
                "handlekey" to "comiis",
            ),
            referer = favoritePage,
        )
        body.isNotBlank() && !LIKE_ERROR_PATTERN.containsMatchIn(body)
    }

    
    



    
    suspend fun fetchForumTodayPosts(): Map<String, Int> = withContext(Dispatchers.IO) {
        val url = http.url("forum.php?forumlist=1&mobile=2&_ts=" + System.currentTimeMillis())
        val html = try { http.get(url) } catch (e: Exception) {
            android.util.Log.e("FollowForum", "fetchTodayPosts ex=${e.message}")
            return@withContext emptyMap()
        }
        val out = HashMap<String, Int>()
        try {
            val doc = org.jsoup.Jsoup.parse(html)
            
            var links = doc.select("div.comiis_forum_nbox li a[href*=forum-]")
            if (links.isEmpty()) {
                links = doc.select("a[href*=forum-]")  
            }
            val fidPtn = Regex("forum-(\\d+)-")
            for (link in links) {
                val m = fidPtn.find(link.attr("href")) ?: continue
                val fid = m.groupValues[1]
            
                val span = link.selectFirst("em span")
                val n = span?.text()?.trim()?.toIntOrNull()
                if (n != null && n > 0) {
                    out[fid] = n
                } else if (!out.containsKey(fid)) {
                    out[fid] = 0
                }
            }
            android.util.Log.d("FollowForum", "fetchTodayPosts 解析到 ${out.size} 个板块")
        } catch (e: Exception) {
            android.util.Log.e("FollowForum", "fetchTodayPosts parse ex=${e.message}")
        }
        out
    }

    suspend fun fetchFollowedForumFids(): Map<String, String> = withContext(Dispatchers.IO) {
        val url = http.url("home.php?mod=space&do=favorite&type=forum&mobile=2&_ts=" + System.currentTimeMillis())
        val html = try { http.get(url) } catch (e: Exception) {
            android.util.Log.e("FollowForum", "fetchFollowed ex=${e.message}")
            return@withContext emptyMap()
        }

        
        android.util.Log.d("FollowForum", "fetchFollowed html.len=${html.length}")
        android.util.Log.d("FollowForum", "fetchFollowed head=${html.take(500)}")

        val out = HashMap<String, String>()

        try {
            val doc = org.jsoup.Jsoup.parse(html)
            
            var items = doc.select("li.mysclist_li")
            android.util.Log.d("FollowForum", "Jsoup li.mysclist_li = ${items.size}")

            
            if (items.isEmpty()) {
                items = doc.select("li:has(a[href*=favid=])")
                android.util.Log.d("FollowForum", "Jsoup fallback li:has(a[href*=favid]) = ${items.size}")
            }

            val favidPtn = Regex("favid=(\\d+)")
            val fidPtn = Regex("forum-(\\d+)-1")

            for (li in items) {
                val delLink = li.select("a[href*=favid=]").first() ?: continue
                val favid = favidPtn.find(delLink.attr("href"))?.groupValues?.get(1) ?: continue
                val forumLink = li.select("a[href*=forum-]").first() ?: continue
                val fid = fidPtn.find(forumLink.attr("href"))?.groupValues?.get(1) ?: continue
                out[fid] = favid
            }
        } catch (e: Exception) {
            android.util.Log.e("FollowForum", "Jsoup ex=${e.message}")
        }

        android.util.Log.d("FollowForum", "已关注板块: $out")
        out
    }

    



    suspend fun followForum(fid: String, formhash: String): Boolean = withContext(Dispatchers.IO) {
        val url = http.url(
            "home.php?mod=spacecp&ac=favorite&type=forum&id=$fid&formhash=$formhash&handlekey=forum_fav&inajax=1"
        )
        try {
            val resp = http.get(url)
            android.util.Log.d("FollowForum", "follow resp=${resp.take(200)}")
            resp.contains("成功") || resp.contains("收藏")
        } catch (e: Exception) {
            android.util.Log.e("FollowForum", "follow ex=${e.message}")
            false
        }
    }

    



    suspend fun unfollowForum(favid: String, formhash: String): Boolean = withContext(Dispatchers.IO) {
        
        val url = http.url(
            "home.php?mod=spacecp&ac=favorite&op=delete&favid=$favid&type=forum" +
                    "&mobile=2&handlekey=favoriteform_$favid&inajax=1"
        )
        try {
            val params = HashMap<String, String>()
            params["referer"] = "https://bbs.binmt.cc/home.php?mod=space&do=favorite&type=forum&mobile=2"
            params["deletesubmit"] = "true"
            params["formhash"] = formhash
            params["handlekey"] = "favorite_del"
            val resp = http.post(url, params, referer = "https://bbs.binmt.cc/forum.php?mobile=2")
            android.util.Log.d("FollowForum", "unfollow resp=${resp.take(300)}")
            
            val isFail = resp.contains("来路不正确") || resp.contains("表单验证串")
                    || resp.contains("无法提交") || resp.contains("不存在")
            !isFail && (resp.contains("成功") || resp.contains("删除") || resp.contains("取消"))
        } catch (e: Exception) {
            android.util.Log.e("FollowForum", "unfollow ex=${e.message}")
            false
        }
    }

    suspend fun followTopic(uid: String, formhash: String, follow: Boolean): String = withContext(Dispatchers.IO) {
        val url = http.url(
            "home.php?mod=spacecp&ac=follow&op=${if (follow) "add" else "del"}" +
                "&fuid=$uid&hash=$formhash&mobile=2",
        )
        http.get(url)
    }

    suspend fun search(keyword: String, fid: String? = null): SearchResult = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        val fidParam = if (!fid.isNullOrEmpty() && fid != "0") "&srchfid=$fid" else ""
        val sr = ForumParser.parseSearchResult(
            http.get(http.url("search.php?mod=forum&searchsubmit=yes&srchtxt=$encoded$fidParam&mobile=2")),
        )
        applySearchBlockFilter(sr)
    }

    suspend fun searchNext(url: String): SearchResult = withContext(Dispatchers.IO) {
        val sr = ForumParser.parseSearchResult(http.get(url))
        applySearchBlockFilter(sr)
    }

    private fun applySearchBlockFilter(sr: SearchResult): SearchResult {
        val blocked = currentBlocked()
        return if (blocked.isEmpty()) sr
        else sr.copy(threads = sr.threads.filterNot { it.authorUid in blocked })
    }

    





    suspend fun me(): UserProfile = withContext(Dispatchers.IO) {
        val body = http.get(http.url("home.php?mod=space&do=profile&mobile=2"))
        val profile = ForumParser.parseUserProfile(body)
        logMeResponse(body, profile)
        profile
    }

    






    suspend fun notices(tabs: List<NoticeTab>): NoticePage = withContext(Dispatchers.IO) {
        val requests = tabs.mapNotNull { tab ->
            val view = tab.view ?: return@mapNotNull null
            view to tab.type
        }.distinct()
        val pages = coroutineScope {
            requests.map { (view, type) ->
                async {
                    val query = if (type == null) {
                        "home.php?mod=space&do=notice&view=$view&mobile=2"
                    } else {
                        "home.php?mod=space&do=notice&view=$view&type=$type&mobile=2"
                    }
                    runCatching { ForumParser.parseNotices(http.get(http.url(query))) }.getOrNull()
                }
            }.awaitAll()
        }
        val merged = mutableListOf<Notice>()
        var loggedOut = false
        for (page in pages) {
            if (page == null) continue
            if (page.loggedOut) {
                loggedOut = true
                continue
            }
            merged += page.notices
        }
        
        
        NoticePage(notices = merged.distinctBy { it.id }, loggedOut = loggedOut && merged.isEmpty())
    }

    





    




    suspend fun pollNoticeView(view: String): NoticeViewOverview = withContext(Dispatchers.IO) {
        val html = http.get(http.url("home.php?mod=space&do=notice&view=$view&mobile=2"))
        val parsed = ForumParser.parseNotices(html)
        val counts = ForumParser.parseNoticeSidebarCounts(html)
        NoticeViewOverview(
            notices = parsed.notices,
            sidebarCounts = counts,
            loggedOut = parsed.loggedOut,
        )
    }

    data class NoticeViewOverview(
        val notices: List<Notice> = emptyList(),
        val sidebarCounts: Map<String, Int> = emptyMap(),
        val loggedOut: Boolean = false,
    )

    suspend fun pmMessages(filter: String): PmPage = withContext(Dispatchers.IO) {
        val body = http.get(http.url("home.php?mod=space&do=pm&filter=$filter&inajax=1"))
        ForumParser.parsePmMessages(body)
    }

    
    suspend fun chatConversation(touid: String): ChatConversation = withContext(Dispatchers.IO) {
        val html = http.get(http.url("home.php?mod=space&do=pm&subop=view&touid=$touid&mobile=2"))
        ForumParser.parseChatConversation(html, touid)
    }

    
    suspend fun userProfile(uid: String): UserProfile = withContext(Dispatchers.IO) {
        val html = http.get(http.url("home.php?mod=space&uid=$uid&do=profile&mobile=2"))
        ForumParser.parseUserProfile(html, fallbackUid = uid)
    }

    
    suspend fun reportPost(
        tid: String,
        fid: String,
        formhash: String,
        pid: String?,
        reason: String,
        message: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val rtype = if (pid.isNullOrBlank()) "thread" else "post"
        val rid = if (rtype == "thread") tid else pid!!
        val formUrl = http.url(
            "misc.php?mod=report&rtype=$rtype&rid=$rid&tid=$tid&fid=$fid&inajax=1&mobile=2",
        )
        val page = runCatching { http.get(formUrl) }.getOrDefault("")
        val fh = ForumParser.parseFormhash(page) ?: formhash
        if (fh.isBlank()) return@withContext false
        val params = mutableMapOf(
            "formhash" to fh,
            "tid" to tid,
            "fid" to fid,
            "rtype" to rtype,
            "rid" to rid,
            "reportsubmit" to "yes",
            "reason" to reason.ifBlank { "其他" },
            "message" to message.ifBlank { "该内容涉嫌违规，请核实处理。" },
        )
        if (rtype == "post" && pid != null) params["pid"] = pid
        val url = http.url(
            "misc.php?mod=report&rtype=$rtype&rid=$rid&tid=$tid&fid=$fid&reportsubmit=yes&inajax=1&mobile=2",
        )
        val body = http.post(url, params)
        body.isNotBlank() && !REPORT_ERROR_PATTERN.containsMatchIn(body)
    }

    





    suspend fun fetchEditPostInit(tid: String, pid: String): EditPostPageData = withContext(Dispatchers.IO) {
        val html = http.get(http.url("forum.php?mod=post&action=edit&tid=$tid&pid=$pid&mobile=2"))
        ForumParser.parseEditPostInit(html)
    }

    





    suspend fun editPost(
        tid: String,
        pid: String,
        formhash: String,
        message: String,
        subject: String? = null,
    ): EditPostResult = withContext(Dispatchers.IO) {
        val url = http.url("forum.php?mod=post&action=edit&extra=&tid=$tid&pid=$pid&page=1&editsubmit=yes&mobile=2")
        
        val text = protectQuoteTagsOnEdit(message)
        val params = mutableMapOf(
            "formhash" to formhash,
            "message" to text,
            "editsubmit" to "yes",
            "tid" to tid,
            "pid" to pid,
        )
        if (!subject.isNullOrBlank()) params["subject"] = subject
        for (aid in attachAids(text)) params["attachnew[$aid][description]"] = ""
        val body = http.post(
            url,
            params,
            referer = "https://bbs.binmt.cc/forum.php?mod=post&action=edit&tid=$tid&pid=$pid&mobile=2",
        )
        ForumParser.parseEditPostResult(body, tid)
    }

    
    suspend fun deletePost(tid: String, pid: String, formhash: String): Boolean = withContext(Dispatchers.IO) {        val formUrl = http.url("forum.php?mod=post&action=edit&tid=$tid&pid=$pid&mobile=2")
        val page = http.get(formUrl)
        val fh = ForumParser.extractDeleteHash(page, pid)
            ?: ForumParser.parseFormhash(page)
            ?: formhash
        if (fh.isBlank()) return@withContext false
        val params = mapOf(
            "formhash" to fh,
            "delete" to "1",
            "pid" to pid,
            "tid" to tid,
            
            "editsubmit" to "yes",
        )
        val url = http.url("forum.php?mod=post&action=edit&extra=&tid=$tid&pid=$pid&page=1&delete=1&mobile=2")
        val body = http.post(url, params)
        
        val stillForm = DELETE_FORM_ACTION.containsMatchIn(body) && DELETE_FORM_SUBMIT.containsMatchIn(body)
        body.isNotBlank() && !stillForm
    }

    





    suspend fun stickReply(
        tid: String,
        fid: String,
        formhash: String,
        pid: String,
        stick: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        val params = mapOf(
            "formhash" to formhash,
            "fid" to fid,
            "tid" to tid,
            "page" to "1",
            "reason" to "手机版主题操作",
            "topiclist[]" to pid,
            "stickreply" to if (stick) "1" else "0",
        )
        val url = http.url(
            "forum.php?mod=topicadmin&action=stickreply&modsubmit=yes&modclick=yes&mobile=2&inajax=1",
        )
        val body = http.post(url, params, referer = "https://bbs.binmt.cc/thread-$tid-1-1.html")
        TOPICADMIN_SUCCESS.containsMatchIn(body)
    }

    



    suspend fun rewardAuthor(
        tid: String,
        pid: String?,
        formhash: String,
        amount: Int,
        message: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        val targetPid = pid?.takeIf { it.isNotBlank() } ?: run {
            val latest = ForumParser.parseThreadPage(
                http.get(http.url("forum.php?mod=viewthread&tid=$tid&mobile=2")),
                1,
            )
            latest.posts.firstOrNull { it.isFirstPost }?.pid
        } ?: return@withContext false
        val ratePage = runCatching {
            http.get(http.url("forum.php?mod=misc&action=rate&tid=$tid&pid=$targetPid&showratetip=1&inajax=1&mobile=2"))
        }.getOrDefault("")
        val fh = ForumParser.parseFormhash(ratePage) ?: formhash
        if (fh.isBlank()) return@withContext false
        val params = mutableMapOf(
            "formhash" to fh,
            "tid" to tid,
            "pid" to targetPid,
            "ratesubmit" to "yes",
            "referer" to "https://bbs.binmt.cc/thread-$tid-1-1.html",
            "score1" to "1",
            "score2" to amount.toString(),
        )
        if (!message.isNullOrBlank()) params["reason"] = message
        val url = http.url("forum.php?mod=misc&action=rate&tid=$tid&pid=$targetPid&ratesubmit=yes&inajax=1&mobile=2")
        val body = http.post(url, params)
        body.isNotBlank() && !REWARD_FAIL_PATTERN.containsMatchIn(body) &&
            REWARD_SUCCESS_PATTERN.containsMatchIn(body)
    }

    
    suspend fun likeUsers(tid: String): List<LikeUser> = withContext(Dispatchers.IO) {
        ForumParser.parseLikeUsers(
            http.get(http.url("misc.php?op=recommend&tid=$tid&mod=faq&mobile=2")),
        )
    }

    
    suspend fun newThread(
        fid: String,
        formhash: String,
        subject: String,
        message: String,
    ): String = withContext(Dispatchers.IO) {
        val url = http.url("forum.php?mod=post&action=newthread&fid=$fid&topicsubmit=yes&mobile=2")
        http.post(
            url,
            mapOf(
                "formhash" to formhash,
                "subject" to subject,
                "message" to message,
                "topicsubmit" to "yes",
            ),
            referer = "https://bbs.binmt.cc/forum-$fid-1.html",
        )
    }

    
    suspend fun submitNewThread(
        fid: String,
        formhash: String,
        subject: String,
        message: String,
        posttime: String? = null,
        typeid: String? = null,
        aids: List<String> = emptyList(),
    ): NewThreadResult = withContext(Dispatchers.IO) {
        val url = http.url("forum.php?mod=post&action=newthread&fid=$fid&extra=&topicsubmit=yes&mobile=2&handlekey=postform&inajax=1")
        val params = mutableMapOf(
            "formhash" to formhash,
            "subject" to subject,
            "message" to message,
            "topicsubmit" to "yes",
        )
        if (!posttime.isNullOrBlank()) params["posttime"] = posttime
        if (!typeid.isNullOrBlank() && typeid != "0") params["typeid"] = typeid
        for (aid in aids) {
            params["attachnew[$aid][description]"] = ""
        }
        val body = http.post(
            url,
            params,
            referer = "https://bbs.binmt.cc/forum.php?mod=post&action=newthread&fid=$fid&mobile=2",
        )
        ForumParser.parseNewThreadResult(body)
    }

    
    suspend fun login(
        username: String,
        password: String,
        questionId: Int = 0,
        answer: String = "",
    ): LoginResult = withContext(Dispatchers.IO) {
        val loginPage = http.get(http.url("member.php?mod=logging&action=login&mobile=2"))
        val loginhash = ForumParser.extractLoginHash(loginPage)
        val formhash = ForumParser.extractLoginFormhash(loginPage) ?: ""
        http.onLog?.invoke(
            LogLevel.INFO,
            "Login",
            "登录页：len=${loginPage.length} loginhash=${loginhash != null} " +
                "formhash=${formhash.isNotEmpty()} 登录表单=${loginPage.contains("loginform")} " +
                "挑战页=${WafSolver.looksLikeChallenge(loginPage)} " +
                "表单字段=[${ForumParser.loginFormFields(loginPage).joinToString(",")}]",
            null,
        )
        if (loginhash == null) return@withContext LoginResult(false, "无法获取登录令牌（可能被风控拦截）")
        val result = http.post(
            http.url("member.php?mod=logging&action=login&loginsubmit=yes&loginhash=$loginhash"),
            mapOf(
                "formhash" to formhash,
                "referer" to http.baseUrl,
                "fastloginfield" to "username",
                "cookietime" to "31104000",
                "username" to username,
                "password" to password,
                "questionid" to questionId.toString(),
                "answer" to answer,
                "submit" to "true",
            ),
            referer = http.url("member.php?mod=logging&action=login&mobile=2"),
        )
        val parsed = ForumParser.parseLoginResult(result)
        logLoginResponse(result, parsed)
        parsed
    }

    







    private fun logLoginResponse(body: String, parsed: LoginResult) {
        val cookieNames = http.cookieStore.cookies(http.host).joinToString(",") { it.name }
        val title = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
            .find(body)?.groupValues?.get(1)?.trim()
        val tip = ForumParser.extractTipText(body)
        val redirect = ForumParser.extractMessageRedirect(body)
        
        
        val hasAuthCookie = cookieNames.split(',').any { it.endsWith("_auth") && !it.endsWith("invite_auth") }
        
        val hints = LOGIN_DIAG_HINTS.filter { body.contains(it) }
        val anchor = if (body.contains("messagetext")) "messagetext" else hints.firstOrNull()
        val snippet = anchor?.let { rawSnippet(body, it) }
        http.onLog?.invoke(
            LogLevel.INFO,
            "Login",
            "登录响应：len=${body.length} title=$title 结果=success:${parsed.success} msg=${parsed.message} " +
                "提示正文=$tip 跳转=$redirect 有authCookie=$hasAuthCookie 命中提示词=$hints",
            null,
        )
        if (snippet != null) http.onLog?.invoke(LogLevel.INFO, "Login", "提示页片段：$snippet", null)
    }

    
    private fun logMeResponse(body: String, profile: UserProfile) {
        if (profile.isLoggedIn) return
        val discuzUid = Regex("discuz_uid\\s*=\\s*'(\\d+)'").find(body)?.groupValues?.get(1)
        val cookieNames = http.cookieStore.cookies(http.host).joinToString(",") { it.name }
        http.onLog?.invoke(
            LogLevel.WARN,
            "Login",
            "资料页未登录：len=${body.length} uid=${profile.uid} discuz_uid=$discuzUid " +
                "登录表单=${body.contains("loginform")} cookie=[$cookieNames]",
            null,
        )
    }

    
    suspend fun checkInStatus(): CheckInStatus = withContext(Dispatchers.IO) {
        val html = http.get(http.url("k_misign-sign.html"))
        ForumParser.parseCheckInStatus(html)
    }

    
    suspend fun submitCheckIn(formhash: String): CheckInResult = withContext(Dispatchers.IO) {
        val body = http.get(
            url = http.url("plugin.php?id=k_misign:sign&operation=qiandao&format=text&formhash=$formhash"),
        )
        ForumParser.parseCheckInResponse(body)
    }

    
    suspend fun myThreads(type: String = "thread", page: Int = 1): MyThreadPage = withContext(Dispatchers.IO) {
        val html = http.get(http.url("home.php?mod=space&do=thread&view=me&type=$type&page=$page&mobile=2"))
        ForumParser.parseMyThreads(html, page)
    }

    
    suspend fun userThreads(uid: String, type: String = "thread", page: Int = 1): MyThreadPage = withContext(Dispatchers.IO) {
        val html = http.get(http.url("home.php?mod=space&uid=$uid&do=thread&view=me&type=$type&page=$page&mobile=2"))
        ForumParser.parseMyThreads(html, page)
    }

    
    suspend fun myFavorites(type: String = "all", page: Int = 1): FavoritePage = withContext(Dispatchers.IO) {
        val html = http.get(http.url("home.php?mod=space&do=favorite&view=me&type=$type&page=$page&mobile=2"))
        ForumParser.parseFavorites(html, page)
    }

    
    suspend fun myFriends(view: String = "", page: Int = 1): FriendUserPage = withContext(Dispatchers.IO) {
        val path = if (view.isBlank()) {
            "home.php?mod=space&do=friend&page=$page&mobile=2"
        } else {
            "home.php?mod=space&do=friend&view=$view&page=$page&mobile=2"
        }
        val html = http.get(http.url(path))
        ForumParser.parseFriends(html, page)
    }

    
    suspend fun followList(type: String, page: Int = 1): FollowUserPage = withContext(Dispatchers.IO) {
        val path = if (type == "following") {
            "home.php?mod=follow&do=following&page=$page&mobile=2"
        } else {
            "home.php?mod=follow&do=follower&page=$page&mobile=2"
        }
        val html = http.get(http.url(path))
        ForumParser.parseFollowUsers(html, page, defaultIsFollowing = (type == "following"))
    }

    
    suspend fun toggleFollowUser(uid: String, follow: Boolean): Boolean = withContext(Dispatchers.IO) {
        val formhash = fetchFormhash(FORMHASH_PAGE)
        if (formhash.isBlank()) return@withContext false
        val url = http.url(
            "home.php?mod=spacecp&ac=follow&op=${if (follow) "add" else "del"}" +
                "&fuid=$uid&hash=$formhash&handlekey=followmod&inajax=1&mobile=2",
        )
        isWriteSucceeded(http.get(url))
    }

    
    suspend fun followUser(uid: String, formhash: String?, follow: Boolean): String = withContext(Dispatchers.IO) {
        
        val fh = formhash?.takeIf { it.isNotBlank() } ?: fetchFormhash(FORMHASH_PAGE)
        if (fh.isBlank()) return@withContext "获取操作凭据失败，请重试"
        val url = http.url(
            "home.php?mod=spacecp&ac=follow&op=${if (follow) "add" else "del"}" +
                "&fuid=$uid&hash=$fh&handlekey=followmod&inajax=1&mobile=2",
        )
        readWriteResult(http.get(url), if (follow) "已关注" else "已取消关注")
            .fold({ it }, { it.message ?: "操作失败，请重试" })
    }

    






    suspend fun setFollowRemark(uid: String, formhash: String, remark: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (uid.isBlank()) return@withContext Result.failure(IllegalStateException("用户不存在"))
            val fh = formhash.takeIf { it.isNotBlank() } ?: fetchFormhash(FORMHASH_PAGE)
            if (fh.isBlank()) {
                return@withContext Result.failure(IllegalStateException("获取操作凭据失败，请重试"))
            }
            val url = http.url(
                "home.php?mod=spacecp&ac=follow&op=bkname&fuid=$uid" +
                    "&editsubmit_btn=true&handlekey=followbkame_$uid&inajax=1&mobile=2",
            )
            val params = mapOf(
                "formhash" to fh,
                "editbkname" to "true",
                "bkname" to remark,
                "handlekey" to "followbkame_$uid",
                "editsubmit_btn" to "true",
            )
            val referer = http.url("home.php?mod=follow&do=following&mobile=2")
            val res = http.post(url, params, referer = referer)
            readWriteResult(res, "备注成功")
        }

    
    private suspend fun fetchFormhash(vararg paths: String): String {
        for (path in paths) {
            ForumParser.parseFormhashAny(http.get(http.url(path)))?.let { return it }
        }
        return ""
    }

    





    private fun readWriteResult(body: String, successText: String): Result<String> {
        if (body.isBlank()) return Result.failure(IllegalStateException("站点没有返回结果"))
        val payload = Regex("(?s)<!\\[CDATA\\[(.*?)\\]\\]>").find(body)?.groupValues?.get(1) ?: body
        val ok = Regex("succeedhandle_|do_success|操作成功|发送成功|成功收听|已成为好友").containsMatchIn(payload)
        if (ok) return Result.success(successText)
        val message = ForumParser.parseAjaxMessage(body)
        val failed = Regex("errorhandle_|submit_invalid|formhash|请先登录|Access Denied|非法操作|没有权限")
            .containsMatchIn(payload)
        if (failed || message != null) {
            return Result.failure(IllegalStateException(message ?: "操作失败"))
        }
        return Result.failure(IllegalStateException("操作失败，请重试"))
    }

    private fun isWriteSucceeded(body: String): Boolean = readWriteResult(body, "").isSuccess

    
    suspend fun addFriend(uid: String, formhash: String, note: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val referer = http.url("home.php?mod=space&uid=$uid&do=profile&mobile=2")
        
        val fh = formhash.takeIf { it.isNotBlank() }
            ?: fetchFormhash("home.php?mod=spacecp&ac=friend&op=add&uid=$uid&mobile=2")
        if (fh.isBlank()) return@withContext Result.failure(IllegalStateException("获取操作凭据失败，请重试"))
        val url = http.url("home.php?mod=spacecp&ac=friend&op=add&uid=$uid&inajax=1&mobile=2")
        val params = mapOf(
            "referer" to referer,
            "addsubmit" to "true",
            "addsubmit_btn" to "true",
            "formhash" to fh,
            "note" to note,
            "gid" to "1",
        )
        val res = http.post(url, params, referer = referer)
        val msg = ForumParser.parseAjaxMessage(res) ?: if (res.isNotBlank()) "请求已发送" else "发送失败"
        Result.success(msg)
    }

    
    suspend fun sendPrivateMessage(touid: String, formhash: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        if (message.isBlank()) return@withContext Result.failure(IllegalArgumentException("消息内容不能为空"))
        
        val fh = formhash.takeIf { it.isNotBlank() }
            ?: chatConversation(touid).formhash.orEmpty()
        if (fh.isBlank()) return@withContext Result.failure(IllegalStateException("获取操作凭据失败，请重试"))
        val referer = http.url("home.php?mod=space&do=pm&mobile=2")
        val url = http.url("home.php?mod=spacecp&ac=pm&op=send&pmid=0&daterange=2&pmsubmit=yes&inajax=1&mobile=2")
        val params = mapOf(
            "formhash" to fh,
            "touid" to touid,
            "message" to message,
            "pmsubmit" to "yes",
        )
        val res = http.post(url, params, referer = referer)
        
        
        
        readWriteResult(res, "发送成功")
    }

    
    suspend fun addUserBlacklist(username: String, formhash: String): Result<String> = withContext(Dispatchers.IO) {
        
        
        val fh = formhash.takeIf { it.isNotBlank() }
            ?: fetchFormhash("home.php?mod=space&do=friend&view=blacklist&mobile=2")
        if (fh.isBlank()) return@withContext Result.failure(IllegalStateException("获取操作凭据失败，请重试"))
        val referer = http.url("home.php?mod=space&do=friend&view=blacklist&mobile=2")
        val url = http.url(
            "home.php?mod=spacecp&ac=friend&op=blacklist&start=&formhash=$fh&inajax=1&mobile=2",
        )
        val params = mapOf(
            "blacklistsubmit" to "true",
            "blacklistsubmit_btn" to "true",
            "formhash" to fh,
            "username" to username,
        )
        val res = http.post(url, params, referer = referer)
        readWriteResult(res, "已加入黑名单")
    }

    




    suspend fun removeUserBlacklist(uid: String): Result<String> = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext Result.failure(IllegalStateException("用户不存在"))
        val fh = fetchFormhash(FORMHASH_PAGE)
        if (fh.isBlank()) {
            return@withContext Result.failure(IllegalStateException("尚未登录或会话已过期"))
        }
        val url = http.url(
            "home.php?mod=spacecp&ac=friend&op=blacklist&subop=delete&uid=$uid" +
                    "&formhash=$fh&inajax=1&mobile=2",
        )
        val res = http.get(url)
        readWriteResult(res, "已移出黑名单")
    }
}





private const val FORMHASH_PAGE = "home.php?mod=space&do=friend&view=blacklist&mobile=2"


private const val AT_USER_PAGE_SIZE = 24


private const val MAX_AT_USER_PAGES = 5
