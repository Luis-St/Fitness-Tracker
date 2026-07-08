package net.luis.tracker.util

/**
 * Parses a user- or locale-formatted weight string into a [Double], accepting both
 * '.' and ',' as the decimal separator. Returns null if the string is not a valid number.
 *
 * This is needed because [String.toDoubleOrNull] only accepts '.', while locale-aware
 * formatting (e.g. German) and manual entry may produce ','.
 */
fun String.toWeightDoubleOrNull(): Double? = replace(',', '.').toDoubleOrNull()

/**
 * Regex matching a (possibly partial) decimal weight being typed: an optional integer part,
 * an optional single decimal separator ('.' or ','), and an optional fractional part.
 * Allows intermediate states such as "12," or "." while the user is still typing.
 */
private val weightInputRegex = Regex("""^\d*[.,]?\d*$""")

/**
 * Whether [input] is acceptable as in-progress text in a weight field. Empty is allowed so
 * the field can be cleared; otherwise the text must match a (partial) decimal number.
 */
fun isAcceptableWeightInput(input: String): Boolean =
	input.isEmpty() || weightInputRegex.matches(input)
