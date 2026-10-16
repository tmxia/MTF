package com.aazhou.mtforum.ui.screen.postedit

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.aazhou.mtforum.data.model.EditPostPageData
import com.aazhou.mtforum.data.model.NewThreadPageData
import com.aazhou.mtforum.data.model.PostDraft
import com.aazhou.mtforum.data.model.ThreadType
import com.aazhou.mtforum.data.parser.SmileyPack
import com.aazhou.mtforum.data.repo.DraftStore
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.SmileyStore
import com.aazhou.mtforum.ui.screen.threaddetail.insertAtSelection
import com.aazhou.mtforum.ui.screen.threaddetail.normalizeAtMentions
import com.aazhou.mtforum.ui.screen.threaddetail.withAttachTags
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File


data class UploadedImageAttachment(
    val aid: String,
    





    val sourceUri: Uri?,
    val fileName: String,
    
    val remoteUrl: String? = null,
) {
    
    val imageModel: Any get() = sourceUri ?: remoteUrl.orEmpty()
}

class PostEditViewModel(
    val fid: String,
    val forumName: String,
    private val repository: ForumRepository,
    private val smileyStore: SmileyStore,
    
    private val editTid: String? = null,
    private val editPid: String? = null,
    private val draftStore: DraftStore? = null,
) : ViewModel() {

    
    val editing: Boolean = !editTid.isNullOrBlank() && !editPid.isNullOrBlank()

    private val _initData = MutableStateFlow<NewThreadPageData?>(null)
    val initData: StateFlow<NewThreadPageData?> = _initData.asStateFlow()

    private val _editData = MutableStateFlow<EditPostPageData?>(null)
    val editData: StateFlow<EditPostPageData?> = _editData.asStateFlow()

    
    private var pendingTypeId: String? = null

    






    private var editInitialAids: Set<String> = emptySet()

    private val _loadingInit = MutableStateFlow(true)
    val loadingInit: StateFlow<Boolean> = _loadingInit.asStateFlow()

    private val _selectedType = MutableStateFlow<ThreadType?>(null)
    val selectedType: StateFlow<ThreadType?> = _selectedType.asStateFlow()

    private val _subject = MutableStateFlow("")
    val subject: StateFlow<String> = _subject.asStateFlow()

    private val _message = MutableStateFlow(TextFieldValue())
    val message: StateFlow<TextFieldValue> = _message.asStateFlow()

    private val _uploadedImages = MutableStateFlow<List<UploadedImageAttachment>>(emptyList())
    val uploadedImages: StateFlow<List<UploadedImageAttachment>> = _uploadedImages.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _smileyPacks = MutableStateFlow<List<SmileyPack>>(emptyList())
    val smileyPacks: StateFlow<List<SmileyPack>> = _smileyPacks.asStateFlow()

    private val _atUsers = MutableStateFlow<List<String>>(emptyList())
    val atUsers: StateFlow<List<String>> = _atUsers.asStateFlow()

    val recentCodes = smileyStore.recent

    private val _state = MutableStateFlow<PostState>(PostState.Idle)
    val state: StateFlow<PostState> = _state.asStateFlow()

    val canPost: Boolean get() = repository.isLoggedIn()

    init {
        if (editing) {
            loadEditData()
        } else {
            applyDraft()
            loadInitData()
        }
        loadSmileysAndAtUsers()
    }

    
    private fun loadEditData() {
        viewModelScope.launch {
            _loadingInit.value = true
            try {
                val data = repository.fetchEditPostInit(editTid!!, editPid!!)
                _editData.value = data
                _subject.value = data.subject.orEmpty()
                
                
                _message.value = TextFieldValue(withAttachTags(data.message, data.attachments.map { it.aid }))
                
                
                _uploadedImages.value = data.attachments.map {
                    UploadedImageAttachment(
                        aid = it.aid,
                        sourceUri = null,
                        fileName = it.fileName,
                        remoteUrl = it.url,
                    )
                }
                editInitialAids = data.attachments.mapTo(HashSet()) { it.aid }
                if (data.permissionDenied != null) {
                    _state.value = PostState.Error(data.permissionDenied)
                }
            } catch (e: Exception) {
                _state.value = PostState.Error(e.message ?: "加载编辑数据失败")
            } finally {
                _loadingInit.value = false
            }
        }
    }

    
    private fun applyDraft() {
        val draft = draftStore?.items?.value?.firstOrNull { it.fid == fid } ?: return
        _subject.value = draft.subject
        
        _message.value = TextFieldValue(draft.message, TextRange(draft.message.length))
        pendingTypeId = draft.typeId
    }

    



    fun currentDraft(): PostDraft? {
        if (editing) return null
        val subject = _subject.value.trim()
        val message = _message.value.text
        if (subject.isBlank() && message.isBlank()) return null
        return PostDraft(
            fid = fid,
            forumName = forumName,
            subject = subject,
            message = message,
            typeId = _selectedType.value?.id ?: pendingTypeId,
            savedAt = System.currentTimeMillis(),
        )
    }

    private fun loadInitData() {
        viewModelScope.launch {
            _loadingInit.value = true
            try {
                val data = repository.fetchNewThreadInit(fid)
                _initData.value = data
                if (data.types.isNotEmpty() && _selectedType.value == null) {
                    
                    _selectedType.value = pendingTypeId
                        ?.let { id -> data.types.firstOrNull { it.id == id } }
                        ?: data.types.first()
                    pendingTypeId = null
                }
                if (data.permissionDenied != null) {
                    _state.value = PostState.Error(data.permissionDenied)
                }
            } catch (e: Exception) {
                _state.value = PostState.Error(e.message ?: "加载发帖数据失败")
            } finally {
                _loadingInit.value = false
            }
        }
    }

    private fun loadSmileysAndAtUsers() {
        viewModelScope.launch {
            runCatching {
                _smileyPacks.value = repository.smileyPacks()
            }
            runCatching {
                _atUsers.value = repository.fetchAtUsers()
            }
        }
    }

    fun onSubjectChange(newSubject: String) {
        if (newSubject.length <= 80) {
            _subject.value = newSubject
        }
    }

    fun onMessageChange(newMessage: TextFieldValue) {
        _message.value = newMessage
    }

    
    fun noteAtUsers(names: List<String>) {
        val merged = (_atUsers.value + names).filter { it.isNotBlank() }.distinct()
        _atUsers.value = merged
    }

    fun onSelectType(type: ThreadType?) {
        _selectedType.value = type
    }

    fun recordSmileyUsed(code: String) {
        viewModelScope.launch {
            smileyStore.addRecent(code)
        }
    }

    





    suspend fun uploadImages(sources: List<Uri>, prepare: suspend (Uri) -> File): List<String> {
        
        val uid = _initData.value?.uploadUid ?: _editData.value?.uploadUid
        val hash = _initData.value?.uploadHash ?: _editData.value?.uploadHash
        if (uid.isNullOrBlank() || hash.isNullOrBlank()) {
            _state.value = PostState.Error("图片上传凭据未就绪，请稍后重试")
            return emptyList()
        }
        _uploading.value = true
        return try {
            val aids = mutableListOf<String>()
            for (uri in sources) {
                val file = try {
                    prepare(uri)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    continue
                }
                val aid = try {
                    repository.uploadReplyImage(file, uid, hash)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                } ?: continue
                _uploadedImages.value = _uploadedImages.value + UploadedImageAttachment(
                    aid = aid,
                    sourceUri = uri,
                    fileName = file.name,
                )
                aids += aid
            }
            aids
        } finally {
            _uploading.value = false
        }
    }

    fun removeAttachment(attachment: UploadedImageAttachment) {
        _uploadedImages.value = _uploadedImages.value.filter { it.aid != attachment.aid }
        
        if (attachment.remoteUrl == null) return
        val current = _message.value
        val tag = "[attachimg]${attachment.aid}[/attachimg]"
        val start = current.text.indexOf(tag)
        if (start < 0) return
        val end = start + tag.length
        val cursor = current.selection.start
        val newCursor = if (cursor >= end) cursor - tag.length else minOf(cursor, start)
        _message.value = current.copy(
            text = current.text.removeRange(start, end),
            selection = TextRange(newCursor.coerceIn(0, current.text.length - tag.length)),
        )
    }

    fun insertTextAtSelection(insertText: String) {
        _message.value = _message.value.insertAtSelection(insertText)
    }

    fun submit() {
        if (!repository.isLoggedIn()) {
            _state.value = PostState.Error("请先登录后再发帖")
            return
        }
        val sub = _subject.value.trim()
        val msg = _message.value.text.trim()
        if (msg.isBlank()) {
            _state.value = PostState.Error("请输入帖子内容")
            return
        }
        if (editing) {
            submitEdit(sub, msg)
            return
        }
        if (sub.isBlank()) {
            _state.value = PostState.Error("请输入帖子标题")
            return
        }
        val init = _initData.value
        val formhash = init?.formhash
        if (formhash.isNullOrBlank()) {
            _state.value = PostState.Error("无法获取发帖令牌，请稍后重试")
            return
        }
        _state.value = PostState.Sending
        viewModelScope.launch {
            try {
                val aids = _uploadedImages.value.map { it.aid }
                val result = repository.submitNewThread(
                    fid = fid,
                    formhash = formhash,
                    subject = sub,
                    
                    
                    message = normalizeAtMentions(_message.value.text, _atUsers.value),
                    posttime = init.posttime,
                    typeid = _selectedType.value?.id,
                    aids = aids,
                )
                if (result.success) {
                    _state.value = PostState.Success
                } else {
                    _state.value = PostState.Error(result.message ?: "发帖失败，请稍后重试")
                }
            } catch (e: Exception) {
                _state.value = PostState.Error(e.message ?: "发帖失败")
            }
        }
    }

    
    private fun submitEdit(subject: String, message: String) {
        val edit = _editData.value
        val formhash = edit?.formhash
        if (formhash.isNullOrBlank()) {
            _state.value = PostState.Error("无法获取编辑令牌，请稍后重试")
            return
        }
        
        val isFirstPost = !edit.subject.isNullOrBlank()
        if (isFirstPost && subject.isBlank()) {
            _state.value = PostState.Error("请输入帖子标题")
            return
        }
        _state.value = PostState.Sending
        viewModelScope.launch {
            try {
                val text = normalizeAtMentions(_message.value.text, _atUsers.value)
                
                val deletedAids = editInitialAids - repository.attachAidsIn(text).toSet()
                
                
                for (aid in deletedAids) {
                    repository.deleteAttach(editTid!!, editPid!!, aid, formhash)
                }
                val result = repository.editPost(
                    tid = editTid!!,
                    pid = editPid!!,
                    formhash = formhash,
                    message = text,
                    subject = if (isFirstPost) subject else null,
                )
                _state.value = if (result.success) {
                    PostState.Success
                } else {
                    PostState.Error(result.message ?: "编辑未生效，请稍后重试")
                }
            } catch (e: Exception) {
                _state.value = PostState.Error(e.message ?: "编辑失败")
            }
        }
    }

    fun consume() {
        _state.value = PostState.Idle
    }

    sealed interface PostState {
        data object Idle : PostState
        data object Sending : PostState
        data object Success : PostState
        data class Error(val message: String) : PostState
    }
}
