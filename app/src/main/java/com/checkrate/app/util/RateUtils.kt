package com.checkrate.app.util

import java.text.DecimalFormat

object RateUtils {
    const val GRAMS_PER_TROY_OUNCE = 31.1034768
    const val GOLD_22K_FACTOR = 22.0 / 24.0
    private val metalCodes = setOf("XAU", "XAG", "XAU22")
    private fun displayName(code: String): String = when (code) {
        "XAU" -> "Gold"
        "XAG" -> "Silver"
        "XAU22" -> "Gold 22K"
        else -> code
    }

    fun isMetal(code: String): Boolean = metalCodes.contains(code)

    fun usdOunceToBasePerGram(usdPerOunce: Double, usdToBase: Double): Double {
        return (usdPerOunce * usdToBase) / GRAMS_PER_TROY_OUNCE
    }

    fun normalizeRatePerGram(code: String, ratePerGram: Double): Double {
        return if (isMetal(code)) ratePerGram else ratePerGram
    }

    fun buildSummary(base: String, rates: Map<String, Double>, codes: Set<String>): String {
        val format = DecimalFormat("0.####")
        if (codes.isEmpty()) return "No tracked items"
        return codes.toSortedSet().joinToString(", ") { code ->
            val value = rates[code]
            if (value == null) "${displayName(code)} n/a" else "${displayName(code)} ${format.format(value)}"
        } + " in $base"
    }

    fun buildMetalSummary(base: String, rates: Map<String, Double>, codes: Set<String>): String {
        val format = DecimalFormat("0.####")
        if (codes.isEmpty()) return "No metals"
        return codes.toSortedSet().joinToString(", ") { code ->
            val value = rates[code]
            if (value == null) "${displayName(code)} n/a" else "${displayName(code)} ${format.format(value)}/g"
        } + " in $base"
    }
}
