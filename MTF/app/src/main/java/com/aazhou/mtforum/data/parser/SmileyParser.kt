package com.aazhou.mtforum.data.parser

import kotlinx.serialization.Serializable


@Serializable
data class SmileyPack(
    val id: String,
    val name: String,
    val folder: String,
    val items: List<Smiley>,
)


@Serializable
data class Smiley(
    val code: String,
    val url: String,
)












object SmileyParser {

    
    private const val SMILEY_BASE = "https://cdn.binmt.cc/static/image/smiley/"

    private val RE_TYPE = Regex("""smilies_type\['_(\d+)'\]\s*=\s*\['([^']*)',\s*'([^']*)'\]""")
    private val RE_CHUNK = Regex("""smilies_array\[(\d+)\]\[\d+\]\s*=\s*\[""")
    private val RE_ITEM = Regex("""\['(\d+)',\s*'((?:[^'\\]|\\.)*)',\s*'([^']*)'""")

    fun parse(js: String): List<SmileyPack> {
        val folders = RE_TYPE.findAll(js).associate { it.groupValues[1] to (it.groupValues[2] to it.groupValues[3]) }
        
        val chunks = mutableMapOf<String, MutableList<Smiley>>()
        val marks = RE_CHUNK.findAll(js).toList()
        marks.forEachIndexed { index, mark ->
            val packId = mark.groupValues[1]
            val end = marks.getOrNull(index + 1)?.range?.first ?: js.length
            val body = js.substring(mark.range.last + 1, end)
            val items = chunks.getOrPut(packId) { mutableListOf() }
            RE_ITEM.findAll(body).forEach { m ->
                val file = m.groupValues[3]
                val folder = folders[packId]?.second ?: return@forEach
                items += Smiley(code = m.groupValues[2], url = "$SMILEY_BASE$folder/$file")
            }
        }
        return folders.mapNotNull { (id, nameAndFolder) ->
            val items = chunks[id]?.distinctBy { it.code }?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            SmileyPack(id = id, name = nameAndFolder.first, folder = nameAndFolder.second, items = items)
        }
    }
}
