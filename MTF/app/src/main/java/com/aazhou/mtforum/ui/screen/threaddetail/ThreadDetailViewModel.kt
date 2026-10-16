package com.aazhou.mtforum.ui.screen.threaddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.model.EditAttachment
import com.aazhou.mtforum.data.model.HistoryItem
import com.aazhou.mtforum.data.model.LikeUser
import com.aazhou.mtforum.data.model.LikeResult
import com.aazhou.mtforum.data.model.PageProgress
import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.model.ThreadPage
import com.aazhou.mtforum.data.parser.ForumParser
import com.aazhou.mtforum.data.repo.ActionStore
import com.aazhou.mtforum.data.repo.BlockedUsersStore
import com.aazhou.mtforum.data.repo.ForumRepository
import com.aazhou.mtforum.data.repo.HistoryStore
import com.aazhou.mtforum.ui.common.UiState
import com.aazhou.mtforum.ui.common.isWaterReply
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private val RE_ATTACH_TAG_AID = Regex("""\[attachimg\](\d+)\[/attachimg\]""", RegexOption.IGNORE_CASE)


internal fun withAttachTags(message: String, aids: List<String>): String {
    if (aids.isEmpty()) return message
    val present = RE_ATTACH_TAG_AID.findAll(message).map { it.groupValues[1] }.toSet()
    val missing = aids.filterNot { it in present }
    if (missing.isEmpty()) return message
    val base = message.trimEnd()
    return buildString {
        append(base)
        missing.forEach { append("\n[attachimg]").append(it).append("[/attachimg]") }
    }
}

