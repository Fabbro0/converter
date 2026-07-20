package com.megaconverter.app.converter.converters

/** HTML -> plain text: drops script/style blocks entirely, turns block-level tags into
 * line breaks, strips the rest, unescapes the common named entities. */
object HtmlStripper {

    private val scriptOrStyle = Regex("(?is)<(script|style)[^>]*>.*?</\\1>")
    private val blockClosing = Regex("(?i)</(p|div|h[1-6]|li|tr|section|article|header|footer|blockquote)>")
    private val lineBreak = Regex("(?i)<br\\s*/?>")
    private val anyTag = Regex("<[^>]+>")
    private val extraBlankLines = Regex("\n{3,}")

    fun toPlainText(html: String): String {
        var text = scriptOrStyle.replace(html, "")
        text = blockClosing.replace(text, "\n")
        text = lineBreak.replace(text, "\n")
        text = anyTag.replace(text, "")
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
        text = text.lines().joinToString("\n") { it.trim() }
        return extraBlankLines.replace(text, "\n\n").trim()
    }
}
