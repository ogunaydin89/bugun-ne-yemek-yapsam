package com.helinesca.bugunneyemek.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helinesca.bugunneyemek.BuildConfig
import com.helinesca.bugunneyemek.data.Dish
import com.helinesca.bugunneyemek.logic.Amounts
import com.helinesca.bugunneyemek.logic.Rules
import com.helinesca.bugunneyemek.logic.emoji

/** Packaged products whose labels someone with an allergy should check. */
private val PACKAGED = setOf(
    "Et / tavuk bulyon", "Kalıp margarin", "Kase margarin", "Bisküvi (petibör)", "Puding / krem şanti tozu",
    "Bitter çikolata", "Mantı (kuru veya dondurulmuş)", "Dondurulmuş köfte", "Dondurulmuş milföy", "Yufka", "Lavaş",
    "Ekmek", "Tost ekmeği", "Hamburger ekmeği", "Mayonez", "Ketçap", "Sucuk", "Sosis", "Salam",
)

private fun servings(d: Dish, n: Int) = if (d.baseSize == 8 && d.type == "tatlı") "$n dilim" else "$n kişilik"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(vm: AppViewModel, dishId: Int) {
    val c = vm.catalog
    val d = c.dishById.getValue(dishId)
    val s = vm.state
    val portions = Rules.portions(s, vm.today())
    val mult = Rules.multiplier(portions, d.baseSize)
    val made = d.baseSize * mult
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var noteDialog by remember { mutableStateOf(false) }

    val names = c.ingredientIds(d).mapNotNull { c.pantry[it]?.name }
    val notes = buildList {
        val bannedHere = c.ingredientIds(d).filter { it in (s.bans + c.allergies.filter { e -> e.group in s.allergyGroups }.map { e -> e.pantryId }) }
        if (bannedHere.isNotEmpty()) add("⛔ Yiyemem dediğiniz malzemeler var: " + bannedHere.mapNotNull { c.pantry[it]?.name }.joinToString())
        if ("bal" in d.warnings) add("⚠️ 1 yaşından küçük bebeklere bal verilmez.")
        if ("az_pismis_yumurta" in d.warnings) add("⚠️ Hamileler, küçük çocuklar ve yaşlılar için yumurtayı iyice pişirin.")
        if (s.allergyGroups.isNotEmpty() || s.bans.isNotEmpty()) {
            val packaged = names.filter { it in PACKAGED }
            if (packaged.isNotEmpty()) add("⚠️ Alerjiniz varsa şu paketli ürünlerin etiketini kontrol edin: ${packaged.joinToString()}.")
        }
        if (made > portions + 0.01) add(
            "Bu yemek en az ${servings(d, d.baseSize)} yapılır. " + when (d.storage) {
                "dondurulur" -> "Kalanı buzdolabında 2-3 gün saklayıp ısıtarak yiyebilirsiniz. Dondurucuda da saklanabilir."
                "buzdolabı" -> "Kalanı buzdolabında 2-3 gün saklayıp ısıtarak yiyebilirsiniz."
                else -> "En iyisi taze yemektir."
            }
        )
        if (mult >= 2) add("Daha büyük tencere ya da iki tepsi kullanın.")
    }

    Page(
        title = d.name,
        onBack = { vm.back() },
        bottom = {
            BottomActions {
                if (BuildConfig.DEBUG) OutlinedButton(onClick = { noteDialog = true }) { Text("📝 Not al") }
                Spacer(Modifier.weight(1f))
                CookedButton(vm, listOf(d.id))
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("${c.emoji(d)}  ${d.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                val per = if (d.baseSize == 8 && d.type == "tatlı") "dilim" else "porsiyon"
                Text("${servings(d, made)} · ${d.minutes} dk · ~${d.kcal} kcal/$per (tahmini)",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
                notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Malzemeler") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Yapılışı") })
            }
            if (tab == 0) Ingredients(vm, d, mult) else Steps(c.steps[d.id].orEmpty())
        }
    }

    if (noteDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noteDialog = false },
            title = { Text("Not al: ${d.name}") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Ne değişmeli?") }) },
            confirmButton = {
                TextButton(onClick = { if (text.isNotBlank()) vm.saveNote(d.id, text); noteDialog = false }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { noteDialog = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun Ingredients(vm: AppViewModel, d: Dish, mult: Int) {
    val lines = vm.catalog.ingredients[d.id].orEmpty()
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(lines) { i, line ->
            if (i > 0) HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, style = MaterialTheme.typography.bodyLarge)
                    if (line.frying) Text("(kızartma için)", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.width(150.dp)) {
                    Text(Amounts.kitchen(line, mult), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                    val metric = Amounts.metric(line, mult)
                    if (metric.isNotEmpty()) Text(metric, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Steps(steps: List<String>) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(steps) { i, step ->
            Row(Modifier.padding(vertical = 8.dp)) {
                Text("${i + 1}.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp))
                Text(step, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
