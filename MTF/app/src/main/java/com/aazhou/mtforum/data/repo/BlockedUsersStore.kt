package com.aazhou.mtforum.data.repo

import kotlinx.coroutines.flow.StateFlow





class BlockedUsersStore(private val relationStore: RelationStore) {

    val blockedUids: StateFlow<Set<String>> = relationStore.blocked

    fun refresh() = relationStore.refreshBlocked(force = true)

    fun add(uid: String) = relationStore.addBlocked(uid)

    fun remove(uid: String) = relationStore.removeBlocked(uid)

    fun addAll(uids: Collection<String>) = relationStore.addAllBlocked(uids)
}
