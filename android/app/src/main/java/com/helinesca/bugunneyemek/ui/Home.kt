package com.helinesca.bugunneyemek.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.helinesca.bugunneyemek.logic.Rules
import com.helinesca.bugunneyemek.logic.Suggestion
import com.helinesca.bugunneyemek.logic.emoji

fun peopleText(adults: Int, kids: Int): String {
    val total = adults + kids
    return if (kids == 0) "$total kişi" else "$total kişi ($adults yetişkin, $kids çocuk)"
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val s = vm.state
    val today = vm.today()
    val guests = Rules.guestsToday(s, today)
    var peopleDialog by remember { mutableStateOf(false) }
    var guestDialog by remember { mutableStateOf(false) }
    var folded by rememberSaveable { mutableStateOf(true) }

    Page(
        title = "Bugün ne yemek yapsam?",
        onBack = null,
        actions = {
            IconButton(onClick = { vm.go(Screen.Browse) }) { Icon(Icons.Default.Search, "Tüm yemekler") }
            IconButton(onClick = { vm.go(Screen.Settings) }) { Icon(Icons.Default.Settings, "Ayarlar") }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            vm.pendingQuestion()?.let { q ->
                val dish = vm.catalog.dishById[q.dishId]
                if (dish != null) Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Geçen sefer ${dish.name} tarifine bakmıştınız. Yaptınız mı?")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { vm.answerQuestion(q, false) }) { Text("Hayır") }
                            TextButton(onClick = { vm.answerQuestion(q, true) }) { Text("Evet ✓") }
                        }
                    }
                }
            }

            Label("Kaç kişi")
            Row {
                FilterChip(selected = false, onClick = { peopleDialog = true },
                    label = { Text(peopleText(s.adults, s.kids)) })
                if (guests) Text("  Bugün misafirle: ${peopleText(s.guestAdults, s.guestKids)}",
                    modifier = Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.primary)
            }
            Label("Mutfak")
            Choices(CUISINES, s.cuisine) { v -> vm.update { it.copy(cuisine = v) } }
            Label("Ne lazım")
            Choices(Rules.NEEDS, s.need) { v -> vm.update { it.copy(need = v) } }

            TextButton(onClick = { folded = !folded }, modifier = Modifier.padding(top = 8.dp)) {
                Text(if (folded) "Bugün farklı bir durum var mı? ▾" else "Bugün farklı bir durum var mı? ▴")
            }
            if (!folded) {
                Label("Süre")
                Choices(TIMES, s.time) { v -> vm.update { it.copy(time = v) } }
                Label("Enerji")
                Choices(ENERGIES, s.energy) { v -> vm.update { it.copy(energy = v) } }
                Label("Bütçe")
                Choices(BUDGETS, s.budget) { v -> vm.update { it.copy(budget = v) } }
                Label("Misafir")
                FilterChip(
                    selected = guests,
                    onClick = {
                        if (guests) vm.update { it.copy(guestDate = null) } else guestDialog = true
                    },
                    label = { Text("Misafir var") },
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = { vm.suggest(fresh = true) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text("Öner", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(onClick = { vm.go(Screen.AtHome) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Elimdekilerle öner")
            }
        }
    }

    if (peopleDialog) CountDialog(
        title = "Evde kaç kişisiniz?", adults = s.adults, kids = s.kids, minAdults = 1,
        onDismiss = { peopleDialog = false },
        onSave = { a, k -> vm.update { it.copy(adults = a, kids = k) }; peopleDialog = false },
    )
    if (guestDialog) CountDialog(
        title = "Normalde ${peopleText(s.adults, s.kids)}. Bugün sofrada kaç kişi olacak?",
        adults = if (s.guestAdults > 0) s.guestAdults else s.adults + 2,
        kids = if (s.guestAdults > 0) s.guestKids else s.kids, minAdults = 1,
        onDismiss = { guestDialog = false },
        onSave = { a, k ->
            vm.update { it.copy(guestDate = today.toString(), guestAdults = a, guestKids = k) }
            guestDialog = false
        },
    )
}

@Composable
fun CountDialog(title: String, adults: Int, kids: Int, minAdults: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var a by remember { mutableIntStateOf(adults) }
    var k by remember { mutableIntStateOf(kids) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Stepper("Yetişkin", a, minAdults, 30) { a = it }
                Stepper("Çocuk", k, 0, 20) { k = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(a, k) }) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
fun ResultsScreen(vm: AppViewModel) {
    val title = Rules.NEEDS.firstOrNull { it.first == vm.state.need }?.second ?: "Öneriler"
    Page(
        title = title,
        onBack = { vm.back() },
        bottom = {
            BottomActions {
                Button(onClick = { vm.suggest(fresh = false) }, modifier = Modifier.fillMaxWidth()) { Text("Başka öner") }
            }
        },
    ) { pad ->
        Results(vm, pad, empty = "Bu seçimlere uyan başka yemek kalmadı. Ana ekranda süreyi, mutfağı ya da \"Ne lazım\"ı değiştirmeyi deneyin.")
    }
}

@Composable
private fun Results(vm: AppViewModel, pad: PaddingValues, empty: String) {
    if (vm.results.isEmpty()) {
        Text(empty, Modifier.padding(pad).padding(24.dp))
        return
    }
    LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(vm.results) { r ->
            when (r) {
                is Suggestion.Single -> DishCard(vm, r.dish)
                is Suggestion.FromHome -> DishCard(
                    vm, r.dish,
                    extra = if (r.missing.isEmpty()) "Her şey evde var ✓" else "Eksik: " + r.missing.joinToString(),
                )
                is Suggestion.Table -> TableCard(vm, r)
            }
        }
    }
}

@Composable
private fun TableCard(vm: AppViewModel, t: Suggestion.Table) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Bugünün sofrası", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            listOf(t.soup, t.main, t.side).forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider()
                Column(Modifier.fillMaxWidth().clickable { vm.openRecipe(d.id) }.padding(vertical = 10.dp)) {
                    Text("${vm.catalog.emoji(d)}  ${d.name}", style = MaterialTheme.typography.titleMedium)
                    Text(meta(d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                CookedButton(vm, listOf(t.soup.id, t.main.id, t.side.id), label = "Hepsini yaptım ✓")
            }
        }
    }
}

@Composable
fun AtHomeScreen(vm: AppViewModel) {
    val c = vm.catalog
    val basicNames = c.basics.mapNotNull { c.pantry[it]?.name }.sorted().joinToString()
    Page(
        title = "Elimde neler var?",
        onBack = { vm.back() },
        bottom = {
            BottomActions {
                TextButton(onClick = { vm.update { it.copy(atHome = emptySet()) } }) { Text("Temizle") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.suggestFromHome(fresh = true) }, enabled = vm.state.atHome.isNotEmpty()) {
                    Text("Öner (${vm.state.atHome.size})")
                }
            }
        },
    ) { pad ->
        PantryPicker(
            catalog = c,
            selected = vm.state.atHome,
            onToggle = { id -> vm.update { it.copy(atHome = it.atHome.toggle(id)) } },
            modifier = Modifier.padding(pad),
            hide = c.basics,
            header = {
                item {
                    Text("Evdeki ana malzemeleri seçin.", style = MaterialTheme.typography.bodyLarge)
                    Text("Şunları zaten evde var sayıyoruz: $basicNames.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                }
            },
        )
    }
}

@Composable
fun AtHomeResultsScreen(vm: AppViewModel) {
    Page(
        title = "Elimdekilerle",
        onBack = { vm.back() },
        bottom = {
            BottomActions {
                Button(onClick = { vm.suggestFromHome(fresh = false) }, modifier = Modifier.fillMaxWidth()) { Text("Başka öner") }
            }
        },
    ) { pad ->
        Results(vm, pad, empty = "Bu malzemelerle yapılabilecek başka yemek bulamadık. Birkaç malzeme daha seçmeyi deneyin.")
    }
}
