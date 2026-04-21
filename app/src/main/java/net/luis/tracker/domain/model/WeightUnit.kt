package net.luis.tracker.domain.model

enum class WeightUnit {
	KG, LBS;

	fun convertFromKg(kg: Double): Double = when (this) {
		KG -> kg
		LBS -> kg * 2.20462
	}

	fun convertToKg(value: Double): Double = when (this) {
		KG -> value
		LBS -> value / 2.20462
	}

	fun formatWeight(kg: Double): String {
		val converted = convertFromKg(kg)
		return if (converted == converted.toLong().toDouble()) {
			"${converted.toLong()} ${name.lowercase()}"
		} else {
			"${"%.1f".format(converted)} ${name.lowercase()}"
		}
	}

	fun formatWeightLarge(kg: Double): String {
		return if (kg >= 1_000.0) {
			"${"%.1f".format(kg / 1_000.0)} t"
		} else {
			formatWeight(kg)
		}
	}
}
