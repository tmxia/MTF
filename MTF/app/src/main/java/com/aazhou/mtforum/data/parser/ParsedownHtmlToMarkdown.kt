package com.aazhou.mtforum.data.parser

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object ParsedownHtmlToMarkdown {
    fun convert(root: Element?): String = if (root == null) "" else blocksOf(root).trim()

    private fun blocksOf(el: Element?): String = el?.childNodes()?.joinToString("") { block(it) } ?: ""

    private fun block(node: Node?): String {
        if (node == null) return ""
        if (node is TextNode) {
            val text = node.text().trim()
            return if (text.isEmpty()) "" else "${text}\n\n"
        }
        if (node !is Element) return ""

        return when (val tag = node.tagName().lowercase()) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val level = tag.substring(1).toIntOrNull() ?: 1
                val text = inlinesOf(node).trim()
                if (text.isEmpty()) "" else "${"#".repeat(level)} $text\n\n"
            }
            "p" -> {
                val text = inlinesOf(node).trim()
                if (text.isEmpty()) "" else "$text\n\n"
            }
            "pre" -> codeBlock(node)
            "blockquote" -> blockquote(node)
            "ul" -> "${list(node, ordered = false)}\n"
            "ol" -> "${list(node, ordered = true)}\n"
            "hr" -> "***\n\n"
            "div" -> if (node.hasClass("tip")) "" else blocksOf(node)
            else -> {
                val text = inline(node).trim()
                if (text.isEmpty()) "" else "$text\n\n"
            }
        }
    }

    private fun codeBlock(pre: Element?): String {
        if (pre == null) return ""
        val text = pre.wholeText().replace('\u00a0', ' ').trim()
        if (text.isEmpty()) return ""
        return "```\n$text\n```\n\n"
    }

    private fun blockquote(el: Element?): String {
        if (el == null) return ""
        val inner = blocksOf(el).trim()
        if (inner.isEmpty()) return ""
        return inner.split("\n").joinToString("\n") { if (it.isEmpty()) ">" else "> $it" } + "\n\n"
    }

    private fun list(el: Element?, ordered: Boolean): String {
        if (el == null) return ""
        val buf = StringBuilder()
        var index = 0
        for (li in el.children()) {
            if (li.tagName().lowercase() != "li") continue
            index++
            val text = inline(li).replace(Regex("\\s*\\n\\s*"), " ").trim()
            buf.append("${if (ordered) "$index." else "-"} $text\n")
        }
        return buf.toString()
    }

    private fun inlinesOf(el: Element?): String = el?.childNodes()?.joinToString("") { inline(it) } ?: ""

    private fun inline(node: Node?): String {
        if (node == null) return ""
        if (node is TextNode) return node.text()
        if (node !is Element) return ""
        return when (node.tagName().lowercase()) {
            "br" -> "  \n"
            "strong", "b" -> "**${inlinesOf(node)}**"
            "em", "i" -> "*${inlinesOf(node)}*"
            "code" -> "`${node.text()}`"
            "a" -> {
                val href = node.attr("href").trim()
                val text = inlinesOf(node).trim()
                if (href.isEmpty()) text else "[$text]($href)"
            }
            "img" -> {
                val src = node.attr("src").trim()
                if (src.isEmpty()) "" else "![]($src)"
            }
            else -> inlinesOf(node)
        }
    }
}
