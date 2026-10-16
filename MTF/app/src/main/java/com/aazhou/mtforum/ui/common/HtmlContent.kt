package com.aazhou.mtforum.ui.common

import com.aazhou.mtforum.data.parser.ForumParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Comment


private const val SMILEY_CHAR_BASE = 0xE000










sealed interface ContentBlock {
    data class Quote(val text: String) : ContentBlock

    






    data class Hidden(val label: String, val blocks: List<ContentBlock>) : ContentBlock
    data class Text(
        val text: String,
        val smiles: Map<Char, String> = emptyMap(),
        val spans: List<StyleSpan> = emptyList(),
    ) : ContentBlock
    data class Image(val url: String) : ContentBlock

    



    data class Video(val url: String, val poster: String?) : ContentBlock

    
    data class ImageGroup(val urls: List<String>) : ContentBlock

    
    data class Table(
        val header: List<String>?,
        val rows: List<List<String>>,
    ) : ContentBlock

    





    data class CodeBlock(val lines: List<String>) : ContentBlock

    



    data class Attachment(
        val fileName: String,
        val url: String,
        val sizeText: String?,
    ) : ContentBlock
}




fun collectImageUrls(html: String): List<String> = collectImageUrls(parseContentBlocks(html))

private fun collectImageUrls(blocks: List<ContentBlock>): List<String> =
    blocks.flatMap { block ->
        when (block) {
            is ContentBlock.Image -> listOf(block.url)
            is ContentBlock.ImageGroup -> block.urls
            is ContentBlock.Hidden -> collectImageUrls(block.blocks)
            else -> emptyList()
        }
    }


fun plainTextOf(html: String): String = plainTextOf(parseContentBlocks(html))

private fun plainTextOf(blocks: List<ContentBlock>): String =
    blocks
        .mapNotNull { block ->
            when (block) {
                is ContentBlock.Text -> block.text.takeIf { it.isNotBlank() }
                is ContentBlock.Quote -> block.text.takeIf { it.isNotBlank() }
                is ContentBlock.CodeBlock -> block.lines.joinToString("\n").takeIf { it.isNotBlank() }
                is ContentBlock.Hidden -> plainTextOf(block.blocks).takeIf { it.isNotBlank() }
                else -> null
            }
        }
        .joinToString("\n")
        .trim()


enum class CellAlign { Start, Center, End }







data class StyleSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val code: Boolean = false,
    val headingLevel: Int = 0,
    val fontSizeScale: Float = 1f,
    val align: CellAlign? = null,
    val linkUrl: String? = null,
)


private data class InlineStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val code: Boolean = false,
    val headingLevel: Int = 0,
    val fontSizeScale: Float = 1f,
    val align: CellAlign? = null,
    val linkUrl: String? = null,
)


private class OpenSpan(val style: InlineStyle, val start: Int, val linkUrl: String?)







fun parseContentBlocks(html: String): List<ContentBlock> {
    if (html.isBlank()) return emptyList()
    
    
    synchronized(contentBlocksCache) {
        contentBlocksCache[html]?.let { return it }
    }
    val blocks = parseContentBlocksUncached(html)
    synchronized(contentBlocksCache) {
        contentBlocksCache[html] = blocks
    }
    return blocks
}


private const val CONTENT_CACHE_MAX_ENTRIES = 128


private val contentBlocksCache = object : LinkedHashMap<String, List<ContentBlock>>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<String, List<ContentBlock>>): Boolean =
        size > CONTENT_CACHE_MAX_ENTRIES
}

private fun parseContentBlocksUncached(html: String): List<ContentBlock> {
    
    val source = hoistQuoteImages(html)
    val body = Jsoup.parseBodyFragment(collapseBlankBlocks(stripEdgeWhitespace(source))).body()
    val builder = BlocksBuilder()
    builder.appendNodes(body.childNodes())
    return groupImages(dropEdgeBlankBlocks(builder.build()))
}








