package com.helinesca.bugunneyemek.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Cooked(val dishId: Int, val date: String)

/** Everything the app remembers about the user. Stored only on the phone, in files/state.json. */
data class UserState(
    val onboarded: Boolean = false,
    val allergyGroups: Set<String> = emptySet(),
    val bans: Set<Int> = emptySet(),
    val dislikes: Set<Int> = emptySet(),
    val loves: Set<Int> = emptySet(),
    val meat: String = "evet",          // evet / az / hic  (red meat only)
    val adults: Int = 2,
    val kids: Int = 0,
    val cuisine: String = "karisik",    // turk / dunya / karisik
    val need: String = "ana yemek",     // a "Ne lazım" section, or "tam sofra"
    val time: String = "farketmez",     // 30 / 60 / farketmez
    val energy: String = "normal",      // usendim / normal / keyifli
    val budget: String = "normal",      // aysonu / normal
    val guestDate: String? = null,      // guests apply only on this date
    val guestAdults: Int = 0,
    val guestKids: Int = 0,
    val history: List<Cooked> = emptyList(),
    val atHome: Set<Int> = emptySet(),
    val lastOpened: Cooked? = null,     // last recipe opened, for the "yaptınız mı?" card
    val askedAbout: String? = null,     // date of that opened recipe we already asked about
)

class StateStore(private val file: File) {

    fun load(): UserState = try {
        if (file.exists()) fromJson(JSONObject(file.readText())) else UserState()
    } catch (e: Exception) {
        UserState()
    }

    fun save(s: UserState) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(toJson(s).toString())
        tmp.renameTo(file)
    }

    private fun ints(set: Set<Int>) = JSONArray(set.sorted())
    private fun strs(set: Set<String>) = JSONArray(set.sorted())
    private fun JSONObject.intSet(key: String): Set<Int> =
        optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: emptySet()
    private fun JSONObject.strSet(key: String): Set<String> =
        optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
    private fun JSONObject.strOrNull(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)

    private fun toJson(s: UserState) = JSONObject().apply {
        put("onboarded", s.onboarded)
        put("allergyGroups", strs(s.allergyGroups))
        put("bans", ints(s.bans))
        put("dislikes", ints(s.dislikes))
        put("loves", ints(s.loves))
        put("meat", s.meat)
        put("adults", s.adults)
        put("kids", s.kids)
        put("cuisine", s.cuisine)
        put("need", s.need)
        put("time", s.time)
        put("energy", s.energy)
        put("budget", s.budget)
        put("guestDate", s.guestDate ?: JSONObject.NULL)
        put("guestAdults", s.guestAdults)
        put("guestKids", s.guestKids)
        put("history", JSONArray(s.history.map { JSONObject().put("dish", it.dishId).put("date", it.date) }))
        put("atHome", ints(s.atHome))
        put("lastOpened", s.lastOpened?.let { JSONObject().put("dish", it.dishId).put("date", it.date) } ?: JSONObject.NULL)
        put("askedAbout", s.askedAbout ?: JSONObject.NULL)
    }

    private fun fromJson(o: JSONObject): UserState {
        val d = UserState()
        val hist = o.optJSONArray("history")?.let { a ->
            (0 until a.length()).map { a.getJSONObject(it).let { h -> Cooked(h.getInt("dish"), h.getString("date")) } }
        } ?: emptyList()
        return UserState(
            onboarded = o.optBoolean("onboarded", false),
            allergyGroups = o.strSet("allergyGroups"),
            bans = o.intSet("bans"),
            dislikes = o.intSet("dislikes"),
            loves = o.intSet("loves"),
            meat = o.optString("meat", d.meat),
            adults = o.optInt("adults", d.adults),
            kids = o.optInt("kids", d.kids),
            cuisine = o.optString("cuisine", d.cuisine),
            need = o.optString("need", d.need),
            time = o.optString("time", d.time),
            energy = o.optString("energy", d.energy),
            budget = o.optString("budget", d.budget),
            guestDate = o.strOrNull("guestDate"),
            guestAdults = o.optInt("guestAdults", 0),
            guestKids = o.optInt("guestKids", 0),
            history = hist,
            atHome = o.intSet("atHome"),
            lastOpened = o.optJSONObject("lastOpened")?.let { Cooked(it.getInt("dish"), it.getString("date")) },
            askedAbout = o.strOrNull("askedAbout"),
        )
    }
}
