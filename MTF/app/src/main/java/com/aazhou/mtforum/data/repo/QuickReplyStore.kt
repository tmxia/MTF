package com.aazhou.mtforum.data.repo

import android.content.Context
import org.json.JSONArray







class QuickReplyStore(context: Context) {

    private val sp = context.getSharedPreferences("mtf_quick_reply", Context.MODE_PRIVATE)
    private val keyPhrases = "phrases"

    private val defaultPhrases = listOf("谢谢分享！")

    fun getPhrases(): List<String> {
        val raw = sp.getString(keyPhrases, null)
        if (raw.isNullOrBlank()) return defaultPhrases
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "").trim()
                if (s.isNotEmpty()) out.add(s)
            }
            out.ifEmpty { defaultPhrases }
        } catch (_: Exception) {
            defaultPhrases
        }
    }

    fun savePhrases(list: List<String>) {
        val arr = JSONArray()
        list.filter { it.isNotBlank() }
            .map { it.trim() }
            .distinct()
            .take(MAX)
            .forEach { arr.put(it) }
        sp.edit().putString(keyPhrases, arr.toString()).apply()
    }

    
    fun wasQuickReplyLastUsed(): Boolean = sp.getBoolean("last_used", false)
    fun setQuickReplyLastUsed(used: Boolean) {
        sp.edit().putBoolean("last_used", used).apply()
    }

    fun addPhrase(s: String) {
        val t = s.trim()
        if (t.isEmpty()) return
        val cur = getPhrases().toMutableList()
        if (cur.contains(t)) return
        cur.add(t)
        savePhrases(cur)
    }

    fun removePhrase(s: String) {
        val cur = getPhrases().toMutableList()
        cur.remove(s)
        if (cur.isEmpty()) {
            
            cur.addAll(defaultPhrases)
        }
        savePhrases(cur)
    }

    companion object {
        private const val MAX = 20
    }
}
