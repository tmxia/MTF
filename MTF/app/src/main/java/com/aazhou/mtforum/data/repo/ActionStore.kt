package com.aazhou.mtforum.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.actionDataStore by preferencesDataStore(name = "mtf_actions")








class ActionStore(private val context: Context) {

    private fun likedKey(tid: String) = booleanPreferencesKey("liked_$tid")
    private fun favoritedKey(tid: String) = booleanPreferencesKey("favorited_$tid")
    private fun followingKey(uid: String) = booleanPreferencesKey("following_$uid")

    
    suspend fun liked(tid: String): Boolean? =
        context.actionDataStore.data.map { it[likedKey(tid)] }.first()

    
    suspend fun favorited(tid: String): Boolean? =
        context.actionDataStore.data.map { it[favoritedKey(tid)] }.first()

    suspend fun setLiked(tid: String, liked: Boolean) {
        context.actionDataStore.edit { it[likedKey(tid)] = liked }
    }

    suspend fun setFavorited(tid: String, favorited: Boolean) {
        context.actionDataStore.edit { it[favoritedKey(tid)] = favorited }
    }

    
    suspend fun following(uid: String): Boolean? =
        context.actionDataStore.data.map { it[followingKey(uid)] }.first()

    suspend fun setFollowing(uid: String, following: Boolean) {
        if (uid.isBlank()) return
        context.actionDataStore.edit { it[followingKey(uid)] = following }
    }
}
