package com.helinesca.bugunneyemek.logic

import com.helinesca.bugunneyemek.data.Catalog
import com.helinesca.bugunneyemek.data.Dish
import com.helinesca.bugunneyemek.data.UserState
import java.time.LocalDate

/** Fixed rules from the README ("Decided defaults" and "Ne lazım" sections). */
object Rules {
    const val MEMORY_DAYS = 12
    const val CHILD_PORTION = 0.5

    /** "Et" in "Et yiyor musunuz?" means red meat only; şarküteri (sucuk, sosis, salam) is not "et". */
    val RED_MEAT = setOf("Dana kıyma", "Dana kuşbaşı", "Dondurulmuş köfte")
    val CHICKEN = setOf("Tavuk göğsü", "Tavuk but / baget", "Bütün tavuk", "Tavuk kanat", "Sosis")
    val LEGUMES = setOf("Kırmızı mercimek", "Yeşil mercimek", "Nohut", "Kuru fasulye")

    /** Items that don't make a dish "contain" something for bans of the main-ingredient kind. */
    val NOT_MAIN = setOf("Tuz", "Karabiber", "Ayçiçek yağı")
    val LIQUIDS = setOf("Süt", "Ayçiçek yağı", "Zeytinyağı", "Üzüm / elma sirkesi", "Sıvı krema (pişirme kreması)")
    val HALF_OK = setOf("Limon", "Roka", "Maydanoz", "Dereotu", "Taze nane", "Spagetti", "Penne / burgu")

    const val TABLE = "tam sofra"
    val SIDES = setOf("pilav-makarna", "salata", "meze", "garnitür")

    /** "Ne lazım" choices in screen order: key to label. Garnitür is not a choice on its own. */
    val NEEDS = listOf(
        "kahvaltılık" to "Kahvaltılık",
        "çorba" to "Çorba",
        "ana yemek" to "Ana yemek",
        "pilav-makarna" to "Pilav-makarna",
        "salata" to "Salata",
        "meze" to "Meze",
        "hamur işi" to "Hamur işi",
        "atıştırmalık" to "Atıştırmalık",
        "tatlı" to "Tatlı",
        TABLE to "Tam sofra",
    )

    fun seasonsOf(month: Int): Set<String> = when (month) {
        in 5..9 -> setOf("yaz")
        11, 12, 1, 2, 3 -> setOf("kış")
        else -> setOf("yaz", "kış")
    }

    fun guestsToday(s: UserState, today: LocalDate) = s.guestDate == today.toString()

    fun people(s: UserState, today: LocalDate): Pair<Int, Int> =
        if (guestsToday(s, today)) s.guestAdults to s.guestKids else s.adults to s.kids

    fun portions(s: UserState, today: LocalDate): Double {
        val (a, k) = people(s, today)
        return (a + k * CHILD_PORTION).coerceAtLeast(1.0)
    }

    /** How many times the written recipe is made. Never below 1: recipes are never scaled down. */
    fun multiplier(portions: Double, baseSize: Int): Int =
        kotlin.math.ceil(portions / baseSize - 1e-9).toInt().coerceAtLeast(1)
}

enum class Protein { RED, CHICKEN, FISH, LEGUME, EGG, OTHER }

fun Catalog.protein(d: Dish): Protein {
    val names = ingredientIds(d).mapNotNull { pantry[it]?.name }.toSet()
    return when {
        names.any { it in Rules.RED_MEAT } -> Protein.RED
        names.any { it in Rules.CHICKEN } -> Protein.CHICKEN
        "Ton balığı" in names -> Protein.FISH
        names.any { it in Rules.LEGUMES } -> Protein.LEGUME
        "Yumurta" in names && "kahvaltılık" in d.sections -> Protein.EGG
        else -> Protein.OTHER
    }
}

fun Catalog.emoji(d: Dish): String = when (d.type) {
    "çorba" -> "🍲"
    "tatlı" -> "🍮"
    "pilav" -> "🍚"
    "makarna" -> "🍝"
    "salata-meze" -> "🥗"
    "hamur işi" -> "🥧"
    "garnitür" -> "🥔"
    else -> when (protein(d)) {
        Protein.RED -> if ("kahvaltılık" in d.sections) "🍳" else "🍖"
        Protein.CHICKEN -> "🍗"
        Protein.FISH -> "🐟"
        Protein.LEGUME -> "🫘"
        Protein.EGG -> "🍳"
        Protein.OTHER -> "🥘"
    }
}
