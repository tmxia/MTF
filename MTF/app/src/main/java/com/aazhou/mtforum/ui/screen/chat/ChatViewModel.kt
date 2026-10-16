package com.aazhou.mtforum.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.cache.CacheKeys
import com.aazhou.mtforum.data.cache.ContentCache
import com.aazhou.mtforum.data.model.ChatConversation
import com.aazhou.mtforum.data.model.ChatMessage
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.SessionStore
import com.aazhou.mtforum.ui.common.AppToast
import com.aazhou.mtforum.ui.common.UiState
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


private val RE_IMG_TAG = Regex("""\[img\](\S+?)\[/img\]""", RegexOption.IGNORE_CASE)


private val RE_CONTENT_IMG = Regex("""<img[^>]*?src=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)


private val RE_SMILEY_CODE = Regex("""\[#[^\]]+\]""")


private val RE_BBCODE_TAG = Regex("""\[/?[a-zA-Z*][^\]]*\]""")










internal fun chatMessageIdentity(message: ChatMessage): String {
    val urls = LinkedHashSet<String>()
    RE_IMG_TAG.findAll(message.text).forEach { urls += it.groupValues[1].replace("&amp;", "&") }
    val html = message.contentHtml
    if (!html.isNullOrBlank()) {
        RE_CONTENT_IMG.findAll(html).forEach { match ->
            val src = match.groupValues[1].replace("&amp;", "&").trim()
            
            if (src.isNotEmpty() && !src.contains("smiley", true) && !src.contains("static/image", true)) {
                urls += src
            }
        }
    }
    val text = message.text
        .replace(RE_IMG_TAG, "")
        .replace(RE_SMILEY_CODE, "")
        .replace(RE_BBCODE_TAG, "")
        .replace(RE_CONTENT_IMG, "")
        .replace("&nbsp;", "")
        .replace("\u00A0", "")
        .filterNot { it.isWhitespace() }
    return urls.joinToString("|") + "\u0000" + text
}













internal fun mergeChatMessages(remote: ChatConversation, local: List<ChatMessage>): ChatConversation {
    val localSelf = local.filter { it.isSelf }
    if (localSelf.isEmpty()) return remote
    val siteSelf = remote.messages.filter { it.isSelf }
    val siteKeys = siteSelf.map(::chatMessageIdentity).toMutableList()

    var lastMatched = -1
    localSelf.forEachIndexed { index, message ->
        val at = siteKeys.indexOf(chatMessageIdentity(message))
        if (at >= 0) {
            siteKeys.removeAt(at)
            lastMatched = index
        }
    }
    val tail = when {
        
        
        siteSelf.isEmpty() -> localSelf
        lastMatched >= 0 -> localSelf.subList(lastMatched + 1, localSelf.size)
        
        else -> localSelf.filter { it.sending }
    }
    if (tail.isEmpty()) return remote
    return remote.copy(messages = remote.messages + tail.map { it.copy(sending = false) })
}

class ChatViewModel(
    val touid: String,
    initialTargetName: String?,
    private val repository: ForumRepository,
    private val sessionStore: SessionStore,
    private val contentCache: ContentCache,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ChatConversation>>(UiState.Loading)
    val state: StateFlow<UiState<ChatConversation>> = _state.asStateFlow()

    private val cacheJson = Json { ignoreUnknownKeys = true }

    private val _targetName = MutableStateFlow(initialTargetName?.ifBlank { null } ?: "聊天")
    val targetName: StateFlow<String> = _targetName.asStateFlow()


    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    
    private val _failedSend = MutableStateFlow<String?>(null)
    val failedSend: StateFlow<String?> = _failedSend.asStateFlow()

    fun consumeFailedSend() {
        _failedSend.value = null
    }

    
    private var uploadCredentials: Pair<String, String>? = null

    val myAvatarUrl: StateFlow<String?> = sessionStore.avatar
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            
            val cached = readDiskCache()
            if (cached != null) {
                applyConversation(cached)
            } else {
                _state.value = UiState.Loading
            }
            reload()
        }
    }

    private suspend fun reload() {
        try {
            val conversation = repository.chatConversation(touid)
            
            
            val local = (_state.value as? UiState.Success)?.data?.messages.orEmpty()
            val merged = mergeChatMessages(conversation, local)
            applyConversation(merged)
            writeDiskCache(merged)
        } catch (e: Exception) {
            
            if ((_state.value as? UiState.Success) == null) {
                _state.value = UiState.Error(e.message ?: "加载聊天记录失败")
            }
        }
    }

    private fun applyConversation(conversation: ChatConversation) {
        conversation.targetName?.takeIf { it.isNotBlank() }?.let {
            _targetName.value = it
        }
        _state.value = UiState.Success(conversation)
    }

    private suspend fun readDiskCache(): ChatConversation? =
        contentCache.read(CacheKeys.chat(touid), CacheKeys.LIST_TTL, allowExpired = true) {
            cacheJson.decodeFromString<ChatConversation>(it)
        }

    private suspend fun writeDiskCache(conversation: ChatConversation) {
        contentCache.write(
            key = CacheKeys.chat(touid),
            ttlMillis = CacheKeys.LIST_TTL,
            value = conversation,
            encode = { cacheJson.encodeToString(ChatConversation.serializer(), it) },
        )
    }

    fun sendMessage(text: String, onSent: () -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isSending.value) return
        _isSending.value = true
        
        onSent()
        val pendingId = "pending_${System.currentTimeMillis()}"
        val pending = ChatMessage(
            id = pendingId,
            text = trimmed,
            contentHtml = localContentHtml(trimmed),
            time = "刚刚",
            isSelf = true,
            avatarUrl = myAvatarUrl.value,
            sending = true,
        )
        val before = (_state.value as? UiState.Success)?.data
        val baseList = before?.messages.orEmpty()
        _state.value = UiState.Success(
            before?.copy(messages = baseList + pending) ?: ChatConversation(
                touid = touid,
                targetName = _targetName.value,
                messages = listOf(pending),
            ),
        )

        viewModelScope.launch {
            try {
                val currentConv = (_state.value as? UiState.Success)?.data
                var formhash = currentConv?.formhash
                if (formhash.isNullOrBlank()) {
                    formhash = runCatching { repository.userProfile(touid).formhash }.getOrNull()
                }
                if (formhash.isNullOrBlank()) {
                    AppToast.show("获取发送凭据失败，请重试")
                    rollbackPending(pendingId, trimmed)
                    return@launch
                }

                val result = repository.sendPrivateMessage(touid, formhash, trimmed)
                if (result.isSuccess) {
                    
                    
                    
                    
                    runCatching {
                        val refreshed = repository.chatConversation(touid)
                        val local = (_state.value as? UiState.Success)?.data?.messages.orEmpty()
                        val merged = mergeChatMessages(refreshed, local)
                        _state.value = UiState.Success(merged)
                        writeDiskCache(merged)
                    }.onFailure {
                        dropSendingFlag(pendingId)
                    }
                } else {
                    AppToast.show(result.exceptionOrNull()?.message ?: "发送失败")
                    rollbackPending(pendingId, trimmed)
                }
            } catch (e: Exception) {
                AppToast.show(e.message ?: "发送失败")
                rollbackPending(pendingId, trimmed)
            } finally {
                _isSending.value = false
            }
        }
    }

    
    private fun dropSendingFlag(pendingId: String) {
        val conv = (_state.value as? UiState.Success)?.data ?: return
        _state.value = UiState.Success(
            conv.copy(
                messages = conv.messages.map {
                    if (it.id == pendingId) it.copy(sending = false) else it
                },
            ),
        )
    }

    





    private fun rollbackPending(pendingId: String, failedMessage: String) {
        val conv = (_state.value as? UiState.Success)?.data ?: return
        _state.value = UiState.Success(
            conv.copy(messages = conv.messages.filterNot { it.id == pendingId }),
        )
        _failedSend.value = failedMessage
    }

    



    fun uploadChatImage(file: File, onResult: (String?) -> Unit) {
        if (_isUploading.value) return
        _isUploading.value = true
        viewModelScope.launch {
            try {
                val credentials = uploadCredentials
                    ?: repository.fetchUploadCredentials()?.also { uploadCredentials = it }
                if (credentials == null) {
                    AppToast.show("上传凭据获取失败，请重试")
                    onResult(null)
                    return@launch
                }
                val upload = repository.uploadImage(file, credentials.first, credentials.second)
                if (upload == null) {
                    AppToast.show("上传失败，可能超出大小限制")
                }
                onResult(upload?.url)
            } catch (e: Exception) {
                AppToast.show(e.message ?: "上传失败")
                onResult(null)
            } finally {
                _isUploading.value = false
            }
        }
    }

    



    private fun localContentHtml(text: String): String? {
        if (!RE_IMG_TAG.containsMatchIn(text)) return null
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        return RE_IMG_TAG.replace(escaped) { match ->
            "<img src=\"${match.groupValues[1].trim()}\" />"
        }
    }
}

