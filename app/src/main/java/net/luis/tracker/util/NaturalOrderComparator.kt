package net.luis.tracker.util

object NaturalOrderComparator : Comparator<String> {

	private val chunkPattern = Regex("""\d+|\D+""")

	override fun compare(a: String, b: String): Int {
		val chunksA = chunkPattern.findAll(a).map { it.value }.toList()
		val chunksB = chunkPattern.findAll(b).map { it.value }.toList()

		for (i in 0 until minOf(chunksA.size, chunksB.size)) {
			val ca = chunksA[i]
			val cb = chunksB[i]
			val isDigitA = ca[0].isDigit()
			val isDigitB = cb[0].isDigit()

			val result = when {
				isDigitA && isDigitB -> ca.toBigInteger().compareTo(cb.toBigInteger())
				else -> ca.compareTo(cb, ignoreCase = true)
			}
			if (result != 0) return result
		}
		return chunksA.size - chunksB.size
	}
}
