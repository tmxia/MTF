package com.aazhou.mtforum.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private val LOCK_HINT = Regex("次数过多|分钟后|锁定|禁止登录")





val SECURITY_QUESTIONS: List<Pair<Int, String>> = listOf(
    0 to "安全提问(未设置请忽略)",
    1 to "母亲的名字",
    2 to "爷爷的名字",
    3 to "父亲出生的城市",
    4 to "您其中一位老师的名字",
    5 to "您个人计算机的型号",
    6 to "您最喜欢的餐馆名称",
    7 to "驾驶执照最后四位数字",
)

class LoginViewModel(
    private val repository: ForumRepository,
    private val sessionStore: SessionStore,
    
    private val onSessionSaved: () -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(username: String, password: String, questionId: Int = 0, answer: String = "") {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("请输入用户名和密码")
            return
        }
        if (questionId > 0 && answer.isBlank()) {
            _state.value = LoginUiState.Error("请输入安全提问的答案")
            return
        }
        _state.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.login(username.trim(), password, questionId, answer.trim())
                if (!result.success) {
                    _state.value = LoginUiState.Error(loginErrorMessage(result.message))
                    return@launch
                }
                val profile = repository.me()
                if (!profile.isLoggedIn) {
                    _state.value = LoginUiState.Error("登录未生效，请检查账号密码后重试")
                    return@launch
                }
                sessionStore.saveSession(
                    cookieHeader = repository.sessionCookieHeader(),
                    uid = profile.uid.orEmpty(),
                    username = profile.name.orEmpty(),
                    avatar = profile.avatarUrl,
                )
                onSessionSaved()
                _state.value = LoginUiState.Success
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    





    private fun loginErrorMessage(siteMessage: String?): String {
        val raw = siteMessage?.takeIf { it.isNotBlank() } ?: return "登录失败，请重试"
        return if (LOCK_HINT.containsMatchIn(raw)) {
            "$raw（该限制由站点按出口 IP 记录，与账号无关，请稍后再试）"
        } else {
            raw
        }
    }

    fun reset() {
        _state.value = LoginUiState.Idle
    }

    sealed interface LoginUiState {
        data object Idle : LoginUiState
        data object Loading : LoginUiState
        data object Success : LoginUiState
        data class Error(val message: String) : LoginUiState
    }
}
