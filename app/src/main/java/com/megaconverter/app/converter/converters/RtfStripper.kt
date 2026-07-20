package com.megaconverter.app.converter.converters

/**
 * Best-effort RTF -> plain text. RTF's control-word/group structure can nest
 * arbitrarily deep and a fully correct parser needs real state tracking; this uses a
 * regex pipeline instead (drop known non-text destination groups, turn \par/\line into
 * newlines, decode \'xx hex escapes, strip remaining control words) which handles
 * typical simple RTF documents well but won't perfectly reconstruct deeply nested
 * tables or embedded objects.
 */
object RtfStripper {

    private val destinationGroup = Regex(
        "\\{\\\\\\*?\\\\(fonttbl|colortbl|stylesheet|info|generator|pict|object|filetbl|listtable" +
            "|revtbl|rsidtbl|latentstyles|themedata|colorschememapping)[^{}]*(\\{[^{}]*}[^{}]*)*}",
    )
    private val hexEscape = Regex("\\\\'([0-9a-fA-F]{2})")
    private val controlWord = Regex("\\\\[a-zA-Z]+-?\\d*[ ]?")

    // Placeholders (Unicode private-use characters, won't occur in real text), used to
    // protect literal escaped braces/backslash (\{ \} \\) from the later "strip bare
    // group braces" pass.
    private const val ESCAPED_OPEN_BRACE = ""
    private const val ESCAPED_CLOSE_BRACE = ""
    private const val ESCAPED_BACKSLASH = ""

    fun toPlainText(rtf: String): String {
        var text = rtf
        repeat(3) { text = destinationGroup.replace(text, "") }
        text = text.replace("\\par", "\n").replace("\\line", "\n").replace("\\tab", "\t")
        text = hexEscape.replace(text) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
        text = controlWord.replace(text, "")
        text = text
            .replace("\\{", ESCAPED_OPEN_BRACE)
            .replace("\\}", ESCAPED_CLOSE_BRACE)
            .replace("\\\\", ESCAPED_BACKSLASH)
        text = text.replace("{", "").replace("}", "")
        text = text
            .replace(ESCAPED_OPEN_BRACE, "{")
            .replace(ESCAPED_CLOSE_BRACE, "}")
            .replace(ESCAPED_BACKSLASH, "\\")
        return text.replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