private fun hoistQuoteImages(html: String): String {
    if ("<img" !in html.lowercase()) return html
    val doc = Jsoup.parseBodyFragment(html)
    val quotes = doc.select("div.comiis_quote, div.locked")
    if (quotes.isEmpty()) return html
    for (quote in quotes) {
        if (hiddenLabelElement(quote) != null) continue
        val images = quote.select("img")
        if (images.isEmpty()) continue
        for (img in images) {
            quote.before(img.outerHtml())
            img.remove()
        }
    }
    return doc.body().html()
}





private fun hiddenLabelElement(element: Element): Element? =
    element.children().firstOrNull { it.tagName() == "h2" && it.hasClass("f_a") }







private fun stripEdgeWhitespace(html: String): String = html
    .replace(Regex("^(?:\\s|&nbsp;|&#160;|<br\\s*/?>|\\u00A0)+", RegexOption.IGNORE_CASE), "")
    .replace(Regex("(?:\\s|&nbsp;|&#160;|<br\\s*/?>|\\u00A0)+$", RegexOption.IGNORE_CASE), "")







private fun groupImages(blocks: List<ContentBlock>): List<ContentBlock> {
    if (blocks.none { it is ContentBlock.Image }) return blocks
    val result = mutableListOf<ContentBlock>()
    var index = 0
    while (index < blocks.size) {
        if (blocks[index] !is ContentBlock.Image) {
            result.add(blocks[index])
            index++
            continue
        }
        val images = mutableListOf<String>()
        var end = index
        while (end < blocks.size && (blocks[end] is ContentBlock.Image || isBlankTextBlock(blocks[end]))) {
            (blocks[end] as? ContentBlock.Image)?.let { images.add(it.url) }
            end++
        }
        if (images.size >= 2) {
            result.add(ContentBlock.ImageGroup(images))
        } else {
            for (i in index until end) result.add(blocks[i])
        }
        index = end
    }
    return result
}

private fun isBlankTextBlock(block: ContentBlock): Boolean =
    block is ContentBlock.Text && isBlankOrNbsp(block.text)


private fun dropEdgeBlankBlocks(blocks: List<ContentBlock>): List<ContentBlock> {
    fun isBlankBlock(block: ContentBlock) = block is ContentBlock.Text && isBlankOrNbsp(block.text)
    val start = blocks.indexOfFirst { !isBlankBlock(it) }
    if (start < 0) return emptyList()
    val end = blocks.indexOfLast { !isBlankBlock(it) }
    return blocks.subList(start, end + 1)
}








private val BLOCK_TAGS = setOf(
    "address", "article", "aside", "blockquote", "center", "dd", "details", "dialog", "div",
    "dl", "dt", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4",
    "h5", "h6", "header", "hr", "li", "main", "nav", "ol", "p", "pre", "section", "summary",
    "table", "td", "th", "tr", "ul",
)









private fun collapseBlankBlocks(html: String): String = html
    .replace(Regex("(?i)(?:<br\\s*/?>\\s*){3,}"), "<br><br>")
    .replace(Regex("(?i)<p\\s*>\\s*(?:&nbsp;|&#160;|\\s)*</p>"), "")
    .replace(Regex("(?i)<div[^>]*>\\s*(?:&nbsp;|&#160;|<br\\s*/?>|\\s)*</div>"), "")

private class BlocksBuilder {
    private val blocks = mutableListOf<ContentBlock>()
    private val text = StringBuilder()
    private val smiles = LinkedHashMap<String, Char>()
    private val spans = mutableListOf<StyleSpan>()
    private val openSpans = ArrayDeque<OpenSpan>()

    fun build(): List<ContentBlock> {
        flushText()
        return blocks
    }

    fun appendNodes(nodes: List<Node>, style: InlineStyle = InlineStyle()) {
        for (node in nodes) {
            when (node) {
                is Comment -> Unit
                is TextNode -> appendText(node.text(), style)
                is Element -> appendElement(node, style)
            }
        }
    }

