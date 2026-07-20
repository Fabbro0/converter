package com.megaconverter.app.converter.converters

/** Minimal RFC4180-ish CSV reader/writer: quoted fields, "" escaping, CRLF output. */
object CsvUtils {

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                c == '\r' -> {}
                c == '\n' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }

    fun format(rows: List<List<String>>): String {
        val sb = StringBuilder()
        rows.forEach { row ->
            sb.append(row.joinToString(",") { escapeField(it) })
            sb.append("\r\n")
        }
        return sb.toString()
    }

    private fun escapeField(field: String): String =
        if (field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
