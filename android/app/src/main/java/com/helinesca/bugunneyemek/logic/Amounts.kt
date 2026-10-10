package com.helinesca.bugunneyemek.logic

import com.helinesca.bugunneyemek.data.IngredientLine
import kotlin.math.floor
import kotlin.math.roundToInt

/** Turns a recipe line into what the user reads, e.g. "1 ½ su bardağı" and "300 ml". */
object Amounts {
    val VOLUME_ML = mapOf(
        "çay kaşığı" to 5.0, "tatlı kaşığı" to 10.0, "yemek kaşığı" to 15.0,
        "çay bardağı" to 100.0, "su bardağı" to 200.0,
    )
    val COUNT_UNITS = setOf("adet", "diş", "sap", "dilim", "kare", "kutu", "tablet", "küp", "paket", "demet")

    private val GLYPHS = mapOf(
        1 to "⅛", 2 to "¼", 3 to "⅜", 4 to "½", 5 to "⅝", 6 to "¾", 7 to "⅞",
    )

    fun parse(a: String): Double =
        if ('/' in a) a.split('/').let { (n, d) -> n.toDouble() / d.toDouble() }
        else a.replace(',', '.').toDouble()

    private fun roundTo(x: Double, step: Double) = (x / step).roundToInt() * step

    /** 1.5 -> "1 ½", 0.25 -> "¼", 2.0 -> "2" (eighths only). */
    fun fraction(x: Double): String {
        val eighths = (x * 8).roundToInt()
        val whole = eighths / 8
        val rest = eighths % 8
        return when {
            rest == 0 -> "$whole"
            whole == 0 -> GLYPHS.getValue(rest)
            else -> "$whole ${GLYPHS.getValue(rest)}"
        }
    }

    /** The kitchen amount, rounded to what can really be measured. */
    fun quantity(line: IngredientLine, mult: Int): Double {
        val q = parse(line.amount) * mult
        return when {
            line.unit in VOLUME_ML -> roundTo(q, 0.25).coerceAtLeast(0.25)
            line.unit == "paket" && line.name == "Kalıp margarin" -> roundTo(q, 0.125).coerceAtLeast(0.125)
            line.unit in COUNT_UNITS && line.name in Rules.HALF_OK -> roundTo(q, 0.5).coerceAtLeast(0.5)
            line.unit in COUNT_UNITS -> floor(q + 0.5).coerceAtLeast(1.0)   // whole: a half rounds up, never below 1
            else -> roundGrams(q)
        }
    }

    private fun roundGrams(g: Double): Double = when {
        g >= 100 -> roundTo(g, 10.0)
        g >= 10 -> roundTo(g, 5.0)
        else -> g.roundToInt().coerceAtLeast(1).toDouble()
    }

    /** Left column: "2 su bardağı", "3 adet", "250 g". */
    fun kitchen(line: IngredientLine, mult: Int): String {
        val q = quantity(line, mult)
        return if (line.unit == "g" || line.unit == "ml") "${q.roundToInt()} ${line.unit}"
        else "${fraction(q)} ${line.unit}"
    }

    /** Right column: "300 ml", "~250 g", or "" when the left column is already metric. */
    fun metric(line: IngredientLine, mult: Int): String {
        if (line.unit == "g" || line.unit == "ml") return ""
        val q = quantity(line, mult)
        val ml = VOLUME_ML[line.unit]
        if (ml != null && (line.pantryId == null || line.name in Rules.LIQUIDS)) {
            return "${roundGrams(q * ml).roundToInt()} ml"
        }
        val perUnit = line.grams / parse(line.amount)
        val grams = roundGrams(perUnit * q).roundToInt()
        return if (line.unit in COUNT_UNITS) "~$grams g" else "$grams g"
    }
}
