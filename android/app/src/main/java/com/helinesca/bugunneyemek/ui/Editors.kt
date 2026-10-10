package com.helinesca.bugunneyemek.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helinesca.bugunneyemek.logic.Rules

/** The question editors, used both at install and later from Ayarlar. */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllergyEditor(vm: AppViewModel, modifier: Modifier = Modifier) {
    val s = vm.state
    val c = vm.catalog
    PantryPicker(
        catalog = c,
        selected = s.bans,
        onToggle = { id -> vm.update { it.copy(bans = it.bans.toggle(id), loves = it.loves - id) } },
        modifier = modifier,
        header = {
            item {
                Text("Alerjim var / yiyemem", style = MaterialTheme.typography.headlineSmall)
                Text("Seçtikleriniz hiç önerilmez.", modifier = Modifier.padding(top = 4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) { Text(Texts.allergyWarning, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
                Label("Gruplar")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    c.allergyGroups.forEach { g ->
                        FilterChip(
                            selected = g in s.allergyGroups,
                            onClick = { vm.update { it.copy(allergyGroups = it.allergyGroups.toggle(g)) } },
                            label = { Text(g.capitalizeTr()) },
                        )
                    }
                }
                s.allergyGroups.sorted().forEach { g ->
                    val names = c.allergies.filter { it.group == g }.joinToString { e ->
                        c.pantry[e.pantryId]!!.name + if (e.note.isNotEmpty()) " (${e.note})" else ""
                    }
                    Text("${g.capitalizeTr()}: $names", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                Label("Tek tek malzemeler")
            }
        },
    )
}

@Composable
fun TasteEditor(vm: AppViewModel, loves: Boolean, modifier: Modifier = Modifier) {
    val s = vm.state
    PantryPicker(
        catalog = vm.catalog,
        selected = if (loves) s.loves else s.dislikes,
        onToggle = { id ->
            vm.update {
                if (loves) it.copy(loves = it.loves.toggle(id), dislikes = it.dislikes - id)
                else it.copy(dislikes = it.dislikes.toggle(id), loves = it.loves - id)
            }
        },
        modifier = modifier,
        hide = if (loves) s.bans else emptySet(),
        header = {
            item {
                Text(if (loves) "Bayılırım" else "Sevmem", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (loves) "Bu malzemelerle yapılan yemekler daha sık önerilir."
                    else "Bu malzemelerle yapılan yemekler daha seyrek önerilir, ama tamamen kaybolmaz.",
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }
        },
    )
}

@Composable
private fun Scroll(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { content() }
}

@Composable
fun MeatEditor(vm: AppViewModel, modifier: Modifier = Modifier) = Scroll(modifier) {
    Text("Et yiyor musunuz?", style = MaterialTheme.typography.headlineSmall)
    Text("Burada \"et\" kırmızı et demek: kıyma, kuşbaşı, köfte. Tavuk, balık ve sucuk, sosis, salam gibi şarküteri ürünleri bundan etkilenmez.",
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
    Choices(listOf("evet" to "Evet", "az" to "Az", "hic" to "Hiç"), vm.state.meat) { v -> vm.update { it.copy(meat = v) } }
    Text(
        when (vm.state.meat) {
            "az" -> "Kırmızı etli yemekler en fazla gün aşırı önerilir."
            "hic" -> "Kırmızı etli yemekler hiç önerilmez."
            else -> "Kırmızı etli yemekler normal şekilde önerilir."
        },
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
fun HouseholdEditor(vm: AppViewModel, modifier: Modifier = Modifier) = Scroll(modifier) {
    Text("Evde kaç kişisiniz?", style = MaterialTheme.typography.headlineSmall)
    Text("Tariflerdeki miktarlar buna göre hesaplanır. Bir çocuk yarım porsiyon sayılır.",
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
    Stepper("Yetişkin", vm.state.adults, 1, 12) { v -> vm.update { it.copy(adults = v) } }
    Stepper("Çocuk", vm.state.kids, 0, 10) { v -> vm.update { it.copy(kids = v) } }
}

val CUISINES = listOf("turk" to "Türk", "dunya" to "Dünya", "karisik" to "Karışık")
val TIMES = listOf("30" to "30 dk", "60" to "1 saat", "farketmez" to "Farketmez")
val ENERGIES = listOf("usendim" to "Üşeniyorum 😴", "normal" to "Normal", "keyifli" to "Keyifliyim 👩‍🍳")
val BUDGETS = listOf("aysonu" to "Ay sonu 💸", "normal" to "Normal")

@Composable
fun DefaultsEditor(vm: AppViewModel, modifier: Modifier = Modifier) = Scroll(modifier) {
    val s = vm.state
    Text("Varsayılan seçimler", style = MaterialTheme.typography.headlineSmall)
    Text("Ana ekranda hazır gelir; her seferinde son seçiminiz hatırlanır.", modifier = Modifier.padding(top = 4.dp))
    Label("Mutfak")
    Choices(CUISINES, s.cuisine) { v -> vm.update { it.copy(cuisine = v) } }
    Label("Ne lazım")
    Choices(Rules.NEEDS, s.need) { v -> vm.update { it.copy(need = v) } }
    Label("Süre")
    Choices(TIMES, s.time) { v -> vm.update { it.copy(time = v) } }
    Label("Enerji")
    Choices(ENERGIES, s.energy) { v -> vm.update { it.copy(energy = v) } }
    Label("Bütçe")
    Choices(BUDGETS, s.budget) { v -> vm.update { it.copy(budget = v) } }
}

fun <T> Set<T>.toggle(x: T): Set<T> = if (x in this) this - x else this + x
