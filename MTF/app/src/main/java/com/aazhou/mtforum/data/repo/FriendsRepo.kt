package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.FriendRequestItem
import com.aazhou.mtforum.data.model.FriendRequestPage
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.parser.ForumParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext









class FriendsRepo(private val http: HttpClient) {

    private val favoritePageUrl: String
        get() = http.url("home.php?mod=space&do=favorite&view=me&type=all&mobile=2")

    private val friendPageUrl: String
        get() = http.url("home.php?mod=space&do=friend&mobile=2")

    
    private val blacklistPageUrl: String
        get() = http.url("home.php?mod=space&do=friend&view=blacklist&mobile=2")

    private fun requestPageUrl(page: Int) =
        http.url("home.php?mod=spacecp&ac=friend&op=request&page=$page&mobile=2")

    
    suspend fun friendRequests(page: Int = 1): FriendRequestPage = withContext(Dispatchers.IO) {
        ForumParser.parseFriendRequests(http.get(requestPageUrl(page)), page)
    }


    
    suspend fun approveFriendRequest(uid: String, formhash: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (uid.isBlank()) return@withContext Result.failure(IllegalStateException("用户不存在"))
            val fh = formhash.ifBlank {
                requestFormhash() ?: friendFormhash() ?: blacklistFormhash() ?: ""
            }
            if (fh.isBlank()) {
                return@withContext Result.failure(IllegalStateException("尚未登录或会话已过期"))
            }
            val url = http.url(
                "home.php?mod=spacecp&ac=friend&op=add&uid=$uid" +
                    "&formhash=$fh&add2submit=true&gid=1&handlekey=friendhk&inajax=1&mobile=2",
            )
            val body = http.post(
                url,
                mapOf("formhash" to fh, "add2submit" to "true", "gid" to "1"),
                referer = requestPageUrl(1),
            )
            readWriteResult(body, "已同意")
        }

    
    suspend fun ignoreFriendRequest(uid: String, formhash: String): Result<String> =
        removeFriend(uid, formhash, "已忽略")

    



    suspend fun removeFriend(uid: String, formhash: String, successText: String = "已删除好友"): Result<String> =
        withContext(Dispatchers.IO) {
            if (uid.isBlank()) return@withContext Result.failure(IllegalStateException("用户不存在"))
            val fh = formhash.ifBlank {
                friendFormhash() ?: blacklistFormhash() ?: requestFormhash() ?: ""
            }
            if (fh.isBlank()) {
                return@withContext Result.failure(IllegalStateException("尚未登录或会话已过期"))
            }
            val url = http.url(
                "home.php?mod=spacecp&ac=friend&op=delete&uid=$uid" +
                    "&formhash=$fh&friendsubmit=true&handlekey=friendhk&inajax=1&mobile=2",
            )
            val body = http.post(
                url,
                mapOf("formhash" to fh, "friendsubmit" to "true"),
                referer = friendPageUrl,
            )
            readWriteResult(body, successText)
        }

    
    suspend fun blacklistFormhash(): String? = withContext(Dispatchers.IO) {
        ForumParser.parseFormhashAny(http.get(blacklistPageUrl))
    }

    
    suspend fun requestFormhash(): String? = withContext(Dispatchers.IO) {
        ForumParser.parseFormhashAny(http.get(requestPageUrl(1)))
    }

    
    suspend fun friendFormhash(): String? = withContext(Dispatchers.IO) {
        ForumParser.parseFormhashAny(http.get(friendPageUrl))
    }

    private val RE_WRITE_OK = Regex(
        "succeedhandle_|do_success|操作成功|删除成功|添加成功|已成为好友|已忽略|忽略成功",
    )

    private val RE_WRITE_FAIL = Regex(
        "errorhandle_|submit_invalid|formhash错误|请先登录|非法操作|没有权限|不存在|失败|错误",
    )

    
    private fun readWriteResult(body: String, successText: String): Result<String> {
        if (body.isBlank()) return Result.failure(IllegalStateException("站点没有返回结果"))
        val payload = Regex("(?s)<!\\[CDATA\\[(.*?)\\]\\]>").find(body)?.groupValues?.get(1) ?: body
        if (RE_WRITE_FAIL.containsMatchIn(payload)) {
            return Result.failure(IllegalStateException(ForumParser.parseAjaxMessage(body) ?: "操作失败"))
        }
        if (RE_WRITE_OK.containsMatchIn(payload)) return Result.success(successText)
        return Result.failure(IllegalStateException(ForumParser.parseAjaxMessage(body) ?: "操作失败"))
    }
}
