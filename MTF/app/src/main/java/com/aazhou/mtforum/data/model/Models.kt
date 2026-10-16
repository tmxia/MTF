package com.aazhou.mtforum.data.model

import kotlinx.serialization.Serializable


@Serializable
data class ForumGroup(
    val name: String,
    val forums: List<Forum>,
)


@Serializable
data class Forum(
    val fid: String,
    val name: String,
    val icon: String? = null,
    val todayPosts: Int = 0,
)


@Serializable
data class Thread(
    val tid: String,
    val subject: String,
    val authorName: String? = null,
    val authorUid: String? = null,
    val avatarUrl: String? = null,
    val level: String? = null,
    val timeText: String? = null,
    val replyCount: String? = null,
    val viewCount: String? = null,
    val excerpt: String? = null,
    val isAnnouncement: Boolean = false,
    
    val forumName: String? = null,
    
    val imageUrls: List<String> = emptyList(),
    
    val likeCount: String? = null,
    
    val isSticky: Boolean = false,
)


@Serializable
data class ForumPage(
    val forumName: String?,
    val announcements: List<Thread>,
    val threads: List<Thread>,
    val page: Int,
    val hasNext: Boolean,
)


@Serializable
data class ThreadPage(
    val tid: String,
    val subject: String?,
    val forumName: String?,
    val posts: List<Post>,
    val page: Int,
    val totalPages: Int,
    val formhash: String?,
    val replyUrl: String?,
    
    val noticeAuthor: String? = null,
    val fid: String? = null,
    
    val opUid: String? = null,
    
    val totalReplies: Int? = null,
    
    val likeCount: Int? = null,
    
    val favoriteCount: Int? = null,
    
    val isLiked: Boolean = false,
    
    val likedStateKnown: Boolean = false,
    
    val isFavorited: Boolean = false,
    
    val favoritedStateKnown: Boolean = false,
    
    val isFollowingOp: Boolean = false,
    
    val followingStateKnown: Boolean = false,
    
    val uploadUid: String? = null,
    val uploadHash: String? = null,
)


@Serializable
data class Post(
    val pid: String,
    val authorName: String,
    val authorUid: String? = null,
    val avatarUrl: String? = null,
    val level: String? = null,
    val timeText: String? = null,
    val location: String? = null,
    val contentHtml: String = "",
    val isOp: Boolean = false,
    
    val isFirstPost: Boolean = false,
    
    val floor: Int? = null,
    
    val floorLabel: String? = null,
    
    val quote: PostQuote? = null,
    
    val editedText: String? = null,
    
    val hasHiddenContent: Boolean = false,
    
    val sticky: Boolean = false,
)







@Serializable
data class PageProgress(
    val authorId: String? = null,
    val maxPage: Int = 0,
    val totalPages: Int = 0,
)







@Serializable
data class ThreadSnapshot(
    val subject: String? = null,
    val forumName: String? = null,
    val opUid: String? = null,
    val fid: String? = null,
    
    val totalReplies: Int? = null,
    val posts: List<Post> = emptyList(),
    val progress: List<PageProgress> = emptyList(),
)







@Serializable
data class DownloadedThread(
    val tid: String,
    val subject: String,
    val forumName: String? = null,
    val fileName: String,
    val uri: String,
    val savedAt: Long,
    val replyCount: Int? = null,
    val sizeBytes: Long = 0,
)


@Serializable
data class PostQuote(
    
    val author: String? = null,
    





    val time: String? = null,
    
    val text: String = "",
    
    val pid: String? = null,
)


data class LikeUser(
    val uid: String,
    val name: String,
    val avatarUrl: String? = null,
)







data class LikeResult(
    val success: Boolean,
    val remainingToday: Int? = null,
    val serverCount: Int? = null,
    
    val message: String? = null,
)


data class ReplyResult(
    val success: Boolean,
    val message: String? = null,
)


data class CheckInStatus(
    val hasSigned: Boolean,
    val formhash: String?,
)


data class CheckInResult(
    val success: Boolean,
    val message: String,
)



data class LoginResult(
    val success: Boolean,
    val message: String? = null,
    val needsCaptcha: Boolean = false,
    val captchaUrl: String? = null,
)


data class UserProfile(
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
    
    val isFollowing: Boolean? = null,
    
    val formhash: String? = null,
) {
    val isLoggedIn: Boolean get() = !uid.isNullOrEmpty() && uid != "0"
}


@Serializable
data class SearchResult(
    val threads: List<Thread>,
    





    val nextUrl: String? = null,
) {
    val hasNext: Boolean get() = nextUrl != null
}


@Serializable
data class HomeData(
    val hotForums: List<Forum>,
    val hotThreads: List<Thread>,
)







@Serializable
data class Notice(
    val id: String,
    val text: String,
    val timeText: String? = null,
    val tid: String? = null,
    
    val avatarUrl: String? = null,
    
    val uid: String? = null,
)


data class NoticePage(
    val loggedOut: Boolean = false,
    val notices: List<Notice> = emptyList(),
    
    val totalPages: Int = 1,
)







@Serializable
data class AtUser(
    val uid: String,
    val name: String,
    val avatarUrl: String? = null,
    val subtitle: String = "",
)


enum class NoticeGroup(val title: String) {
    PM("我的消息"),
    FANS("我的粉丝"),
    MY_POST("我的帖子"),
    INTERACTIVE("坛友互动"),
    SYSTEM("系统提醒"),
    APP("应用提醒"),
}









