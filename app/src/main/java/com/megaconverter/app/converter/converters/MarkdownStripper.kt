package com.megaconverter.app.converter.converters

/** Best-effort Markdown -> plain text: strips common syntax, keeps the readable content. */
object MarkdownStripper {

    fun toPlainText(markdown: String): String {
        var text = markdown
        text = Regex("(?m)^#{1,6}\\s*").replace(text, "")
        text = Regex("```[a-zA-Z0-9]*\\n?").replace(text, "")
        text = Regex("`([^`]+)`").replace(text, "$1")
        text = Regex("\\*\\*\\*([^*]+)\\*\\*\\*").replace(text, "$1")
        text = Regex("\\*\\*([^*]+)\\*\\*").replace(text, "$1")
        text = Regex("__([^_]+)__").replace(text, "$1")
        text = Regex("\\*([^*]+)\\*").replace(text, "$1")
        text = Regex("(?<!\\w)_([^_]+)_(?!\\w)").replace(text, "$1")
        text = Regex("!\\[([^]]*)]\\(([^)]+)\\)").replace(text, "[Immagine: $1]")
        text = Regex("\\[([^]]+)]\\(([^)]+)\\)").replace(text, "$1 ($2)")
        text = Regex("(?m)^>\\s?").replace(text, "")
        text = Regex("(?m)^\\s*[-*+]\\s+").replace(text, "• ")
        text = Regex("(?m)^\\s*\\d+\\.\\s+").replace(text, "")
        text = Regex("(?m)^(-{3,}|\\*{3,}|_{3,})$").replace(text, "")
        return text.trim()
    }
}
