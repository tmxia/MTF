package com.aazhou.mtforum.data.model

import kotlinx.serialization.Serializable


@Serializable
data class FriendRequestItem(
    val uid: String,
    val username: String,
    val avatarUrl: String? = null,
    val description: String? = null,
)


@Serializable
data class FriendRequestPage(
    val requests: List<FriendRequestItem>,
    val page: Int,
    val hasNext: Boolean,
    val formhash: String? = null,
)
