package com.megaconverter.app.converter.converters

import android.graphics.Bitmap
import android.os.Build
import com.megaconverter.app.converter.FileFormat
import java.io.OutputStream

object ImageEncoder {

    /** Encodes [bitmap] into [out] using [format]. Returns false for unsupported formats. */
    fun write(bitmap: Bitmap, format: FileFormat, out: OutputStream): Boolean {
        return when (format) {
            FileFormat.JPG -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            FileFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            FileFormat.WEBP -> {
                val webpFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(webpFormat, 92, out)
            }
            FileFormat.BMP -> {
                BmpEncoder.encode(bitmap, out)
                true
            }
            else -> false
        }
    }
}
