package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.CheckInResult
import com.aazhou.mtforum.data.model.CheckInStatus
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.parser.ForumParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext








class CheckInRepo(private val httpClient: HttpClient) {

    
    suspend fun status(): CheckInStatus = withContext(Dispatchers.IO) {
        val html = httpClient.get(httpClient.url(SIGN_PAGE))
        CheckInStatus(
            hasSigned = ForumParser.parseCheckInStatus(html).hasSigned,
            formhash = ForumParser.parseFormhashAny(html),
        )
    }

    
    suspend fun sign(formhash: String): CheckInResult = withContext(Dispatchers.IO) {
        val body = httpClient.get(
            url = httpClient.url(
                "plugin.php?id=k_misign:sign&operation=qiandao&format=text&formhash=$formhash",
            ),
        )
        ForumParser.parseCheckInResponse(body)
    }

    companion object {
        private const val SIGN_PAGE = "k_misign-sign.html"
    }
}
