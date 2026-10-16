package com.aazhou.mtforum.ui.screen.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.UserProfile
import com.aazhou.mtforum.data.repo.ActionStore
import com.aazhou.mtforum.data.repo.BlockedUsersStore
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.SessionStore
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class UserProfileViewModel(
    val uid: String,
    private val repository: ForumRepository,
    private val sessionStore: SessionStore,
    private val actionStore: ActionStore,
    private val contentCache: ContentCache,
    private val blockedUsersStore: BlockedUsersStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val state: StateFlow<UiState<UserProfile>> = _state.asStateFlow()

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

    
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    private val cacheJson = Json { ignoreUnknownKeys = true }

    
    val myUid: StateFlow<String> = sessionStore.uid
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    
    val isSelf: StateFlow<Boolean> = combine(sessionStore.uid, MutableStateFlow(uid)) { myUid, targetUid ->
        myUid.isNotBlank() && myUid == targetUid
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    
    
    val isBlocked: StateFlow<Boolean> = blockedUsersStore.blockedUids
        .map { it.contains(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    
    private val _followingOverride = MutableStateFlow<Boolean?>(null)
    val isFollowing: StateFlow<Boolean> = combine(_state, _followingOverride) { profileState, override ->
        if (override != null) return@combine override
        val profile = (profileState as? UiState.Success)?.data
        profile?.isFollowing ?: false
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        load()
    }

    





    fun load() {
        viewModelScope.launch {
            
            val cached = readCache()
            if (cached != null && _state.value !is UiState.Success) {
                _state.value = UiState.Success(cached)
            }
            if (_state.value !is UiState.Success) {
                _state.value = UiState.Loading
            }
            try {
                val profile = repository.userProfile(uid)
                applyFollowState(profile)
                _state.value = UiState.Success(profile)
                saveToCache(profile)
            } catch (e: Exception) {
                
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error(e.message ?: "加载失败")
                } else {
                    _actionMessage.value = "新数据获取失败"
                }
            }
        }
    }

    
    private suspend fun applyFollowState(profile: UserProfile) {
        val localFollow = actionStore.following(uid)
        if (localFollow != null) {
            _followingOverride.value = localFollow
        } else if (profile.isFollowing != null) {
            _followingOverride.value = profile.isFollowing
        }
    }

    private suspend fun readCache(): UserProfile? =
        contentCache.read(
            CacheKeys.userProfile(uid),
            CacheKeys.LIST_TTL,
            allowExpired = true,
        ) {
            cacheJson.decodeFromString<CachedProfile>(it)
        }?.toUserProfile()

    
    private suspend fun saveToCache(profile: UserProfile) {
        if (!profile.isLoggedIn) return
        contentCache.write(
            key = CacheKeys.userProfile(uid),
            ttlMillis = CacheKeys.LIST_TTL,
            value = CachedProfile.from(profile),
            encode = { cacheJson.encodeToString(CachedProfile.serializer(), it) },
        )
    }

    
    fun toggleFollow() {
        val currentProfile = (_state.value as? UiState.Success)?.data ?: return
        if (_isOperating.value) return
        val currentFollowing = isFollowing.value
        val target = !currentFollowing
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val msg = repository.followUser(uid, currentProfile.formhash, target)
                _followingOverride.value = target
                actionStore.setFollowing(uid, target)
                AppToast.show(msg)
            } catch (e: Exception) {
                AppToast.show(e.message ?: "操作失败")
            } finally {
                _isOperating.value = false
            }
        }
    }

    
    fun addFriend(note: String, onDone: () -> Unit) {
        val currentProfile = (_state.value as? UiState.Success)?.data ?: return
        val formhash = currentProfile.formhash ?: ""
        if (_isOperating.value) return
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val res = repository.addFriend(uid, formhash, note)
                res.onSuccess { msg ->
                    AppToast.show(msg)
                    onDone()
                }.onFailure { e ->
                    AppToast.show(e.message ?: "加好友失败")
                }
            } finally {
                _isOperating.value = false
            }
        }
    }

    
    fun sendPm(message: String, onDone: () -> Unit) {
        val currentProfile = (_state.value as? UiState.Success)?.data ?: return
        val formhash = currentProfile.formhash ?: ""
        if (message.isBlank()) {
            AppToast.show("请输入聊天内容")
            return
        }
        if (_isOperating.value) return
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val res = repository.sendPrivateMessage(uid, formhash, message)
                res.onSuccess { msg ->
                    AppToast.show(msg)
                    onDone()
                }.onFailure { e ->
                    AppToast.show(e.message ?: "发送失败")
                }
            } finally {
                _isOperating.value = false
            }
        }
    }

    
    fun blockUser(onDone: () -> Unit) {
        val currentProfile = (_state.value as? UiState.Success)?.data ?: return
        val username = currentProfile.name.orEmpty()
        val formhash = currentProfile.formhash ?: ""
        if (username.isBlank()) {
            AppToast.show("未能获取用户名，无法拉黑")
            return
        }
        if (_isOperating.value) return
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val res = repository.addUserBlacklist(username, formhash)
                res.onSuccess { msg ->
                    blockedUsersStore.add(uid)
                    AppToast.show(msg)
                    onDone()
                }.onFailure { e ->
                    AppToast.show(e.message ?: "拉黑失败")
                }
            } finally {
                _isOperating.value = false
            }
        }
    }

    
    fun unblockUser() {
        if (_isOperating.value) return
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val res = repository.removeUserBlacklist(uid)
                res.onSuccess { msg ->
                    blockedUsersStore.remove(uid)
                    AppToast.show(msg)
                }.onFailure { e ->
                    AppToast.show(e.message ?: "移出黑名单失败")
                }
            } finally {
                _isOperating.value = false
            }
        }
    }

    
}


@Serializable
private data class CachedProfile(
    val uid: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val level: String? = null,
    val groupName: String? = null,
    val threads: Int? = null,
    val posts: Int? = null,
    val friends: Int? = null,
    val following: Int? = null,
    val followers: Int? = null,
    val credits: Int? = null,
    val gold: Int? = null,
    val onlineTime: String? = null,
) {
    fun toUserProfile(): UserProfile = UserProfile(
        uid = uid,
        name = name,
        avatarUrl = avatarUrl,
        level = level,
        groupName = groupName,
        threads = threads,
        posts = posts,
        friends = friends,
        following = following,
        followers = followers,
        credits = credits,
        gold = gold,
        onlineTime = onlineTime,
    )

    companion object {
        fun from(profile: UserProfile): CachedProfile = CachedProfile(
            uid = profile.uid,
            name = profile.name,
            avatarUrl = profile.avatarUrl,
            level = profile.level,
            groupName = profile.groupName,
            threads = profile.threads,
            posts = profile.posts,
            friends = profile.friends,
            following = profile.following,
            followers = profile.followers,
            credits = profile.credits,
            gold = profile.gold,
            onlineTime = profile.onlineTime,
        )
    }
}