    private fun appendText(value: String, style: InlineStyle) {
        if (value.isEmpty()) return
        
        
        val body = eatLineStartWhitespace(value)
        if (body.isEmpty()) return
        val start = text.length
        text.append(body)
        syncSpans(style, start, text.length)
    }

    
    private fun eatLineStartWhitespace(value: String): String {
        if (value.isEmpty()) return value
        val out = StringBuilder(value.length)
        var atLineStart = text.isEmpty() || text[text.length - 1] == '\n'
        for (c in value) {
            when {
                c == '\n' -> {
                    out.append(c)
                    atLineStart = true
                }
                atLineStart && (c.isWhitespace() || c == '\u00A0') -> Unit
                else -> {
                    out.append(c)
                    atLineStart = false
                }
            }
        }
        return out.toString()
    }

    
    private fun appendBreak(style: InlineStyle) {
        val cursor = text.length
        if (cursor >= 2 && text[cursor - 1] == '\n' && text[cursor - 2] == '\n') return
        appendText("\n", style)
    }

    





    private fun appendBlockBreak(style: InlineStyle) {
        if (text.isEmpty() || text[text.length - 1] == '\n') return
        appendText("\n", style)
    }

    private fun appendSmiley(url: String, style: InlineStyle) {
        val placeholder = smiles.getOrPut(url) {
            (SMILEY_CHAR_BASE + smiles.size).toChar()
        }
        val start = text.length
        text.append(placeholder)
        syncSpans(style, start, text.length)
    }

    



    private fun syncSpans(style: InlineStyle, start: Int, end: Int) {
        var depth = 0
        while (depth < openSpans.size && openSpans[depth].style == style) depth++
        while (openSpans.size > depth) {
            closeSpan(openSpans.size - 1, start)
        }
        if (depth == openSpans.size && style != InlineStyle()) {
            openSpans.add(OpenSpan(style, start, style.linkUrl))
        }
    }

    private fun closeSpan(index: Int, end: Int) {
        val open = openSpans.removeAt(index)
        if (end > open.start) {
            spans.add(
                StyleSpan(
                    start = open.start,
                    end = end,
                    bold = open.style.bold,
                    italic = open.style.italic,
                    underline = open.style.underline,
                    strikethrough = open.style.strikethrough,
                    code = open.style.code,
                    headingLevel = open.style.headingLevel,
                    fontSizeScale = open.style.fontSizeScale,
                    align = open.style.align,
                    linkUrl = open.linkUrl,
                ),
            )
        }
    }

