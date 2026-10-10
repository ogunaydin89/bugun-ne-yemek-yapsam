package com.helinesca.bugunneyemek.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.helinesca.bugunneyemek.data.Catalog
import com.helinesca.bugunneyemek.data.Cooked
import com.helinesca.bugunneyemek.data.StateStore
import com.helinesca.bugunneyemek.data.UserState
import com.helinesca.bugunneyemek.logic.Suggester
import com.helinesca.bugunneyemek.logic.Suggestion
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object Results : Screen
    data object AtHome : Screen
    data object AtHomeResults : Screen
    data class Recipe(val dishId: Int) : Screen
    data object Browse : Screen
    data object Settings : Screen
    data class Edit(val what: EditKind) : Screen
    data object History : Screen
    data object About : Screen
}

enum class EditKind { ALLERGIES, DISLIKES, LOVES, MEAT, HOUSEHOLD, DEFAULTS }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val catalog: Catalog = Catalog.load(app.assets)
    private val store = StateStore(File(app.filesDir, "state.json"))
    private val notesFile = File(app.filesDir, "notlar.txt")

    var state by mutableStateOf(store.load())
        private set

    val nav = mutableStateListOf<Screen>(if (state.onboarded) Screen.Home else Screen.Onboarding)
    val screen: Screen get() = nav.last()

    var results by mutableStateOf<List<Suggestion>>(emptyList())
        private set
    private val shown = mutableSetOf<Int>()

    fun today(): LocalDate = LocalDate.now()

    fun update(change: (UserState) -> UserState) {
        state = change(state)
        store.save(state)
    }

    fun go(s: Screen) { nav.add(s) }
    fun back(): Boolean = if (nav.size > 1) { nav.removeAt(nav.lastIndex); true } else false
    fun restart(s: Screen) { nav.clear(); nav.add(s) }

    // --- Suggestions ---

    private fun suggester() = Suggester(catalog, state, today())

    fun suggest(fresh: Boolean) {
        if (fresh) shown.clear()
        results = suggester().suggest(shown)
        shown += results.flatMap { dishIds(it) }
        if (screen != Screen.Results) go(Screen.Results)
    }

    fun suggestFromHome(fresh: Boolean) {
        if (fresh) shown.clear()
        results = suggester().fromHome(state.atHome, shown)
        shown += results.flatMap { dishIds(it) }
        if (screen != Screen.AtHomeResults) go(Screen.AtHomeResults)
    }

    fun dishIds(s: Suggestion): List<Int> = when (s) {
        is Suggestion.Single -> listOf(s.dish.id)
        is Suggestion.Table -> listOf(s.soup.id, s.main.id, s.side.id)
        is Suggestion.FromHome -> listOf(s.dish.id)
    }

    // --- Memory ---

    fun markCooked(ids: List<Int>, date: LocalDate = today()) = update { s ->
        val add = ids.filter { id -> s.history.none { it.dishId == id && it.date == date.toString() } }
        s.copy(
            history = s.history + add.map { Cooked(it, date.toString()) },
            guestDate = if (s.guestDate == date.toString()) null else s.guestDate,   // guests reset after "Yaptım"
        )
    }

    fun cookedToday(id: Int) = state.history.any { it.dishId == id && it.date == today().toString() }

    fun removeCooked(entry: Cooked) = update { it.copy(history = it.history - entry) }

    fun openRecipe(id: Int) {
        update { it.copy(lastOpened = Cooked(id, today().toString())) }
        go(Screen.Recipe(id))
    }

    /** The dish to ask "Dün ... yaptınız mı?" about, if any. */
    fun pendingQuestion(): Cooked? {
        val o = state.lastOpened ?: return null
        val opened = LocalDate.parse(o.date)
        if (!opened.isBefore(today()) || opened.isBefore(today().minusDays(2))) return null
        if (state.askedAbout == o.date) return null
        if (state.history.any { it.dishId == o.dishId && !LocalDate.parse(it.date).isBefore(opened) }) return null
        return o
    }

    fun answerQuestion(q: Cooked, cooked: Boolean) {
        if (cooked) markCooked(listOf(q.dishId), LocalDate.parse(q.date))
        update { it.copy(askedAbout = q.date) }
    }

    // --- Owner's test notes (debug builds only) ---

    fun saveNote(dishId: Int, text: String) {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val name = catalog.dishById[dishId]?.name ?: "?"
        notesFile.appendText("$stamp | $dishId $name | ${text.replace('\n', ' ')}\n")
    }
}
