package com.megaconverter.app.converter

enum class FormatCategory(val label: String) {
    IMAGE("Immagine"),
    DOCUMENT("Documento"),
    AUDIO("Audio"),
    VIDEO("Video"),
}

enum class FileFormat(
    val extension: String,
    val mimeType: String,
    val category: FormatCategory,
) {
    // Images
    JPG("jpg", "image/jpeg", FormatCategory.IMAGE),
    PNG("png", "image/png", FormatCategory.IMAGE),
    WEBP("webp", "image/webp", FormatCategory.IMAGE),
    BMP("bmp", "image/bmp", FormatCategory.IMAGE),

    // Documents
    TXT("txt", "text/plain", FormatCategory.DOCUMENT),
    PDF("pdf", "application/pdf", FormatCategory.DOCUMENT),
    DOCX(
        "docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        FormatCategory.DOCUMENT,
    ),
    EPUB("epub", "application/epub+zip", FormatCategory.DOCUMENT),

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
            return entries.firstOrNull { it.extension == normalized }
        }

        fun fromMimeType(mimeType: String?): FileFormat? {
            if (mimeType.isNullOrBlank()) return null
            return entries.firstOrNull { it.mimeType.equals(mimeType, ignoreCase = true) }
        }
    }
}
