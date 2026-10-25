package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.UserProfile
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.ui.common.AppToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone







class CheckInManager(
    private val repository: ForumRepository,
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient,
    private val currentUser: StateFlow<UserProfile?>,
    private val scope: CoroutineScope,
) {
    
    private val checkInRepo = CheckInRepo(httpClient)

    private val mutex = Mutex()

    private val _hasSigned = MutableStateFlow(false)
    val hasSigned: StateFlow<Boolean> = _hasSigned.asStateFlow()

    private val _isCheckingIn = MutableStateFlow(false)
    val isCheckingIn: StateFlow<Boolean> = _isCheckingIn.asStateFlow()

    init {
        scope.launch {
            currentUser.collect { user ->
                val uid = user?.uid
                if (uid.isNullOrEmpty() || uid == "0") {
                    _hasSigned.value = false
                } else {
                    val today = todayDateString()
                    val savedDate = sessionStore.checkInDate(uid).first()
                    if (savedDate == today) {
                        _hasSigned.value = true
                    } else {
                        checkStatusSilently(uid)
                    }
                }
            }
        }
    }

    
    fun checkStatusSilently(targetUid: String? = currentUser.value?.uid) {
        val uid = targetUid ?: return
        if (uid.isEmpty() || uid == "0" || !httpClient.isLoggedIn()) return
        val today = todayDateString()

        scope.launch {
            val savedDate = sessionStore.checkInDate(uid).first()
            if (savedDate == today) {
                _hasSigned.value = true
                return@launch
            }
            if (!mutex.tryLock()) return@launch
            try {
                val status = checkInRepo.status()
                if (status.hasSigned) {
                    _hasSigned.value = true
                    sessionStore.saveCheckInDate(uid, today)
                } else {
                    
                    _hasSigned.value = false
                }
            } catch (_: Exception) {
                
            } finally {
                mutex.unlock()
            }
        }
    }

    
    fun performCheckIn() {
        if (!httpClient.isLoggedIn()) {
            AppToast.show("请先登录")
            return
        }
        val uid = currentUser.value?.uid
        if (uid.isNullOrEmpty() || uid == "0") {
            AppToast.show("请先登录")
            return
        }
        if (_hasSigned.value) {
            AppToast.show("今日已签到")
            return
        }
        if (_isCheckingIn.value) {
            return
        }
        if (!mutex.tryLock()) {
            return
        }

        scope.launch {
            _isCheckingIn.value = true
            try {
                val today = todayDateString()
                
                val status = checkInRepo.status()
                if (status.hasSigned) {
                    _hasSigned.value = true
                    sessionStore.saveCheckInDate(uid, today)
                    AppToast.show("今日已签到")
                    return@launch
                }
                val formhash = status.formhash
                if (formhash.isNullOrBlank()) {
                    AppToast.show("获取签到令牌失败，请稍后重试")
                    return@launch
                }

                
                delay(500)

                
                val result = checkInRepo.sign(formhash)
                if (result.success) {
                    _hasSigned.value = true
                    sessionStore.saveCheckInDate(uid, today)
                    AppToast.show(result.message)
                } else {
                    AppToast.show(result.message)
                }
            } catch (e: Exception) {
                AppToast.show("签到失败，请稍后重试")
            } finally {
                _isCheckingIn.value = false
                mutex.unlock()
            }
        }
    }

    companion object {
        fun todayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            sdf.timeZone = TimeZone.getTimeZone("GMT+8")
            return sdf.format(Date())
        }
    }
}