    private fun flushText() {
        val cursor = text.length
        while (openSpans.isNotEmpty()) closeSpan(openSpans.size - 1, cursor)
        if (isBlankOrNbsp(text)) {
            text.clear()
            smiles.clear()
            spans.clear()
            return
        }
        val raw = text.toString()
        val lead = raw.indexOfFirst { it.code != 0xA0 && !it.isWhitespace() }
        val tail = raw.indexOfLast { it.code != 0xA0 && !it.isWhitespace() }
        
        
        val start = if (lead < 0) 0 else lead
        val end = if (tail < 0) raw.length else tail + 1
        
        detectBareLinks(raw, start, end, spans)
        blocks.add(
            ContentBlock.Text(
                text = raw.substring(start, end),
                smiles = smiles.entries.associate { (url, placeholder) -> placeholder to url },
                spans = normalizeSpans(spans, start, end),
            ),
        )
        text.clear()
        smiles.clear()
        spans.clear()
    }









private fun quoteBodyText(element: Element): String {
    val lines = element.html()
        .split(ForumParser.RE_QUOTE_LINE_BREAK)
        .map { Jsoup.parseBodyFragment(it).body().text().trim() }
        .filter { it.isNotEmpty() }
    val metaAt = lines.indexOfFirst { it.contains("发表于") }
    if (metaAt < 0) return element.text().trim()
    return lines.drop(metaAt + 1).joinToString("\n").trim()
}
    private fun appendElement(element: Element, style: InlineStyle) {
        if (element.hasClass("comiis_quote")) {
            flushText()
            val labelEl = hiddenLabelElement(element)
            if (labelEl != null) {
                
                val inner = BlocksBuilder()
                inner.appendNodes(element.childNodes().filter { it !== labelEl })
                val hidden = dropEdgeBlankBlocks(groupImages(inner.build()))
                if (hidden.isNotEmpty()) {
                    blocks.add(ContentBlock.Hidden(label = labelEl.text().trim(), blocks = hidden))
                }
                return
            }
            val quote = quoteBodyText(element)
            if (quote.isNotEmpty()) blocks.add(ContentBlock.Quote(quote))
            return
        }
        
        
        if (element.hasClass("comiis_attach")) {
            flushText()
            parseAttachment(element)?.let { blocks.add(it) }
            return
        }
        if (element.hasClass("comiis_blockcode") || element.tagName() == "pre") {
            flushText()
            codeLinesOf(element)?.let { blocks.add(ContentBlock.CodeBlock(it)) }
            return
        }
        
        
        
        val blockBoundary = element.tagName() in BLOCK_TAGS
        if (blockBoundary) appendBlockBreak(style)
        when (element.tagName()) {
            "script", "style" -> Unit
            "video", "embed", "object" -> appendVideoElement(element, style)
            "img" -> appendImage(element, style)
            "br" -> appendBreak(style)
            "table" -> {
                flushText()
                buildTable(element)?.let { blocks.add(it) }
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val level = element.tagName().substring(1).toInt()
                appendNodes(element.childNodes().toList(), style.copy(headingLevel = level))
            }
            "a" -> appendAnchor(element, style)
            "ul", "ol" ->
                
                
                if (element.select("img, video, embed, object").isNotEmpty()) appendContainer(element, style)
                else appendList(element, style, ordered = element.tagName() == "ol")
            "blockquote" -> {
                flushText()
                val quote = quoteBodyText(element)
                if (quote.isNotEmpty()) blocks.add(ContentBlock.Quote(quote))
            }
            else -> appendContainer(element, style)
        }
        if (blockBoundary) appendBlockBreak(style)
    }

    





