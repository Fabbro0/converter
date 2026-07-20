package com.megaconverter.app.converter.converters

import android.graphics.Bitmap
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android's [Bitmap.CompressFormat] has no BMP option, so a plain uncompressed
 * 24-bit BMP writer is used for image -> BMP conversions.
 */
object BmpEncoder {

    fun encode(bitmap: Bitmap, out: OutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val rowSizeBytes = (width * 3 + 3) / 4 * 4
        val pixelDataSize = rowSizeBytes * height
        val fileSize = 54 + pixelDataSize

        val header = ByteBuffer.allocate(54).order(ByteOrder.LITTLE_ENDIAN)
        header.put('B'.code.toByte())
        header.put('M'.code.toByte())
        header.putInt(fileSize)
        header.putInt(0)
        header.putInt(54)
        header.putInt(40)
        header.putInt(width)
        header.putInt(height) // positive height = bottom-up row order
        header.putShort(1)
        header.putShort(24)
        header.putInt(0)
        header.putInt(pixelDataSize)
        header.putInt(2835)
        header.putInt(2835)
        header.putInt(0)
        header.putInt(0)
        out.write(header.array())

        val row = IntArray(width)
        val rowBytes = ByteArray(rowSizeBytes)
        for (y in height - 1 downTo 0) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var idx = 0
            for (x in 0 until width) {
                val pixel = row[x]
                rowBytes[idx++] = (pixel and 0xFF).toByte()
                rowBytes[idx++] = ((pixel shr 8) and 0xFF).toByte()
                rowBytes[idx++] = ((pixel shr 16) and 0xFF).toByte()
            }
            out.write(rowBytes, 0, rowSizeBytes)
        }
    }
}