enum class NoticeTab(
    val title: String,
    val group: NoticeGroup,
    val view: String? = null,
    val type: String? = null,
    val pmFilter: String? = null,
) {
    PM_PRIVATE("私人消息", NoticeGroup.PM, pmFilter = "privatepm"),
    PM_ANNOUNCE("公共消息", NoticeGroup.PM, pmFilter = "announcepm"),

    POST("帖子", NoticeGroup.MY_POST, view = "mypost", type = "post"),
    PCOMMENT("点评", NoticeGroup.MY_POST, view = "mypost", type = "pcomment"),
    ACTIVITY("活动", NoticeGroup.MY_POST, view = "mypost", type = "activity"),
    REWARD("悬赏", NoticeGroup.MY_POST, view = "mypost", type = "reward"),
    GOODS("商品", NoticeGroup.MY_POST, view = "mypost", type = "goods"),
    AT("提到我的", NoticeGroup.MY_POST, view = "mypost", type = "at"),

    POKE("打招呼", NoticeGroup.INTERACTIVE, view = "interactive", type = "poke"),
    FRIEND("好友", NoticeGroup.INTERACTIVE, view = "interactive", type = "friend"),
    WALL("留言", NoticeGroup.INTERACTIVE, view = "interactive", type = "wall"),
    COMMENT("评论", NoticeGroup.INTERACTIVE, view = "interactive", type = "comment"),
    CLICK("挺你", NoticeGroup.INTERACTIVE, view = "interactive", type = "click"),
    SHARE("分享", NoticeGroup.INTERACTIVE, view = "interactive", type = "sharenotice"),

    SYSTEM("系统提醒", NoticeGroup.SYSTEM, view = "system"),
    APP("应用提醒", NoticeGroup.APP, view = "app"),
    ;

    
    val key: String get() = name
}


fun NoticeGroup.tabs(): List<NoticeTab> = NoticeTab.entries.filter { it.group == this }







val PREFETCH_TABS: List<NoticeTab> = listOf(
    NoticeTab.POST,
    NoticeTab.PCOMMENT,
    NoticeTab.ACTIVITY,
    NoticeTab.REWARD,
    NoticeTab.GOODS,
    NoticeTab.AT,
    NoticeTab.PM_PRIVATE,
    NoticeTab.PM_ANNOUNCE,
)


val BACKGROUND_TABS: List<NoticeTab> = PREFETCH_TABS


data class PmPage(
    val loggedOut: Boolean = false,
    val messages: List<PrivateMessage> = emptyList(),
)


@Serializable
data class PrivateMessage(
    val plid: String? = null,
    val touid: String? = null,
    val username: String,
    val avatarUrl: String? = null,
    val summary: String,
    val timeText: String? = null,
    val unread: Boolean = false,
)


@Serializable
data class ChatMessage(
    val id: String = "",
    val text: String,
    val time: String? = null,
    val isSelf: Boolean = false,
    val avatarUrl: String? = null,
    





    val contentHtml: String? = null,
    
    val sending: Boolean = false,
)


@Serializable
data class ChatConversation(
    val touid: String,
    val targetName: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val formhash: String? = null,
    
    val targetOnline: Boolean? = null,
)


@Serializable
data class FollowUserItem(
    val uid: String,
    val username: String,
    val avatarUrl: String? = null,
    val fansCount: String? = null,
    val followCount: String? = null,
    val isFollowing: Boolean = false,
    
    val remark: String? = null,
)


@Serializable
data class FollowUserPage(
    val users: List<FollowUserItem>,
    val page: Int,
    val hasNext: Boolean,
)


@Serializable
data class FriendUserItem(
    val uid: String,
    val username: String,
    val avatarUrl: String? = null,
    val description: String? = null,
)


@Serializable
data class FriendUserPage(
    val users: List<FriendUserItem>,
    val page: Int,
    val hasNext: Boolean,
)


@Serializable
data class FavoriteItem(
    val favid: String,
    val id: String,
    val title: String,
    val description: String? = null,
    val dateText: String? = null,
    val type: String = "thread", 
)


@Serializable
data class FavoritePage(
    val items: List<FavoriteItem>,
    val page: Int,
    val hasNext: Boolean,
)


@Serializable
data class MyThreadPage(
    val threads: List<Thread>,
    val page: Int,
    val hasNext: Boolean,
)


@Serializable
data class ThreadType(
    val id: String,
    val name: String,
)


data class NewThreadPageData(
    val formhash: String,
    val posttime: String? = null,
    val uploadUid: String? = null,
    val uploadHash: String? = null,
    val types: List<ThreadType> = emptyList(),
    val permissionDenied: String? = null,
)


data class NewThreadResult(
    val success: Boolean,
    val tid: String? = null,
    val message: String? = null,
)







data class EditAttachment(
    val aid: String,
    val url: String,
    val fileName: String,
)







data class EditPostPageData(
    val formhash: String,
    val subject: String? = null,
    val message: String = "",
    val attachments: List<EditAttachment> = emptyList(),
    val uploadUid: String? = null,
    val uploadHash: String? = null,
    val types: List<ThreadType> = emptyList(),
    val permissionDenied: String? = null,
)


data class EditPostResult(
    val success: Boolean,
    val message: String? = null,
)








@Serializable
data class PostDraft(
    val fid: String,
    val forumName: String = "",
    val subject: String = "",
    val message: String = "",
    val typeId: String? = null,
    val savedAt: Long = 0L,
)





data class HistoryItem(
    val tid: String,
    val title: String,
    val author: String,
    val avatarUrl: String?,
    val forumName: String?,
    val viewedAt: Long,
    val type: String = "thread",
)
