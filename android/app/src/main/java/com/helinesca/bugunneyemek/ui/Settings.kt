package com.helinesca.bugunneyemek.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helinesca.bugunneyemek.BuildConfig
import com.helinesca.bugunneyemek.logic.Rules
import com.helinesca.bugunneyemek.logic.emoji
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BrowseScreen(vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("") }
    val c = vm.catalog
    val shown = c.dishes
        .filter { query.isBlank() || fold(it.name).contains(fold(query)) }
        .filter { section.isEmpty() || section in it.sections }
        .sortedWith(compareBy(TR.let { java.text.Collator.getInstance(it) }) { it.name })
    Page(title = "Tüm yemekler", onBack = { vm.back() }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("Yemek ara") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = section.isEmpty(), onClick = { section = "" }, label = { Text("Hepsi") })
                Rules.NEEDS.filter { it.first != Rules.TABLE }.forEach { (k, l) ->
                    FilterChip(selected = section == k, onClick = { section = if (section == k) "" else k }, label = { Text(l) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(shown, key = { it.id }) { d ->
                    ListItem(
                        headlineContent = { Text("${c.emoji(d)}  ${d.name}") },
                        supportingContent = { Text(d.description) },
                        modifier = Modifier.clickable { vm.openRecipe(d.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = vm.state
    val c = vm.catalog
    fun names(ids: Set<Int>) = if (ids.isEmpty()) "Yok" else ids.mapNotNull { c.pantry[it]?.name }.sorted().joinToString()
    val rows = listOf(
        Triple("Alerjim var / yiyemem",
            (s.allergyGroups.map { it.capitalizeTr() } + s.bans.mapNotNull { c.pantry[it]?.name }).ifEmpty { listOf("Yok") }.joinToString(),
            Screen.Edit(EditKind.ALLERGIES)),
        Triple("Sevmem", names(s.dislikes), Screen.Edit(EditKind.DISLIKES)),
        Triple("Bayılırım", names(s.loves), Screen.Edit(EditKind.LOVES)),
        Triple("Et (kırmızı et)", mapOf("evet" to "Evet", "az" to "Az", "hic" to "Hiç").getValue(s.meat), Screen.Edit(EditKind.MEAT)),
        Triple("Evde kaç kişisiniz", peopleText(s.adults, s.kids), Screen.Edit(EditKind.HOUSEHOLD)),
        Triple("Varsayılan seçimler", "Mutfak, ne lazım, süre, enerji, bütçe", Screen.Edit(EditKind.DEFAULTS)),
        Triple("Geçmiş", "${s.history.size} kayıt", Screen.History),
        Triple("Hakkında", "Sürüm, uyarılar, lisanslar", Screen.About),
    )
    Page(title = "Ayarlar", onBack = { vm.back() }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(rows) { (title, value, target) ->
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(value, maxLines = 2) },
                    modifier = Modifier.clickable { vm.go(target) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EditScreen(vm: AppViewModel, what: EditKind) {
    Page(title = "Ayarlar", onBack = { vm.back() }) { pad ->
        val m = Modifier.padding(pad)
        when (what) {
            EditKind.ALLERGIES -> AllergyEditor(vm, m)
            EditKind.DISLIKES -> TasteEditor(vm, loves = false, modifier = m)
            EditKind.LOVES -> TasteEditor(vm, loves = true, modifier = m)
            EditKind.MEAT -> MeatEditor(vm, m)
            EditKind.HOUSEHOLD -> HouseholdEditor(vm, m)
            EditKind.DEFAULTS -> DefaultsEditor(vm, m)
        }
    }
}

private val DATE = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR)

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val entries = vm.state.history.sortedByDescending { it.date }
    Page(title = "Geçmiş", onBack = { vm.back() }) { pad ->
        if (entries.isEmpty()) {
            Text("Henüz \"Yaptım ✓\" dediğiniz bir yemek yok.", Modifier.padding(pad).padding(24.dp))
        } else LazyColumn(Modifier.padding(pad)) {
            items(entries) { e ->
                val d = vm.catalog.dishById[e.dishId]
                ListItem(
                    headlineContent = { Text(d?.let { "${vm.catalog.emoji(it)}  ${it.name}" } ?: "?") },
                    supportingContent = { Text(LocalDate.parse(e.date).format(DATE)) },
                    trailingContent = {
                        IconButton(onClick = { vm.removeCooked(e) }) { Icon(Icons.Default.Delete, "Sil") }
                    },
                    modifier = Modifier.clickable { d?.let { vm.openRecipe(it.id) } },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    Page(title = "Hakkında", onBack = { vm.back() }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Bugün Ne Yemek Yapsam?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Sürüm ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text("Bu uygulama internete bağlanmaz. Seçimleriniz ve geçmişiniz yalnızca bu telefonda saklanır.",
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp))
            HealthCard()
            Column(Modifier.padding(top = 16.dp)) { ImportantText() }
            Label("Kaynaklar ve lisanslar")
            Text(Texts.calorieSource, style = MaterialTheme.typography.bodySmall)
            Text("Yemek listesi ve tarifler: CC BY-NC 4.0 © helinesca. Uygulama kodu: MIT © helinesca.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