    private fun codeLinesOf(element: Element): List<String>? {
        val items = element.select("li")
        val raw = if (items.isNotEmpty()) {
            items.map { li -> decodedTextOf(li) }
        } else {
            decodedTextOf(element).split('\n')
        }
        val lines = raw.map { it.replace('\u00A0', ' ').trimEnd() }
        return lines.takeIf { candidates -> candidates.any { it.isNotBlank() } }
    }

    
    private fun decodedTextOf(element: Element): String {
        val out = StringBuilder()
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> out.append(node.wholeText)
                is Element ->
                    if (node.tagName() == "br") out.append('\n') else out.append(decodedTextOf(node))
                else -> Unit
            }
        }
        return out.toString()
    }

    
    private fun appendContainer(element: Element, style: InlineStyle) {
        val childStyle = when (element.tagName()) {
            "strong", "b" -> style.copy(bold = true)
            "em", "i" -> style.copy(italic = true)
            "u" -> style.copy(underline = true)
            "s", "strike", "del" -> style.copy(strikethrough = true)
            "code", "kbd", "samp" -> style.copy(code = true)
            
            
            "font" -> style.copy(fontSizeScale = fontSizeScale(element.attr("size")) ?: style.fontSizeScale)
            else -> style.copy(align = parseAlign(element) ?: style.align)
        }
        appendNodes(element.childNodes().toList(), childStyle)
    }

    private fun appendAnchor(element: Element, style: InlineStyle) {
        val img = element.selectFirst("img")
        val href = element.attr("href").trim()
        
        
        if (img != null && isVideoUrl(href)) {
            appendVideo(url = normalizeMediaUrl(href), poster = imageUrl(img), style = style)
            return
        }
        if (img != null) {
            appendImage(img, style)
            return
        }
        
        if (isVideoUrl(href)) {
            appendVideo(url = normalizeMediaUrl(href), poster = null, style = style)
            return
        }
        val link = href.takeIf { it.isNotBlank() && !it.startsWith("javascript:", true) }
        appendNodes(element.childNodes().toList(), style.copy(linkUrl = link ?: style.linkUrl))
    }

    



    private fun appendVideoElement(element: Element, style: InlineStyle) {
        val candidates = listOf(element.attr("src"), element.attr("data-src")) +
            element.select("source").map { it.attr("src") } +
            element.select("img").flatMap { listOf(it.attr("file"), it.attr("zoomfile")) }
        val url = candidates.firstOrNull { it.isNotBlank() && isVideoUrl(it) }
            ?: return
        val posterImage = element.selectFirst("img")
        val poster = posterImage?.let { imageUrl(it) } ?: element.attr("poster").takeIf { it.isNotBlank() }
        appendVideo(url = normalizeMediaUrl(url), poster = poster?.let { normalizeMediaUrl(it) }, style = style)
    }

    private fun appendVideo(url: String?, poster: String?, style: InlineStyle) {
        if (url == null) return
        flushText()
        blocks.add(ContentBlock.Video(url = url, poster = poster))
    }

    private fun appendList(element: Element, style: InlineStyle, ordered: Boolean) {
        val items = element.children().filter { it.tagName() == "li" }
        if (items.isEmpty()) {
            appendNodes(element.childNodes().toList(), style)
            return
        }
        flushText()
        val buffer = StringBuilder()
        items.forEachIndexed { index, li ->
            if (index > 0) buffer.append('\n')
            buffer.append(if (ordered) "${index + 1}. " else "· ")
            buffer.append(li.text())
        }
        buffer.append('\n')
        appendText(buffer.toString(), style)
    }

    private fun appendImage(img: Element, style: InlineStyle) {
        val url = imageUrl(img) ?: return
        if (isSmiley(img)) {
            appendSmiley(url, style)
        } else {
            flushText()
            blocks.add(ContentBlock.Image(url))
        }
    }

    



    private fun parseAttachment(element: Element): ContentBlock.Attachment? {
        val link = element.selectFirst("a[href]") ?: return null
        val url = link.attr("href").trim()
        val fileName = link.selectFirst("span.f_ok")?.text()?.trim().orEmpty()
        if (url.isEmpty() || fileName.isEmpty()) return null
        return ContentBlock.Attachment(
            fileName = fileName,
            url = normalizeMediaUrl(url),
            sizeText = link.selectFirst("p.attach_size")?.text()?.trim()?.ifBlank { null },
        )
    }

    private fun buildTable(element: Element): ContentBlock.Table? {
        val trs = element.select("tr")
        val rows = trs.map { tr ->
            tr.children().filter { it.tagName() == "td" || it.tagName() == "th" }.map { it.text().trim() }
        }.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return null
        
        val firstTr = trs.firstOrNull()
        val firstIsHeader = firstTr != null && (
            firstTr.children().any { it.tagName() == "th" } ||
                firstTr.children().filter { it.tagName() == "td" }
                    .let { it.isNotEmpty() && it.all { cell -> cell.selectFirst("strong, b") != null } }
            )
        val width = rows.maxOf { it.size }
        fun pad(row: List<String>) = if (row.size < width) row + List(width - row.size) { "" } else row
        val header = if (firstIsHeader) pad(rows.first()) else null
        val body = (if (firstIsHeader) rows.drop(1) else rows).map(::pad)
        return ContentBlock.Table(header = header, rows = body)
    }

}





private fun normalizeSpans(spans: List<StyleSpan>, start: Int, end: Int): List<StyleSpan> {
    if (spans.isEmpty()) return emptyList()
    val result = mutableListOf<StyleSpan>()
    for (span in spans) {
        val s = span.start.coerceAtLeast(start)
        val e = span.end.coerceAtMost(end)
        if (e <= s) continue
        val moved = span.copy(start = s - start, end = e - start)
        val last = result.lastOrNull()
        if (last != null && sameStyle(last, moved) && last.end == moved.start) {
            result[result.size - 1] = last.copy(end = moved.end)
        } else {
            result.add(moved)
        }
    }
    return result
}

