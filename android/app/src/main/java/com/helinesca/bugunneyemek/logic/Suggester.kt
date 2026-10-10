package com.helinesca.bugunneyemek.logic

import com.helinesca.bugunneyemek.data.Catalog
import com.helinesca.bugunneyemek.data.Dish
import com.helinesca.bugunneyemek.data.UserState
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

sealed interface Suggestion {
    data class Single(val dish: Dish) : Suggestion
    data class Table(val soup: Dish, val main: Dish, val side: Dish) : Suggestion
    data class FromHome(val dish: Dish, val missing: List<String>) : Suggestion
}

/**
 * Picks dishes: first filter (bans, red meat, cuisine, section, time), then score
 * (tastes, situation, memory, season), then take the best with some variety.
 * See README, "Suggestion logic".
 */
class Suggester(
    private val c: Catalog,
    private val s: UserState,
    private val today: LocalDate,
    private val random: Random = Random.Default,
) {
    val banned: Set<Int> = s.bans + c.allergies.filter { it.group in s.allergyGroups }.map { it.pantryId }
    private val cooked = s.history.map { it.dishId to LocalDate.parse(it.date) }
    private val redIds = c.idsNamed(Rules.RED_MEAT)
    private val seasons = Rules.seasonsOf(today.monthValue)
    private val portions = Rules.portions(s, today)
    private val guests = Rules.guestsToday(s, today)

    private fun daysAgo(date: LocalDate) = ChronoUnit.DAYS.between(date, today)

    private val redRecently = cooked.any { (id, date) ->
        daysAgo(date) in 0..1 && c.dishById[id]?.let { c.protein(it) == Protein.RED } == true
    }
    private val lastProtein: Protein? = cooked.filter { daysAgo(it.second) in 0..1 }
        .maxByOrNull { it.second }?.let { (id, _) -> c.dishById[id]?.let { c.protein(it) } }

    fun allowed(d: Dish, section: String?): Boolean {
        val ids = c.ingredientIds(d)
        if (ids.any { it in banned }) return false
        if (ids.any { it in redIds }) {
            if (s.meat == "hic") return false
            if (s.meat == "az" && redRecently) return false
        }
        when (s.cuisine) {
            "turk" -> if (d.cuisine != "Türk") return false
            "dunya" -> if (d.cuisine != "Dünya") return false
        }
        if (section != null && section !in d.sections) return false
        when (s.time) {
            "30" -> if (d.minutes > 30) return false
            "60" -> if (d.minutes > 60) return false
        }
        return true
    }

    fun score(d: Dish): Double {
        val ids = c.ingredientIds(d)
        var score = random.nextDouble() * 2.5
        score += 3.0 * ids.count { it in s.loves }
        score -= 6.0 * ids.count { it in s.dislikes }
        cooked.filter { it.first == d.id }.maxOfOrNull { it.second }?.let {
            if (daysAgo(it) < Rules.MEMORY_DAYS) score -= 50
        }
        val p = c.protein(d)
        if (lastProtein != null && p == lastProtein && p in setOf(Protein.RED, Protein.CHICKEN, Protein.LEGUME)) score -= 2
        if (guests && d.forGuests) score += 3
        when (s.energy) {
            "usendim" -> score += when (d.effort) { "kolay" -> 3.0; "uğraştırır" -> -6.0; else -> 0.0 } - if (d.minutes > 60) 3 else 0
            "keyifli" -> if (d.effort == "uğraştırır") score += 2
        }
        if (s.budget == "aysonu") score += if (d.cost == "ucuz") 3 else -3
        if (d.season.isNotEmpty()) score += if (d.season in seasons) 1.0 else -4.0
        if (portions <= 1.5 && !guests) {
            if (d.baseSize <= 2) score += 1.5
            if (d.storage != "taze") score += 1.0
        }
        return score
    }

    /** Best [n] dishes from a section, avoiding three of the same kind (e.g. three kıyma dishes). */
    fun top(section: String?, n: Int, exclude: Set<Int>, filter: (Dish) -> Boolean = { true }): List<Dish> {
        val scored = c.dishes.filter { it.id !in exclude && allowed(it, section) && filter(it) }
            .map { it to score(it) }.toMutableList()
        val picked = mutableListOf<Dish>()
        while (picked.size < n && scored.isNotEmpty()) {
            val best = scored.maxBy { (d, sc) -> sc - 3.0 * picked.count { c.protein(it) == c.protein(d) && c.protein(d) != Protein.OTHER } }
            picked += best.first
            scored.remove(best)
        }
        return picked
    }

    fun suggest(exclude: Set<Int>): List<Suggestion> =
        if (s.need == Rules.TABLE) listOfNotNull(table(exclude))
        else top(s.need, 3, exclude).map { Suggestion.Single(it) }

    fun table(exclude: Set<Int>): Suggestion.Table? {
        val soup = top("çorba", 1, exclude).firstOrNull() ?: return null
        val main = top("ana yemek", 1, exclude).firstOrNull() ?: return null
        val side = top(null, 1, exclude) { d -> d.sections.any { it in Rules.SIDES } }.firstOrNull() ?: return null
        return Suggestion.Table(soup, main, side)
    }

    /** Dishes that can be made (or nearly) from what is at home. Basics are assumed to be there. */
    fun fromHome(atHome: Set<Int>, exclude: Set<Int>, n: Int = 6): List<Suggestion.FromHome> {
        val have = atHome + c.basics
        val notMain = c.idsNamed(Rules.NOT_MAIN)
        return c.dishes.asSequence()
            .filter { it.id !in exclude && allowed(it, null) }
            .mapNotNull { d ->
                val need = c.ingredientIds(d) - notMain
                if (need.none { it in atHome }) return@mapNotNull null
                val missing = need.filter { it !in have }
                if (missing.size > 2) null else Triple(d, missing, score(d) - 4.0 * missing.size)
            }
            .sortedByDescending { it.third }
            .take(n)
            .map { (d, missing, _) -> Suggestion.FromHome(d, missing.mapNotNull { c.pantry[it]?.name }) }
            .toList()
    }
}
