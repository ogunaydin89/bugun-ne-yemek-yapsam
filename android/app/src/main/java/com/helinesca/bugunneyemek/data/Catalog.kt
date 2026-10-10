package com.helinesca.bugunneyemek.data

import android.content.res.AssetManager

data class PantryItem(
    val id: Int,
    val group: String,
    val name: String,
    val gCup: Double?,
    val gTbsp: Double?,
    val gTsp: Double?,
    val unit: String,
    val gUnit: Double?,
)

data class Dish(
    val id: Int,
    val name: String,
    val type: String,
    val sections: Set<String>,
    val speed: String,
    val effort: String,
    val cost: String,
    val forGuests: Boolean,
    val season: String,
    val cuisine: String,
    val baseSize: Int,
    val storage: String,
    val warnings: Set<String>,
    val minutes: Int,
    val kcal: Int,
    val description: String,
)

data class IngredientLine(
    val pantryId: Int?,
    val name: String,
    val amount: String,
    val unit: String,
    val grams: Int,
    val frying: Boolean,
)

data class AllergyEntry(val group: String, val pantryId: Int, val note: String)

/** The read-only data bundled with the app, built from the files in the repo's data/ folder. */
class Catalog(
    val pantry: Map<Int, PantryItem>,
    val dishes: List<Dish>,
    val ingredients: Map<Int, List<IngredientLine>>,
    val steps: Map<Int, List<String>>,
    val allergies: List<AllergyEntry>,
    val basics: Set<Int>,
) {
    val dishById: Map<Int, Dish> = dishes.associateBy { it.id }
    val pantryByName: Map<String, PantryItem> = pantry.values.associateBy { it.name }
    val allergyGroups: List<String> = allergies.map { it.group }.distinct()
    val pantryGroups: List<Pair<String, List<PantryItem>>> =
        pantry.values.sortedBy { it.id }.groupBy { it.group }.toList()

    private val idsCache = HashMap<Int, Set<Int>>()

    /** Every pantry item used in the dish's recipe. */
    fun ingredientIds(dish: Dish): Set<Int> = idsCache.getOrPut(dish.id) {
        ingredients[dish.id].orEmpty().mapNotNull { it.pantryId }.toSet()
    }

    fun idsNamed(names: Collection<String>): Set<Int> = names.mapNotNull { pantryByName[it]?.id }.toSet()

    companion object {
        fun load(assets: AssetManager): Catalog =
            parse { name -> assets.open("catalog/$name").bufferedReader(Charsets.UTF_8).use { it.readText() } }

        fun parse(read: (String) -> String): Catalog {
            val pantry = tsv(read("pantry.tsv")).associate { r ->
                val id = r.getValue("id").toInt()
                id to PantryItem(
                    id = id,
                    group = r.getValue("grup"),
                    name = r.getValue("malzeme"),
                    gCup = r["g_su_bardagi"]?.toDoubleOrNull(),
                    gTbsp = r["g_yemek_kasigi"]?.toDoubleOrNull(),
                    gTsp = r["g_cay_kasigi"]?.toDoubleOrNull(),
                    unit = r["birim"].orEmpty(),
                    gUnit = r["g_birim"]?.toDoubleOrNull(),
                )
            }
            val dishes = tsv(read("dishes.tsv")).map { r ->
                Dish(
                    id = r.getValue("id").toInt(),
                    name = r.getValue("ad"),
                    type = r.getValue("tur"),
                    sections = r.getValue("ne_lazim").split(',').filter { it.isNotBlank() }.toSet(),
                    speed = r.getValue("sure"),
                    effort = r.getValue("zahmet"),
                    cost = r.getValue("maliyet"),
                    forGuests = r["misafir"] == "evet",
                    season = r["mevsim"].orEmpty(),
                    cuisine = r.getValue("mutfak"),
                    baseSize = r.getValue("taban_kisi").toInt(),
                    storage = r.getValue("saklama"),
                    warnings = r["uyari"].orEmpty().split(',').filter { it.isNotBlank() }.toSet(),
                    minutes = r.getValue("sure_dk").toInt(),
                    kcal = r.getValue("kcal_porsiyon").toInt(),
                    description = r.getValue("aciklama"),
                )
            }
            val ingredients = tsv(read("recipe_ingredients.tsv"))
                .groupBy({ it.getValue("dish_id").toInt() }) { r ->
                    IngredientLine(
                        pantryId = r["pantry_id"]?.toIntOrNull(),
                        name = r.getValue("malzeme"),
                        amount = r.getValue("miktar"),
                        unit = r.getValue("olcu"),
                        grams = r.getValue("gram").toInt(),
                        frying = r["kizartma"] == "evet",
                    )
                }
            val steps = tsv(read("recipe_steps.tsv"))
                .groupBy({ it.getValue("dish_id").toInt() }) { it.getValue("metin") }
            val allergies = tsv(read("alerji_gruplari.tsv")).map {
                AllergyEntry(it.getValue("grup"), it.getValue("pantry_id").toInt(), it["not"].orEmpty())
            }
            val basics = tsv(read("temel_malzemeler.tsv")).map { it.getValue("pantry_id").toInt() }.toSet()
            return Catalog(pantry, dishes, ingredients, steps, allergies, basics)
        }

        private fun tsv(text: String): List<Map<String, String>> {
            val lines = text.lines().filter { it.isNotBlank() }
            val header = lines.first().split('\t')
            return lines.drop(1).map { line ->
                val cells = line.split('\t')
                header.indices.associate { header[it] to cells.getOrElse(it) { "" } }
            }
        }
    }
}