private fun sameStyle(a: StyleSpan, b: StyleSpan): Boolean = a.copy(start = 0, end = 0) == b.copy(start = 0, end = 0)


private val BARE_URL = Regex("https?://[^\\s\\u00A0\\u3000\\uE000-\\uF8FF<>\"']+")


private const val URL_TAIL_CHARS = ".,;:!?，。；、！？…～~）)】]〉》\"'”’"






private fun detectBareLinks(raw: String, start: Int, end: Int, spans: MutableList<StyleSpan>) {
    for (match in BARE_URL.findAll(raw)) {
        val url = match.value.trimEnd(*URL_TAIL_CHARS.toCharArray())
        val from = match.range.first
        val to = from + url.length
        if (to - from <= "https://".length) continue
        if (from < start || to > end) continue
        if (spans.any { it.linkUrl != null && it.start < to && from < it.end }) continue
        spans.add(StyleSpan(start = from, end = to, linkUrl = url))
    }
}


private fun parseAlign(element: Element): CellAlign? {
    val attr = element.attr("align").trim().lowercase()
    val css = Regex("text-align\\s*:\\s*(\\w+)", RegexOption.IGNORE_CASE)
        .find(element.attr("style"))?.groupValues?.get(1)?.lowercase()
        .orEmpty()
    return when (attr.ifEmpty { css }) {
        "center" -> CellAlign.Center
        "right" -> CellAlign.End
        "left", "start" -> CellAlign.Start
        else -> null
    }
}

private fun fontSizeScale(value: String): Float? = when (value.trim().trimStart('+')) {
    "1" -> 0.75f
    "2" -> 0.85f
    "3" -> 1f
    "4" -> 1.15f
    "5" -> 1.3f
    "6" -> 1.45f
    "7" -> 1.6f
    else -> null
}


private fun isBlankOrNbsp(s: CharSequence): Boolean {
    for (c in s) {
        if (c.code != 0xA0 && !c.isWhitespace()) return false
    }
    return true
}







internal fun isSmiley(img: Element): Boolean =
    img.hasAttr("smilieid") || img.attr("src").contains("/smiley/")

internal fun imageUrl(img: Element): String? {
    
    val raw = listOf(
        "zoomfile", "file", "comiis_loadimages", "data-original", "data-src",
        "data-file", "data-lazy-src", "src",
    )
        .map { img.attr(it) }
        .firstOrNull { it.isNotBlank() && !isPlaceholderImage(it) && !isFileTypeIcon(it) }
        ?: return null
    return normalizeMediaUrl(raw)
}








private fun upgradeToHttps(url: String): String {
    if (!url.startsWith("http://", ignoreCase = true)) return url
    return "https://" + url.substring("http://".length)
}


private fun isPlaceholderImage(url: String): Boolean {
    if (url.startsWith("data:")) return true
    val lower = url.lowercase()
    return "none.gif" in lower || "none.png" in lower ||
        "blank.gif" in lower || "imageloading" in lower
}


private fun isFileTypeIcon(url: String): Boolean =
    "/static/image/filetype/" in url.lowercase()







private fun isVideoUrl(value: String): Boolean {
    val path = value.substringBefore('?').substringBefore('#').lowercase()
    return VIDEO_EXTENSIONS.any { path.endsWith(".$it") }
}

private val VIDEO_EXTENSIONS = listOf("mp4", "mov", "m4v", "webm", "m3u8", "mkv")


internal fun normalizeMediaUrl(raw: String): String {
    
    
    if (raw.startsWith("file:", ignoreCase = true) ||
        raw.startsWith("content:", ignoreCase = true) ||
        raw.startsWith("data:", ignoreCase = true)
    ) {
        return raw
    }
    val absolute = when {
        raw.startsWith("http") -> raw
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("/") -> "https://bbs.binmt.cc$raw"
        else -> "https://bbs.binmt.cc/$raw"
    }
    return upgradeToHttps(absolute)
}
