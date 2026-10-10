package com.helinesca.bugunneyemek.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val INTRO = 3          // steps 0..2: aim, how it works, not a cookbook
private const val HEALTH = 3
private const val IMPORTANT = 4
private const val ALLERGY = 5
private const val DISLIKES = 6
private const val LOVES = 7
private const val MEAT = 8
private const val HOUSEHOLD = 9
private const val DEFAULTS = 10
private const val DID_IT = 11

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val importantScroll = rememberScrollState()
    val readToEnd by remember {
        derivedStateOf { importantScroll.maxValue == 0 || importantScroll.value >= importantScroll.maxValue - 8 }
    }
    val question = step in ALLERGY..DEFAULTS

    Scaffold(
        bottomBar = {
            BottomActions {
                if (step > 0) TextButton(onClick = { step-- }) { Text("Geri") }
                Spacer(Modifier.weight(1f))
                if (question) TextButton(onClick = { step++ }) { Text("Atla") }
                when (step) {
                    HEALTH -> Button(onClick = { step++ }) { Text("Anladım") }
                    IMPORTANT -> Button(onClick = { step++ }, enabled = readToEnd) {
                        Text(if (readToEnd) "Okudum, anladım" else "Sonuna kadar okuyun")
                    }
                    DID_IT -> Button(onClick = {
                        vm.update { it.copy(onboarded = true) }
                        vm.restart(Screen.Home)
                    }) { Text("Başlayalım") }
                    else -> Button(onClick = { step++ }) { Text("Devam") }
                }
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        when (step) {
            in 0 until INTRO -> TextPage(m, Texts.intro[step].first, Texts.intro[step].second)
            HEALTH -> HealthPage(m)
            IMPORTANT -> Column(m.fillMaxSize().verticalScroll(importantScroll).padding(24.dp)) { ImportantText() }
            ALLERGY -> AllergyEditor(vm, m)
            DISLIKES -> TasteEditor(vm, loves = false, modifier = m)
            LOVES -> TasteEditor(vm, loves = true, modifier = m)
            MEAT -> MeatEditor(vm, m)
            HOUSEHOLD -> HouseholdEditor(vm, m)
            DEFAULTS -> DefaultsEditor(vm, m)
            DID_IT -> TextPage(m, Texts.didItTitle, Texts.didIt)
        }
    }
}

@Composable
private fun TextPage(modifier: Modifier, title: String, body: String) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.padding(top = 32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun HealthPage(modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.padding(top = 32.dp))
        HealthCard()
    }
}

@Composable
fun HealthCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(Texts.healthTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Text(Texts.health, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun ImportantText() {
    Text(Texts.importantTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Texts.important.forEach { (section, bullets) ->
        Text(section, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        bullets.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
    }
    Text(Texts.importantAccept, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 24.dp))
}
