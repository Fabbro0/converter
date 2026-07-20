package com.megaconverter.app.converter

enum class FormatCategory(val label: String) {
    IMAGE("Immagine"),
    DOCUMENT("Documento"),
    SPREADSHEET("Foglio di calcolo"),
    ARCHIVE("Archivio"),
    AUDIO("Audio"),
    VIDEO("Video"),
}

enum class FileFormat(
    val extension: String,
    val mimeType: String,
    val category: FormatCategory,
) {
    // Images (encodable)
    JPG("jpg", "image/jpeg", FormatCategory.IMAGE),
    PNG("png", "image/png", FormatCategory.IMAGE),
    WEBP("webp", "image/webp", FormatCategory.IMAGE),
    BMP("bmp", "image/bmp", FormatCategory.IMAGE),

    // Images (Android can decode these but has no public encoder for them, so they can
    // only ever be a conversion *source*; see ImageToImageConverter.DECODE_ONLY_FORMATS)
    HEIC("heic", "image/heic", FormatCategory.IMAGE),
    GIF("gif", "image/gif", FormatCategory.IMAGE),

    // Documents
    TXT("txt", "text/plain", FormatCategory.DOCUMENT),
    PDF("pdf", "application/pdf", FormatCategory.DOCUMENT),
    DOCX(
        "docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        FormatCategory.DOCUMENT,
    ),
    EPUB("epub", "application/epub+zip", FormatCategory.DOCUMENT),
    CBZ("cbz", "application/vnd.comicbook+zip", FormatCategory.DOCUMENT),
    MD("md", "text/markdown", FormatCategory.DOCUMENT),
    HTML("html", "text/html", FormatCategory.DOCUMENT),
    RTF("rtf", "application/rtf", FormatCategory.DOCUMENT),
    PPTX(
        "pptx",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        FormatCategory.DOCUMENT,
    ),

    // Spreadsheets
    CSV("csv", "text/csv", FormatCategory.SPREADSHEET),
    XLSX(
        "xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        FormatCategory.SPREADSHEET,
    ),

    // Archives
    ZIP("zip", "application/zip", FormatCategory.ARCHIVE),

    // Audio
    MP3("mp3", "audio/mpeg", FormatCategory.AUDIO),
    WAV("wav", "audio/wav", FormatCategory.AUDIO),
    AAC("aac", "audio/aac", FormatCategory.AUDIO),
    M4A("m4a", "audio/mp4", FormatCategory.AUDIO),
    FLAC("flac", "audio/flac", FormatCategory.AUDIO),
    OGG("ogg", "audio/ogg", FormatCategory.AUDIO),

    // Video
    MP4("mp4", "video/mp4", FormatCategory.VIDEO),
    MKV("mkv", "video/x-matroska", FormatCategory.VIDEO),
    AVI("avi", "video/x-msvideo", FormatCategory.VIDEO),
    WEBM("webm", "video/webm", FormatCategory.VIDEO),
    MOV("mov", "video/quicktime", FormatCategory.VIDEO),
    ;

    companion object {
        fun fromExtension(extension: String): FileFormat? {
            val normalized = extension.trim().trimStart('.').lowercase()
            if (normalized == "jpeg") return JPG
            if (normalized == "htm") return HTML
            return entries.firstOrNull { it.extension == normalized }
        }

        fun fromMimeType(mimeType: String?): FileFormat? {
            if (mimeType.isNullOrBlank()) return null
            if (mimeType.equals("image/jpg", ignoreCase = true)) return JPG
            return entries.firstOrNull { it.mimeType.equals(mimeType, ignoreCase = true) }
        }
    }
}
