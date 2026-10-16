package com.aazhou.mtforum

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.webkit.CookieManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.download.ThreadArchiveStore
import com.aazhou.mtforum.data.log.AppLogger
import com.aazhou.mtforum.data.log.LogStore
import com.aazhou.mtforum.data.model.BACKGROUND_TABS
import com.aazhou.mtforum.data.model.UserProfile
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.data.repo.ActionStore
import com.aazhou.mtforum.data.repo.DownloadStore
import com.aazhou.mtforum.data.repo.DraftStore
import com.aazhou.mtforum.data.repo.BlockedUsersStore
import com.aazhou.mtforum.data.repo.RelationStore
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.HistoryStore
import com.aazhou.mtforum.data.repo.LogSettingsStore
import com.aazhou.mtforum.data.repo.NoticeChecker
import com.aazhou.mtforum.data.repo.NoticePollStore
import com.aazhou.mtforum.data.repo.NoticeUnreadStore
import com.aazhou.mtforum.data.repo.CheckInManager
import com.aazhou.mtforum.data.repo.SessionStore
import com.aazhou.mtforum.data.repo.SmileyStore
import com.aazhou.mtforum.data.repo.ThemeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class AppContainer(application: Application) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext: Context = application

    
    private val noticeRefreshLock = Any()
    private var lastNoticeRefreshAt = 0L

    
    private var foregroundPollJob: Job? = null

    val sessionStore = SessionStore(application)
    
    val historyStore = HistoryStore(application)
    val themeStore = ThemeStore(application)
    val actionStore = ActionStore(application)
    
    val downloadStore = DownloadStore(application)
    
    val archiveStore = ThreadArchiveStore(application)
    val httpClient = HttpClient()
    
    val smileyStore = SmileyStore(application)
    val repository = ForumRepository(httpClient, smileyStore)
    
    val relationStore = RelationStore(application, repository)
    val blockedUsersStore = BlockedUsersStore(relationStore)

    init {
        
        repository.blockedUidsProvider = { blockedUsersStore.blockedUids.value }
        
        appScope.launch { relationStore.refresh(force = true) }
    }
    val noticeUnreadStore = NoticeUnreadStore(application)
    
    val noticeChecker = NoticeChecker(repository, noticeUnreadStore)
    
    val noticePollStore = NoticePollStore(application)

    
    val unreadNotices: StateFlow<Boolean> =
        noticeUnreadStore.hasUnread.stateIn(appScope, SharingStarted.Eagerly, false)

    






    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    val checkInManager = CheckInManager(repository, sessionStore, httpClient, currentUser, appScope)

    
    val contentCache = ContentCache(java.io.File(application.cacheDir, "mtf_content"))

    
    val draftStore = DraftStore(java.io.File(application.cacheDir, "mtf_drafts"))

    
    val logStore = LogStore(java.io.File(application.filesDir, "logs/mtf_log.jsonl"))
    val logSettings = LogSettingsStore(application)

    
    val logger = AppLogger(logStore, appScope)

    init {
        httpClient.onSessionChanged = { refreshCurrentUserAsync() }
        httpClient.onLog = { level, tag, message, throwable ->
            logger.log(level, tag, message, throwable)
        }
        
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    refreshNoticesIfDue()
                    startForegroundPolling()
                    
                    relationStore.refresh(force = false)
                }

                override fun onStop(owner: LifecycleOwner) {
                    foregroundPollJob?.cancel()
                    foregroundPollJob = null
                }
            },
        )
        
        appScope.launch { logSettings.level.collect { logger.setLevel(it) } }
        appScope.launch {
            
            if (logStore.consumeCrashMarker()) {
                logger.info(AppLogger.TAG_CRASH, "检测到上次运行异常退出，上一条 ERROR 即当时的崩溃日志")
            }
            val saved = sessionStore.cookieHeader.first()
            if (saved.isNotEmpty()) {
                restoreCookies(saved)
                logger.info("Session", "已从磁盘恢复登录会话")
            }
            
            val savedUid = sessionStore.uid.first()
            if (savedUid.isNotEmpty()) {
                _currentUser.value = UserProfile(
                    uid = savedUid,
                    name = sessionStore.username.first().ifEmpty { null },
                    avatarUrl = sessionStore.avatar.first().ifEmpty { null },
                )
            }
            if (httpClient.isLoggedIn()) {
                
                
                refreshNoticesIfDue()
                refreshCurrentUser()
            }
            
            appScope.launch {
                if (noticePollStore.enabled.first()) NoticePollWorker.enqueue(appContext)
            }
        }
    }

    





    private fun startForegroundPolling() {
        if (foregroundPollJob?.isActive == true) return
        foregroundPollJob = appScope.launch {
            while (isActive) {
                delay(FOREGROUND_REFRESH_INTERVAL_MS)
                refreshNoticesIfDue()
            }
        }
    }

    





    private fun refreshNoticesIfDue() {
        if (!httpClient.isLoggedIn()) return
        val now = SystemClock.elapsedRealtime()
        synchronized(noticeRefreshLock) {
            if (lastNoticeRefreshAt != 0L &&
                now - lastNoticeRefreshAt < NOTICE_REFRESH_MIN_INTERVAL_MS
            ) {
                return
            }
            lastNoticeRefreshAt = now
        }
        appScope.launch {
            
            runCatching { noticeChecker.check(BACKGROUND_TABS) }.onFailure { e ->
                logger.warn("Notice", "查询消息失败", e)
            }
        }
    }

    
    fun markNoticesRead(ids: List<String>) {
        appScope.launch { noticeUnreadStore.markRead(ids) }
        
        NoticeNotifier.cancel(appContext)
    }

    






    fun isLoggedIn(): Boolean = currentUser.value?.isLoggedIn == true

    
    suspend fun refreshCurrentUser() {
        if (!httpClient.isLoggedIn()) {
            _currentUser.value = null
            return
        }
        val profile = runCatching { repository.me() }.getOrElse { e ->
            logger.warn("Session", "拉取当前用户资料失败", e)
            
            
            cachedProfile()?.let { _currentUser.value = it }
            return
        }
        if (!profile.isLoggedIn) {
            cachedProfile()?.let { _currentUser.value = it } ?: run { _currentUser.value = null }
            return
        }
        _currentUser.value = profile
        
        persistSession(profile.uid.orEmpty(), profile.name.orEmpty(), profile.avatarUrl)
    }

    
    private suspend fun cachedProfile(): UserProfile? {
        val uid = sessionStore.uid.first()
        if (uid.isEmpty()) return null
        return UserProfile(
            uid = uid,
            name = sessionStore.username.first().ifEmpty { null },
            avatarUrl = sessionStore.avatar.first().ifEmpty { null },
        )
    }

    
    fun refreshCurrentUserAsync() {
        appScope.launch { refreshCurrentUser() }
    }

    
    private fun restoreCookies(header: String) {
        for (pair in header.split(";")) {
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val name = pair.substring(0, eq).trim()
            val value = pair.substring(eq + 1).trim()
            if (name.isEmpty() || value.isEmpty()) continue
            httpClient.cookieStore.setCookie(httpClient.host, name, value)
        }
    }

    
    fun persistSession(uid: String, username: String, avatar: String?) {
        appScope.launch {
            sessionStore.saveSession(httpClient.cookieHeader(), uid, username, avatar)
        }
    }

    fun logout() {
        httpClient.cookieStore.clear()
        
        CookieManager.getInstance().removeAllCookies(null)
        _currentUser.value = null
        logger.info("Session", "已退出登录")
        NoticePollWorker.cancel(appContext)
        appScope.launch { sessionStore.clearSession() }
    }
}


private const val NOTICE_REFRESH_MIN_INTERVAL_MS = 60_000L


private const val FOREGROUND_REFRESH_INTERVAL_MS = 90_000L