class ThreadDetailViewModel(
    private val tid: String,
    private val repository: ForumRepository,
    private val actionStore: ActionStore,
    private val historyStore: HistoryStore,
    myUidFlow: Flow<String>,
    private val blockedUsersStore: BlockedUsersStore,
) : ViewModel() {

    
    val myUid: StateFlow<String> =
        myUidFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    
    val blockedUids: StateFlow<Set<String>> = blockedUsersStore.blockedUids

    private val _state = MutableStateFlow<UiState<ThreadPage>>(UiState.Loading)
    val state: StateFlow<UiState<ThreadPage>> = _state.asStateFlow()

    



    private val atNames = mutableSetOf<String>()

    private val _replyState = MutableStateFlow<ReplyState>(ReplyState.Idle)
    val replyState: StateFlow<ReplyState> = _replyState.asStateFlow()

    





    private val _editTarget = MutableStateFlow<Post?>(null)
    val editTarget: StateFlow<Post?> = _editTarget.asStateFlow()

    private val _editForm = MutableStateFlow<EditFormState>(EditFormState.Idle)
    val editForm: StateFlow<EditFormState> = _editForm.asStateFlow()

    
    private val _orderType = MutableStateFlow(0)
    val orderType: StateFlow<Int> = _orderType.asStateFlow()

    
    private val _opOnly = MutableStateFlow(false)
    val opOnly: StateFlow<Boolean> = _opOnly.asStateFlow()

    





    private val _foldExpanded = MutableStateFlow(false)
    val foldExpanded: StateFlow<Boolean> = _foldExpanded.asStateFlow()

    
    private val _pendingScrollAnchor = MutableStateFlow<ScrollAnchor?>(null)
    val pendingScrollAnchor: StateFlow<ScrollAnchor?> = _pendingScrollAnchor.asStateFlow()

    
    private val _replyTarget = MutableStateFlow<Post?>(null)
    val replyTarget: StateFlow<Post?> = _replyTarget.asStateFlow()

    private val _followState = MutableStateFlow<FollowState>(FollowState.Idle)
    val followState: StateFlow<FollowState> = _followState.asStateFlow()

    
    private var followPending = false

    
    private val _liked = MutableStateFlow(false)
    val liked: StateFlow<Boolean> = _liked.asStateFlow()
    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount.asStateFlow()

    





    private var actionInitialized = false

    
    private val _favorited = MutableStateFlow(false)
    val favorited: StateFlow<Boolean> = _favorited.asStateFlow()
    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    







    private val pool = LinkedHashMap<String, Post>()

    






    private data class Sequence(val authorId: String?, val orderType: Int)

    
    private val currentSequence: Sequence
        get() = Sequence(opOnlyAuthorId(), _orderType.value)

    
    private val viewProgress = LinkedHashMap<Sequence, PageProgress>()

    





    private val fetchedPages = HashMap<Sequence, MutableSet<Int>>()

    





    private val pageOf = HashMap<Sequence, MutableMap<String, Int>>()

    





    private val shortPageRetry = HashMap<Sequence, Pair<Int, Int>>()

    







    private val exhaustedViews = HashSet<Sequence>()

    
    private var subject: String? = null
    private var forumName: String? = null
    private var opUid: String? = null
    private var fid: String? = null
    private var totalReplies: Int? = null

    



    private var session: ThreadPage? = null

    
    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    
    private val _loadMoreState = MutableStateFlow<LoadMoreState>(LoadMoreState.Idle)
    val loadMoreState: StateFlow<LoadMoreState> = _loadMoreState.asStateFlow()

    
    private val _waterPids = MutableStateFlow<Set<String>>(emptySet())
    val waterPids: StateFlow<Set<String>> = _waterPids.asStateFlow()

    
    private val _likeUsers = MutableStateFlow<List<LikeUser>?>(null)
    val likeUsers: StateFlow<List<LikeUser>?> = _likeUsers.asStateFlow()
    private val _likeUsersLoading = MutableStateFlow(false)
    val likeUsersLoading: StateFlow<Boolean> = _likeUsersLoading.asStateFlow()

    
    private var requestedAnchor: ScrollAnchor? = null

    
    private var recentVisiblePids: Set<String> = emptySet()

    
    private var dataGeneration = 0

    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load(1)
    }

    







    fun load(
        target: Int = currentMaxPage,
        orderType: Int = _orderType.value,
        stalePages: Set<Int> = emptySet(),
    ) {
        val seq = Sequence(opOnlyAuthorId(), orderType)
        val previousOpOnly = _opOnly.value
        val previousOrderType = _orderType.value
        
        
        val keepContent = pool.isNotEmpty() || _state.value is UiState.Success
        if (!keepContent) {
            _state.value = UiState.Loading
        }
        val generation = ++dataGeneration
        viewModelScope.launch {
            val data = try {
                repository.threadPage(tid, target, seq.orderType, seq.authorId)
            } catch (e: Exception) {
                if (generation != dataGeneration) return@launch
                requestedAnchor = null
                if (keepContent) {
                    
                    _opOnly.value = previousOpOnly
                    _orderType.value = previousOrderType
                    _actionMessage.value = friendlyError(e)
                } else {
                    _state.value = UiState.Error(friendlyError(e))
                }
                return@launch
            }
            
            if (generation != dataGeneration) return@launch
            if (!actionInitialized) {
                _likeCount.value = data.likeCount ?: 0
                _favoriteCount.value = data.favoriteCount ?: 0
                
                
                if (data.likedStateKnown) {
                    _liked.value = data.isLiked
                    actionStore.setLiked(tid, data.isLiked)
                } else {
                    _liked.value = actionStore.liked(tid) ?: false
                }
                if (data.favoritedStateKnown) {
                    _favorited.value = data.isFavorited
                    actionStore.setFavorited(tid, data.isFavorited)
                } else {
                    _favorited.value = actionStore.favorited(tid) ?: false
                }
                
                
                val following = if (data.followingStateKnown) {
                    actionStore.setFollowing(data.opUid.orEmpty(), data.isFollowingOp)
                    data.isFollowingOp
                } else {
                    actionStore.following(data.opUid.orEmpty()) ?: false
                }
                _followState.value = FollowState.Done(following, fromUser = false)
                actionInitialized = true
            }
            mergeIntoPool(seq, target, data)
            recordProgress(seq, target, data.totalPages)
            adoptMeta(data)
            publish()
            
            val stale = stalePages - target
            if (stale.isNotEmpty() && generation == dataGeneration) {
                refetchStale(seq, stale, generation)
                publish()
            }
        }
    }

    






    private suspend fun publish() {
        val view = viewPage()
        
        val water = withContext(Dispatchers.Default) {
            val op = view.posts.firstOrNull { it.isFirstPost } ?: view.posts.firstOrNull()
            view.posts.filter { it !== op && isWaterReply(it.contentHtml) }.map { it.pid }.toSet()
        }
        _state.value = UiState.Success(view)
        try {
            val op = view.posts.firstOrNull { it.isOp } ?: view.posts.firstOrNull()
            historyStore.add(
                HistoryItem(
                    tid = view.tid,
                    title = view.subject.orEmpty().ifBlank { "（无标题）" },
                    author = op?.authorName.orEmpty(),
                    avatarUrl = op?.avatarUrl,
                    forumName = view.forumName,
                    viewedAt = System.currentTimeMillis(),
                )
            )
        } catch (_: Exception) { }
        _waterPids.value = water
        _hasNext.value = hasNextForView()
        _loadMoreState.value =
            if (_hasNext.value) LoadMoreState.Idle else LoadMoreState.End
        requestedAnchor?.let {
            _pendingScrollAnchor.value = it
            requestedAnchor = null
        }
    }

    
    fun loadMore() {
        viewModelScope.launch {
            fetchNextPage()
        }
    }

    





    private suspend fun fetchNextPage(): Boolean {
        if (_loadingMore.value || !_hasNext.value) return false
        val current = _state.value as? UiState.Success ?: return false
        val generation = dataGeneration
        _loadingMore.value = true
        _loadMoreState.value = LoadMoreState.Loading
        val seq = currentSequence
        val startPage = nextPageToFetch(seq)
        if (startPage == 0) {
            
            _hasNext.value = false
            _loadMoreState.value = LoadMoreState.End
            return false
        }
        val knownTotalPages = maxOf(current.data.totalPages, progressOf(seq).totalPages)
        
        
        
        var lastProductivePage = progressOf(seq).maxPage
        var added = 0
        val failed = try {
            val data = repository.threadPage(tid, startPage, seq.orderType, seq.authorId)
            
            
            val saneTotalPages =
                if (data.posts.isEmpty() && data.totalPages > knownTotalPages) knownTotalPages
                else data.totalPages
            added = mergeIntoPool(seq, startPage, data)
            
            
            recordProgress(seq, startPage, saneTotalPages)
            if (added > 0) {
                lastProductivePage = startPage
            } else if (startPage > knownTotalPages) {
                
                
                
                
                val healed = lastProductivePage.coerceAtLeast(1)
                viewProgress[seq] =
                    PageProgress(authorId = seq.authorId, maxPage = healed, totalPages = healed)
            }
            false
        } catch (e: Exception) {
            
            startPage <= knownTotalPages
        }
        
        
        if (generation != dataGeneration) {
            _loadingMore.value = false
            return failed
        }
        
        
        if (!failed && added == 0) {
            exhaustedViews.add(seq)
        }
        
        val expected = expectedPoolSize()
        if (expected != null && pool.size < expected) {
            shortPageRetry[seq] = startPage to expected
        } else {
            shortPageRetry.remove(seq)
        }
        publish()
        _loadingMore.value = false
        return failed
    }

    





    fun snapshotForDownload(): ThreadPage? {
        if (_state.value !is UiState.Success) return null
        return viewPage().copy(posts = poolOrder())
    }

    
    private val currentMaxPage: Int
        get() = progressOf(currentSequence).maxPage.coerceAtLeast(1)

    
    private fun opOnlyAuthorId(): String? = if (_opOnly.value) opUid else null

    private fun progressOf(seq: Sequence): PageProgress =
        viewProgress[seq] ?: PageProgress(authorId = seq.authorId)

    
    private fun recordProgress(seq: Sequence, fetchedPage: Int, totalPages: Int) {
        val current = progressOf(seq)
        noteFetchedPage(seq, fetchedPage)
        viewProgress[seq] = PageProgress(
            authorId = seq.authorId,
            maxPage = maxOf(current.maxPage, fetchedPage),
            totalPages = maxOf(current.totalPages, totalPages),
        )
    }

    
    private fun noteFetchedPage(seq: Sequence, page: Int) {
        fetchedPages.getOrPut(seq) { mutableSetOf() } += page
    }

    






    private fun mergeIntoPool(seq: Sequence, pageNo: Int, page: ThreadPage): Int {
        var added = 0
        val pageMap = pageOf.getOrPut(seq) { mutableMapOf() }
        for (post in page.posts) {
            val existing = pool[post.pid]
            
            
            
            
            
            
            val merged = when {
                seq.authorId == null -> post
                existing != null -> post.copy(
                    floor = existing.floor,
                    floorLabel = existing.floorLabel,
                    sticky = existing.sticky,
                )
                else -> post.copy(floor = null)
            }
            if (existing == null) added++
            pool[post.pid] = merged
            pageMap[post.pid] = pageNo
        }
        return added
    }

    
    private fun adoptMeta(data: ThreadPage) {
        if (subject == null) subject = data.subject
        if (forumName == null) forumName = data.forumName
        if (opUid == null) opUid = data.opUid
        if (fid == null) fid = data.fid
        data.totalReplies?.let { totalReplies = it }
        session = data.copy(posts = emptyList())
        
        
        exhaustedViews.clear()
    }

    



    private fun expectedPoolSize(): Int? = totalReplies?.let { it + 1 }

    










    private fun nextPageToFetch(seq: Sequence): Int {
        
        if (seq in exhaustedViews) return 0
        val expected = expectedPoolSize()
        
        if (expected != null && pool.size >= expected) return 0
        val progress = viewProgress[seq]
        
        if (progress == null || progress.maxPage == 0) return 1
        val lastPage = maxOf(progress.totalPages, progress.maxPage).coerceAtLeast(1)
        val loaded = fetchedPages[seq].orEmpty()
        val missing = (1..lastPage).firstOrNull { it !in loaded }
        if (missing != null) return missing
        
        if (expected == null) return 0
        
        
        
        val (retriedPage, retriedExpected) = shortPageRetry[seq] ?: (0 to 0)
        if (retriedExpected != expected) return lastPage
        if (retriedPage == lastPage + 1) return 0
        return lastPage + 1
    }

    
    private fun hasNextForView(): Boolean = nextPageToFetch(currentSequence) > 0

    






    private fun poolOrder(): List<Post> {
        val ordered = ArrayList<Post>(pool.size)
        var last = 0
        for (post in pool.values) {
            val floor = post.floor
            if (floor != null) {
                ordered.add(post)
                last = floor
                continue
            }
            last += 1
            ordered.add(post.copy(floor = last, floorLabel = post.floorLabel ?: "$last#"))
        }
        return ordered.sortedBy { it.floor }
    }

    












    private fun viewPosts(): List<Post> {
        val all = poolOrder()
        val filtered = if (_opOnly.value) {
            val op = opUid
            all.filter { it.isFirstPost || it.isOp || (op != null && it.authorUid == op) }
        } else {
            all
        }
        val first = filtered.filter { it.isFirstPost }
        val sticky = filtered.filter { !it.isFirstPost && it.sticky }
        val rest = filtered.filter { !it.isFirstPost && !it.sticky }
        val orderedRest = if (_orderType.value == 1) rest.asReversed() else rest
        return first + sticky + orderedRest
    }

    
    private fun viewPage(): ThreadPage {
        val latest = session
        return ThreadPage(
            tid = tid,
            subject = subject,
            forumName = forumName,
            posts = viewPosts(),
            page = currentMaxPage,
            totalPages = latest?.totalPages ?: progressOf(currentSequence).totalPages,
            formhash = latest?.formhash,
            replyUrl = latest?.replyUrl,
            fid = fid ?: latest?.fid,
            opUid = opUid ?: latest?.opUid,
            totalReplies = totalReplies ?: latest?.totalReplies,
            likeCount = latest?.likeCount,
            favoriteCount = latest?.favoriteCount,
            isLiked = latest?.isLiked ?: false,
            likedStateKnown = latest?.likedStateKnown ?: false,
            isFavorited = latest?.isFavorited ?: false,
            favoritedStateKnown = latest?.favoritedStateKnown ?: false,
            noticeAuthor = latest?.noticeAuthor,
            isFollowingOp = latest?.isFollowingOp ?: false,
            followingStateKnown = latest?.followingStateKnown ?: false,
            
            
            uploadUid = latest?.uploadUid,
            uploadHash = latest?.uploadHash,
        )
    }

    




    suspend fun ensureUploadCredentials(): Pair<String, String>? {
        val current = (_state.value as? UiState.Success)?.data
        val uid = current?.uploadUid
        val hash = current?.uploadHash
        if (!uid.isNullOrBlank() && !hash.isNullOrBlank()) return uid to hash
        val seq = currentSequence
        val fresh = runCatching { repository.threadPage(tid, currentMaxPage, seq.orderType, seq.authorId) }
            .getOrNull() ?: return null
        val freshUid = fresh.uploadUid ?: return null
        val freshHash = fresh.uploadHash ?: return null
        
        _state.value = UiState.Success(
            current?.copy(uploadUid = freshUid, uploadHash = freshHash) ?: fresh.copy(posts = emptyList()),
        )
        return freshUid to freshHash
    }

    






    private fun refreshEditedFloor(pid: String) {
        viewModelScope.launch {
            if (refetchFloorPage(pid) == true) {
                publish()
                return@launch
            }
            
            
            recentVisiblePids = recentVisiblePids + pid
            refresh()
        }
    }

    





    fun refreshFirstPage() {
        if (_state.value !is UiState.Success && pool.isEmpty()) {
            load(1)
            return
        }
        val seq = currentSequence
        val generation = dataGeneration
        viewModelScope.launch {
            val fresh = try {
                repository.threadPage(tid, 1, seq.orderType, seq.authorId)
            } catch (e: Exception) {
                if (generation == dataGeneration) _actionMessage.value = friendlyError(e, "刷新失败")
                return@launch
            }
            if (generation != dataGeneration) return@launch
            mergeIntoPool(seq, 1, fresh)
            recordProgress(seq, 1, fresh.totalPages)
            adoptMeta(fresh)
            publish()
        }
    }

    






    fun refreshAfterReply() {
        if (_state.value !is UiState.Success && pool.isEmpty()) {
            load(1)
            return
        }
        val seq = currentSequence
        val generation = dataGeneration
        viewModelScope.launch {
            val pages = linkedSetOf(1)
            progressOf(seq).let { p ->
                val last = maxOf(p.totalPages, p.maxPage).coerceAtLeast(1)
                if (last > 1) pages += last
            }
            for (pageNo in pages) {
                val fresh = try {
                    repository.threadPage(tid, pageNo, seq.orderType, seq.authorId)
                } catch (e: Exception) {
                    if (generation == dataGeneration) {
                        _actionMessage.value = friendlyError(e, "刷新失败")
                    }
                    return@launch
                }
                if (generation != dataGeneration) return@launch
                mergeIntoPool(seq, pageNo, fresh)
                recordProgress(seq, pageNo, fresh.totalPages)
                adoptMeta(fresh)
            }
            publish()
        }
    }

    





    fun setReplyFilter(opOnly: Boolean, orderType: Int, anchor: ScrollAnchor? = null) {
        val changed = _opOnly.value != opOnly || _orderType.value != orderType
        if (!changed) return
        requestedAnchor = anchor
        _opOnly.value = opOnly
        _orderType.value = orderType
        val seq = currentSequence
        
        
        val target = nextPageToFetch(seq)
        
        val stale = stalePagesOnSwitch(seq, skip = target)
        if (target > 0) {
            load(target, orderType = seq.orderType, stalePages = stale)
            return
        }
        
        
        val generation = dataGeneration
        viewModelScope.launch {
            if (generation == dataGeneration) publish()
            refetchStale(seq, stale, generation)
            if (generation == dataGeneration) publish()
        }
    }

    



    private fun friendlyError(e: Exception, fallback: String = "加载失败"): String = when (e) {
        is IOException -> "网络连接失败，请检查网络"
        else -> e.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    
    fun consumePendingScrollAnchor() {
        _pendingScrollAnchor.value = null
    }

    
    fun toggleFoldExpanded() {
        _foldExpanded.value = !_foldExpanded.value
    }

    
    fun setReplyTarget(post: Post?) {
        _replyTarget.value = post
    }

    
    fun report(pid: String?, reason: String, message: String) {
        val data = (_state.value as? UiState.Success)?.data ?: return
        if (!repository.isLoggedIn() || data.formhash.isNullOrBlank() || data.fid.isNullOrBlank()) {
            _actionMessage.value = "请先登录"
            return
        }
        viewModelScope.launch {
            val ok = runCatching {
                repository.reportPost(tid, data.fid, data.formhash, pid, reason, message)
            }.getOrDefault(false)
            _actionMessage.value = if (ok) "举报已提交" else "举报失败，请稍后重试"
        }
    }

    



    fun stickReply(pid: String, stick: Boolean) {
        val data = (_state.value as? UiState.Success)?.data ?: return
        val fid = data.fid
        if (!repository.isLoggedIn() || data.formhash.isNullOrBlank() || fid.isNullOrBlank()) {
            _actionMessage.value = "请先登录"
            return
        }
        viewModelScope.launch {
            val ok = runCatching {
                repository.stickReply(tid, fid, data.formhash, pid, stick)
            }.getOrDefault(false)
            if (!ok) {
                _actionMessage.value = if (stick) "置顶失败，站点可能不允许" else "取消置顶失败"
                return@launch
            }
            _actionMessage.value = if (stick) "已置顶该回复" else "已取消置顶"
            reloadAllAfterStick()
        }
    }

    







    private fun reloadAllAfterStick() {
        pool.clear()
        pageOf.clear()
        viewProgress.clear()
        fetchedPages.clear()
        shortPageRetry.clear()
        exhaustedViews.clear()
        dataGeneration++
        load(1)
    }

    
    fun deleteReply(pid: String) {
        val data = (_state.value as? UiState.Success)?.data ?: return
        if (!repository.isLoggedIn() || data.formhash.isNullOrBlank()) {
            _actionMessage.value = "请先登录"
            return
        }
        viewModelScope.launch {
            val submitted = runCatching {
                repository.deletePost(tid, pid, data.formhash)
            }.getOrDefault(false)
            
            
            
            val stillThere = refetchFloorPage(pid)
            val gone = if (stillThere == null) submitted else !stillThere
            if (gone) {
                
                pool.remove(pid)
                publish()
                _actionMessage.value = "已删除该回复"
            } else {
                _actionMessage.value = "删除未生效，站点可能不允许删除"
            }
        }
    }

    
    private suspend fun fetchPageOrNull(seq: Sequence, page: Int): ThreadPage? = try {
        repository.threadPage(tid, page, seq.orderType, seq.authorId).takeIf { it.posts.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    












    private suspend fun refetchFloorPage(pid: String): Boolean? {
        var fetched = false
        for ((seq, page) in pageOf.entries.mapNotNull { (seq, map) -> map[pid]?.let { seq to it } }) {
            
            
            if (seq.authorId != null) continue
            val data = fetchPageOrNull(seq, page) ?: continue
            mergeIntoPool(seq, page, data)
            fetched = true
            if (data.posts.any { it.pid == pid }) return true
        }
        return if (fetched) false else null
    }

    fun toggleFollow() {
        if (followPending) return
        val current = _state.value as? UiState.Success ?: return
        val uid = current.data.opUid
        val formhash = current.data.formhash
        if (uid.isNullOrBlank() || formhash.isNullOrBlank()) {
            
            _followState.value = FollowState.Error(
                if (repository.isLoggedIn()) "页面还在加载，稍等一下再关注" else "请先登录后再关注",
            )
            return
        }
        val following = (_followState.value as? FollowState.Done)?.following ?: false
        
        _followState.value = FollowState.Done(!following)
        followPending = true
        viewModelScope.launch {
            try {
                repository.followTopic(uid, formhash, follow = !following)
                
                actionStore.setFollowing(uid, !following)
            } catch (e: Exception) {
                
                _followState.value = FollowState.Error(e.message ?: "操作失败")
            } finally {
                followPending = false
            }
        }
    }

    fun consumeFollowState() {
        _followState.value = FollowState.Idle
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    




    fun toggleLike(): Boolean {
        val current = _state.value as? UiState.Success ?: return false
        val formhash = current.data.formhash
        if (!repository.isLoggedIn() || formhash.isNullOrBlank()) return false
        val target = !_liked.value
        
        _liked.value = target
        _likeCount.value = (_likeCount.value + if (target) 1 else -1).coerceAtLeast(0)
        val optimistic = _likeCount.value
        viewModelScope.launch { actionStore.setLiked(tid, target) }
        viewModelScope.launch {
            val result = runCatching { repository.setLike(tid, formhash, target) }
                .getOrDefault(LikeResult(false))
            if (result.success) {
                
                
                val corrected = result.serverCount
                if (corrected != null &&
                    ((target && corrected >= optimistic) || (!target && corrected <= optimistic))
                ) {
                    _likeCount.value = corrected
                }
            } else {
                _liked.value = !target
                _likeCount.value = (_likeCount.value + if (target) -1 else 1).coerceAtLeast(0)
                actionStore.setLiked(tid, !target)
            }
        }
        return true
    }

    
    fun toggleFavorite(): Boolean {
        val current = _state.value as? UiState.Success ?: return false
        val formhash = current.data.formhash
        if (!repository.isLoggedIn() || formhash.isNullOrBlank()) return false
        val target = !_favorited.value
        _favorited.value = target
        _favoriteCount.value = (_favoriteCount.value + if (target) 1 else -1).coerceAtLeast(0)
        viewModelScope.launch { actionStore.setFavorited(tid, target) }
        viewModelScope.launch {
            val ok = runCatching { repository.setFavorite(tid, formhash, target) }.getOrDefault(false)
            if (!ok) {
                _favorited.value = !target
                _favoriteCount.value = (_favoriteCount.value + if (target) -1 else 1).coerceAtLeast(0)
                actionStore.setFavorited(tid, !target)
                _actionMessage.value = if (target) "收藏失败，请稍后重试" else "取消收藏失败"
            }
        }
        return true
    }

    
    fun noteAtUsers(names: List<String>) {
        atNames += names.filter { it.isNotBlank() }
    }

    



    fun beginEdit(post: Post) {
        if (!repository.isLoggedIn()) {
            _actionMessage.value = "请先登录"
            return
        }
        _editTarget.value = post
        _editForm.value = EditFormState.Loading
        viewModelScope.launch {
            try {
                val data = repository.fetchEditPostInit(tid, post.pid)
                _editAttachments.value = data.attachments
                
                
                val body = withAttachTags(data.message, data.attachments.map { it.aid })
                
                
                editInitialAids = data.attachments.mapTo(HashSet()) { it.aid }
                _editForm.value = when {
                    data.permissionDenied != null -> EditFormState.Error(data.permissionDenied)
                    else -> EditFormState.Ready(post.pid, body, data.attachments)
                }
            } catch (e: Exception) {
                _editForm.value = EditFormState.Error(friendlyError(e, "加载编辑内容失败"))
            }
        }
    }

    
    fun consumeEditForm() {
        _editForm.value = EditFormState.Idle
    }

    





    private val _editAttachments = MutableStateFlow<List<EditAttachment>>(emptyList())
    val editAttachments: StateFlow<List<EditAttachment>> = _editAttachments.asStateFlow()

    







    private var editInitialAids: Set<String> = emptySet()

    
    fun cancelEdit() {
        _editForm.value = EditFormState.Idle
        _editTarget.value = null
        _editAttachments.value = emptyList()
    }

    
    fun submitEdit(message: String) {        val current = _state.value
        if (current !is UiState.Success) {
            _editForm.value = EditFormState.Error("页面未就绪")
            return
        }
        val formhash = current.data.formhash
        val target = _editTarget.value
        if (target == null || formhash.isNullOrBlank()) {
            _editForm.value = EditFormState.Error("缺少编辑令牌，请重新进入帖子")
            return
        }
        
        val text = normalizeAtMentions(message, atNames)
        _editForm.value = EditFormState.Loading
        viewModelScope.launch {
            try {
                
                val stillReferenced = repository.attachAidsIn(text).toSet()
                val deletedAids = editInitialAids - stillReferenced
                
                
                for (aid in deletedAids) {
                    repository.deleteAttach(tid, target.pid, aid, formhash)
                }
                val result = repository.editPost(
                    tid = tid,
                    pid = target.pid,
                    formhash = formhash,
                    message = text,
                    
                    subject = if (target.isFirstPost) subject else null,
                )
                if (result.success) {
                    _editForm.value = EditFormState.Idle
                    _editTarget.value = null
                    _editAttachments.value = emptyList()
                    _actionMessage.value = result.message ?: "已保存"
                    refreshEditedFloor(target.pid)
                } else {
                    _editForm.value = EditFormState.Error(result.message ?: "编辑未生效，请稍后重试")
                }
            } catch (e: Exception) {
                _editForm.value = EditFormState.Error(friendlyError(e, "编辑失败"))
            }
        }
    }

    fun reply(message: String) {
        val current = _state.value
        if (current !is UiState.Success) {
            _replyState.value = ReplyState.Error("页面未就绪")
            return
        }
        val formhash = current.data.formhash
        val fid = current.data.fid
        if (formhash.isNullOrBlank() || fid.isNullOrBlank()) {
            _replyState.value = ReplyState.Error("缺少回复令牌，请重新进入帖子")
            return
        }
        val target = _replyTarget.value
        val notice = target?.let {
            ForumParser.replyQuoteNotice(it.authorName, it.timeText, it.contentHtml, it.pid, tid)
        }
        
        
        val message = normalizeAtMentions(message, atNames)
        
        
        val noticeAuthor = if (target == null) current.data.noticeAuthor else null
        _replyState.value = ReplyState.Sending
        viewModelScope.launch {
            try {
                val result = repository.reply(
                    fid = fid,
                    tid = tid,
                    formhash = formhash,
                    message = message,
                    noticeAuthor = noticeAuthor,
                    noticeTrimStr = notice,
                )
                if (result.success) {
                    _replyTarget.value = null
                    _replyState.value = ReplyState.Success
                } else {
                    _replyState.value = ReplyState.Error(result.message ?: "回复被拒绝（可能需登录或触发风控）")
                }
            } catch (e: Exception) {
                _replyState.value = ReplyState.Error(e.message ?: "回复失败")
            }
        }
    }

    fun consumeReplyState() {
        _replyState.value = ReplyState.Idle
    }

    sealed interface ReplyState {
        data object Idle : ReplyState
        data object Sending : ReplyState
        data object Success : ReplyState
        data class Error(val message: String) : ReplyState
    }

    
    sealed interface EditFormState {
        data object Idle : EditFormState
        data object Loading : EditFormState
        



        data class Ready(
            val pid: String,
            val text: String,
            val attachments: List<EditAttachment> = emptyList(),
        ) : EditFormState
        data class Error(val message: String) : EditFormState
    }

    
    sealed interface LoadMoreState {
        data object Idle : LoadMoreState
        data object Loading : LoadMoreState
        data object End : LoadMoreState
    }

    







    fun refresh() {
        if (_isRefreshing.value) return
        if (_state.value !is UiState.Success && pool.isEmpty()) {
            load(1)
            return
        }
        _isRefreshing.value = true
        val seq = currentSequence
        val generation = dataGeneration
        
        val seenPages = recentVisiblePids
            .mapNotNull { pageOf[seq]?.get(it) }
            .filter { it != 1 }
            .toSortedSet()
        viewModelScope.launch {
            try {
                val fresh = repository.threadPage(tid, 1, seq.orderType, seq.authorId)
                
                if (generation != dataGeneration) return@launch
                mergeIntoPool(seq, 1, fresh)
                recordProgress(seq, 1, fresh.totalPages)
                adoptMeta(fresh)
                for (page in seenPages) {
                    if (generation != dataGeneration) break
                    val extra = try {
                        repository.threadPage(tid, page, seq.orderType, seq.authorId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        
                        continue
                    }
                    mergeIntoPool(seq, page, extra)
                }
                publish()
            } catch (e: Exception) {
                if (generation == dataGeneration) _actionMessage.value = friendlyError(e, "刷新失败")
            }
            if (generation == dataGeneration) _isRefreshing.value = false
        }
    }

    
    fun noteVisiblePids(pids: Set<String>) {
        recentVisiblePids = pids
    }

    







    private suspend fun refetchStale(seq: Sequence, pages: Set<Int>, generation: Int) {
        for (page in pages) {
            if (page <= 0 || generation != dataGeneration) return
            val data = try {
                repository.threadPage(tid, page, seq.orderType, seq.authorId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                continue
            }
            mergeIntoPool(seq, page, data)
        }
    }

    







    private fun stalePagesOnSwitch(seq: Sequence, skip: Int): Set<Int> {
        val progress = progressOf(seq)
        val lastPage = maxOf(progress.totalPages, progress.maxPage).coerceAtLeast(1)
        val seen = recentVisiblePids.mapNotNull { pageOf[seq]?.get(it) }
        return (setOf(1, lastPage) + seen).filter { it != skip }.toSortedSet()
    }

    
    fun reward(pid: String?, amount: Int, message: String?) {
        val data = (_state.value as? UiState.Success)?.data ?: return
        if (!repository.isLoggedIn() || data.formhash.isNullOrBlank()) {
            _actionMessage.value = "请先登录"
            return
        }
        viewModelScope.launch {
            val ok = runCatching {
                repository.rewardAuthor(tid, pid, data.formhash, amount, message)
            }.getOrDefault(false)
            _actionMessage.value =
                if (ok) "打赏成功" else "打赏失败（可能今日已评分或积分不足）"
        }
    }

    
    fun loadLikeUsers() {
        if (_likeUsersLoading.value) return
        _likeUsersLoading.value = true
        viewModelScope.launch {
            _likeUsers.value = runCatching { repository.likeUsers(tid) }.getOrDefault(emptyList())
            _likeUsersLoading.value = false
        }
    }

    fun clearLikeUsers() {
        _likeUsers.value = null
    }

    sealed interface FollowState {
        data object Idle : FollowState

        



        data class Done(val following: Boolean, val fromUser: Boolean = true) : FollowState
        data class Error(val message: String) : FollowState
    }
}


data class ScrollAnchor(val index: Int, val offset: Int)










private const val FETCH_ORDER = 0

