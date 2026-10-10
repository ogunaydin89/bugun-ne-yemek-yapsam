package com.helinesca.bugunneyemek

import com.helinesca.bugunneyemek.data.Catalog
import com.helinesca.bugunneyemek.data.Cooked
import com.helinesca.bugunneyemek.data.UserState
import com.helinesca.bugunneyemek.logic.Amounts
import com.helinesca.bugunneyemek.logic.Protein
import com.helinesca.bugunneyemek.logic.Rules
import com.helinesca.bugunneyemek.logic.Suggester
import com.helinesca.bugunneyemek.logic.Suggestion
import com.helinesca.bugunneyemek.logic.protein
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

/** Runs against the real files in the repo's data/ folder. */
class LogicTest {
    private val c = Catalog.parse { File("../../data/$it").readText() }
    private val today = LocalDate.of(2026, 9, 24)
    private fun suggester(s: UserState, seed: Int = 1) = Suggester(c, s, today, Random(seed))

    @Test fun everyDishHasARecipe() {
        assertEquals(153, c.dishes.size)
        c.dishes.forEach {
            assertTrue("${it.name}: ingredients", c.ingredients[it.id].orEmpty().isNotEmpty())
            assertTrue("${it.name}: steps", c.steps[it.id].orEmpty().size >= 3)
            assertTrue("${it.name}: sections", it.sections.isNotEmpty())
        }
    }

    @Test fun fractions() {
        assertEquals("1 ½", Amounts.fraction(1.5))
        assertEquals("¼", Amounts.fraction(0.25))
        assertEquals("⅛", Amounts.fraction(0.125))
        assertEquals("3", Amounts.fraction(3.0))
    }

    @Test fun neverHalfAnEggAndMargarineInEighths() {
        c.ingredients.values.flatten().forEach { line ->
            for (mult in 1..4) {
                val q = Amounts.quantity(line, mult)
                if (line.unit in Amounts.COUNT_UNITS && line.name !in Rules.HALF_OK && line.name != "Kalıp margarin") {
                    assertEquals("${line.name} x$mult", q, Math.rint(q), 1e-9)
                }
            }
        }
        val marg = c.ingredients.values.flatten().first { it.name == "Kalıp margarin" && it.amount == "1/8" }
        assertEquals("⅛ paket", Amounts.kitchen(marg, 1))
    }

    @Test fun recipesAreOnlyMultipliedNeverDivided() {
        assertEquals(1, Rules.multiplier(1.0, 4))
        assertEquals(1, Rules.multiplier(3.0, 4))
        assertEquals(2, Rules.multiplier(6.0, 4))
        assertEquals(2, Rules.multiplier(3.0, 2))
        assertEquals(1, Rules.multiplier(4.0, 4))
        val s = UserState(adults = 2, kids = 2)
        assertEquals(3.0, Rules.portions(s, today), 1e-9)
    }

    @Test fun noRedMeatWhenMeatIsHic() {
        val s = UserState(meat = "hic", need = "ana yemek")
        repeat(20) { seed ->
            suggester(s, seed).top("ana yemek", 10, emptySet()).forEach {
                assertTrue(it.name, c.protein(it) != Protein.RED)
            }
        }
    }

    @Test fun glutenAllergyBansFlourDishes() {
        val s = UserState(allergyGroups = setOf("gluten"))
        val flour = c.pantryByName.getValue("Buğday unu").id
        val sug = suggester(s)
        c.dishes.filter { flour in c.ingredientIds(it) }.forEach { assertTrue(it.name, !sug.allowed(it, null)) }
    }

    @Test fun eggDishesAreBreakfastNotMain() {
        listOf("Menemen", "Çılbır", "Omlet", "Sucuklu yumurta").forEach { n ->
            val d = c.dishes.first { it.name == n }
            assertTrue(n, "kahvaltılık" in d.sections && "ana yemek" !in d.sections)
        }
    }

    @Test fun tableHasSoupMainAndSide() {
        val t = suggester(UserState(need = Rules.TABLE)).suggest(emptySet()).single() as Suggestion.Table
        assertTrue("çorba" in t.soup.sections)
        assertTrue("ana yemek" in t.main.sections)
        assertTrue(t.side.sections.any { it in Rules.SIDES })
    }

    @Test fun recentlyCookedDishIsNotSuggestedAgain() {
        val karniyarik = c.dishes.first { it.name == "Karnıyarık" }
        val s = UserState(need = "ana yemek", history = listOf(Cooked(karniyarik.id, today.minusDays(3).toString())))
        repeat(30) { seed ->
            val ids = suggester(s, seed).suggest(emptySet()).map { (it as Suggestion.Single).dish.id }
            assertTrue(karniyarik.id !in ids)
        }
    }

    @Test fun fromHomeUsesWhatIsAtHome() {
        val have = c.idsNamed(listOf("Kırmızı mercimek", "Havuç", "Patates", "Limon"))
        val res = suggester(UserState()).fromHome(have, emptySet())
        assertTrue(res.any { it.dish.name == "Mercimek çorbası" && it.missing.isEmpty() })
        res.forEach { assertTrue(it.missing.size <= 2) }
    }
}
