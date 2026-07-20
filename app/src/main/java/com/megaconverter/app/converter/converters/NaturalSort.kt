package com.megaconverter.app.converter.converters

/** Sorts "page2.jpg" before "page10.jpg" (plain string sort would put page10 first). */
object NaturalSort {
    private val chunkPattern = Regex("\\d+|\\D+")

    val comparator: Comparator<String> = Comparator { a, b ->
        val partsA = chunkPattern.findAll(a).map { it.value }.toList()
        val partsB = chunkPattern.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(partsA.size, partsB.size)) {
            val pa = partsA[i]
            val pb = partsB[i]
            val numA = pa.toLongOrNull()
            val numB = pb.toLongOrNull()
            val cmp = if (numA != null && numB != null) numA.compareTo(numB) else pa.compareTo(pb)
            if (cmp != 0) return@Comparator cmp
        }
        partsA.size.compareTo(partsB.size)
    }
}
