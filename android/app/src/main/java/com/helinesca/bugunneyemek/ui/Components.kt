package com.helinesca.bugunneyemek.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helinesca.bugunneyemek.data.Catalog
import com.helinesca.bugunneyemek.data.Dish
import com.helinesca.bugunneyemek.logic.emoji
import java.util.Locale

val TR: Locale = Locale.forLanguageTag("tr")

/** Lower-case and drop Turkish accents, so "cilbir" finds "Çılbır". */
fun fold(s: String): String = s.lowercase(TR)
    .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i').replace('ö', 'o').replace('ş', 's').replace('ü', 'u')

fun String.capitalizeTr() = replaceFirstChar { it.titlecase(TR) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = actions,
            )
        },
        bottomBar = { bottom?.invoke() },
        content = content,
    )
}

@Composable
fun BottomActions(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Choices(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            FilterChip(selected = selected == key, onClick = { onSelect(key) }, label = { Text(label) })
        }
    }
}

@Composable
fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, Modifier.width(96.dp))
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }) { Text("−") }
        Text("$value", Modifier.width(48.dp), style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(max)) }) { Text("+") }
    }
}

fun meta(d: Dish) = "${d.minutes} dk · ${d.effort} · ${d.cost} · ~${d.kcal} kcal"

@Composable
fun DishCard(vm: AppViewModel, dish: Dish, extra: String? = null) {
    ElevatedCard(onClick = { vm.openRecipe(dish.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${vm.catalog.emoji(dish)}  ${dish.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(dish.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            Text(meta(dish), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
            if (extra != null) Text(extra, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.openRecipe(dish.id) }) { Text("Tarif") }
                Spacer(Modifier.width(8.dp))
                CookedButton(vm, listOf(dish.id))
            }
        }
    }
}

@Composable
fun CookedButton(vm: AppViewModel, ids: List<Int>, label: String = "Yaptım ✓") {
    val done = ids.all { vm.cookedToday(it) }
    FilledTonalButton(onClick = { vm.markCooked(ids) }, enabled = !done) {
        Text(if (done) "Bugün yapıldı ✓" else label)
    }
}

/** Ingredient chips grouped as in the pantry, with a search box. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantryPicker(
    catalog: Catalog,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hide: Set<Int> = emptySet(),
    header: LazyListScope.() -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp)) {
        header()
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Malzeme ara") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        catalog.pantryGroups.forEach { (group, items) ->
            val shown = items.filter { it.id !in hide && (query.isBlank() || fold(it.name).contains(fold(query))) }
            if (shown.isNotEmpty()) item(key = group) {
                Label(group)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { p ->
                        FilterChip(selected = p.id in selected, onClick = { onToggle(p.id) }, label = { Text(p.name) })
                    }
                }
            }
        }
    }
}
