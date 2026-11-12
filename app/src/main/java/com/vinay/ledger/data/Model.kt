package com.vinay.ledger.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min
import kotlin.math.roundToLong

/* ---------- Types ---------- */

enum class Bucket(val key: String, val label: String) {
    FIXED("fixed", "Fixed"), RECURRING("recurring", "Recurring"), ONEOFF("oneoff", "One-off");

    companion object {
        fun of(k: String?): Bucket? = entries.firstOrNull { it.key == k }
    }
}

data class Entry(
    val id: String,
    val date: String,          // yyyy-MM-dd
    val desc: String,
    val amount: Double,
    val cat: String,
    val bucket: Bucket? = null,
    val created: Long = 0L,
)

data class Cat(val name: String, val color: Long, val kw: List<String> = emptyList())

data class NamedAmount(val name: String, val amount: Double)

data class Plan(
    val salary: Double = 0.0,
    val saves: List<NamedAmount> = emptyList(),
    val bills: List<NamedAmount>? = null, // null on a month = use the monthly plan's bills
) {
    val billList: List<NamedAmount> get() = bills ?: emptyList()
}

data class Budgets(val recurring: Double = 0.0, val delivery: Double = 0.0, val oneoff: Double = 0.0)

data class LedgerData(
    val entries: List<Entry> = emptyList(),
    val cats: Map<String, Cat> = defaultCats(),
    val alias: Map<String, String> = emptyMap(),
    val plan: Plan = Plan(
        112000.0,
        listOf(NamedAmount("Postal RD", 20000.0), NamedAmount("Home / land loan", 20000.0)),
    ),
    val months: Map<String, Plan> = emptyMap(),
    val budgets: Budgets = Budgets(17000.0, 4000.0, 15000.0),
)

/* ---------- Default categories (same ids as the web app, so backups move across) ---------- */

private val CAT_DEFS = listOf(
    Triple("rent", "PG rent", "fixed"), Triple("physio_bill", "Physio bill", "fixed"),
    Triple("swiggy", "Swiggy", "recurring"), Triple("zomato", "Zomato", "recurring"),
    Triple("lunch", "Office lunch", "recurring"), Triple("eatout", "Eating out", "recurring"),
    Triple("snacks", "Fruits & snacks", "recurring"), Triple("grocery", "Groceries", "recurring"),
    Triple("t_office", "Office commute", "recurring"), Triple("t_physio", "Physio commute", "recurring"),
    Triple("t_local", "Local rides", "recurring"), Triple("electricity", "Electricity", "recurring"),
    Triple("laundry", "Laundry", "recurring"), Triple("recharge", "Recharges", "recurring"),
    Triple("sub_rec", "Subscriptions", "recurring"), Triple("salon", "Salon", "recurring"),
    Triple("physio_consult", "Physio consultation", "recurring"), Triple("travel", "Travel & trips", "oneoff"),
    Triple("gifts", "Gifts & family", "oneoff"), Triple("entertainment", "Entertainment", "oneoff"),
    Triple("sub_one", "One-time subscriptions", "oneoff"), Triple("personal", "Shopping & fees", "oneoff"),
    Triple("health", "Medical", "oneoff"), Triple("other", "Other", "recurring"),
)
val RULE_BUCKET: Map<String, String> = CAT_DEFS.associate { it.first to it.third }

private val DEFAULT_COLOR = mapOf(
    "rent" to 0xFF5B6470, "physio_bill" to 0xFF8C3B4E, "physio_consult" to 0xFF8C3B4E, "swiggy" to 0xFFB07D2B,
    "zomato" to 0xFFA0522D, "lunch" to 0xFF9C6B3F, "eatout" to 0xFF7B4B7F, "snacks" to 0xFF6B7F3A,
    "grocery" to 0xFF2F7D6D, "t_office" to 0xFF3D5A80, "t_physio" to 0xFF4A7C8C, "t_local" to 0xFF55529A,
    "electricity" to 0xFF5B6470, "laundry" to 0xFF4A7C8C, "recharge" to 0xFF3D5A80, "sub_rec" to 0xFF7B4B7F,
    "salon" to 0xFF9C6B3F, "travel" to 0xFF17614A, "gifts" to 0xFF8C3B4E, "entertainment" to 0xFF55529A,
    "sub_one" to 0xFF7B4B7F, "personal" to 0xFFA0522D, "health" to 0xFF8C3B4E, "other" to 0xFF8B9096,
)
val PALETTE = listOf(
    0xFF17614A, 0xFF3D5A80, 0xFFB07D2B, 0xFF7B4B7F, 0xFFA0522D, 0xFF4A7C8C,
    0xFF6B7F3A, 0xFF8C3B4E, 0xFF5B6470, 0xFF2F7D6D, 0xFF9C6B3F, 0xFF55529A, 0xFF8B9096,
)

fun defaultCats(): Map<String, Cat> = CAT_DEFS.associate { (id, name, _) -> id to Cat(name, DEFAULT_COLOR[id] ?: PALETTE[0]) }

/* ---------- Auto-category rules (ported from the web app) ---------- */

private val RULES: List<Pair<Regex, String>> = listOf(
    """\bpg rent\b|^ ?rent\b|house rent""" to "rent",
    "physio bill" to "physio_bill",
    "physio consult|phsio consult" to "physio_consult",
    "new sim" to "personal",
    "recharg" to "recharge",
    """flight|airport|railway|bus stand|\btrain\b|seat selection|clock ?room""" to "travel",
    "electricity" to "electricity",
    "laundry" to "laundry",
    "youtube|netflix|prime video|hotstar|spotify|jiocinema" to "sub_rec",
    "subscription|bumble" to "sub_one",
    "saloon|salon|haircut" to "salon",
    "gift|donation|party|sweets|for sister|for mom|for grandma|for dad|amazon for" to "gifts",
    """courier|charges|repair|\bmisc\b""" to "personal",
    "fruit|snack|banana|cool drink|protein bar|juice" to "snacks",
    "instamart|zepto|blinkit|bigbasket|dmart|grocer" to "grocery",
    "swiggy" to "swiggy",
    "zomato" to "zomato",
    "lunch" to "lunch",
    " to " to "_ride",
    "restaurant|restro|food with|food outside|food at|dinner|mcdonald|kfc|bistro|cafe|eating out|pizza|domino|burger" to "eatout",
    "movie|entertainment|theat|concert" to "entertainment",
    """bought|case\b|protector|mask|membrane|cord|dress|shoe|pant|shirt|boxer|bottle|power bank|charger|earphone""" to "personal",
    """medicine|clinic|pharma|\btest\b|doctor|hospital""" to "health",
).map { Regex(it.first) to it.second }

fun categorize(desc: String): String {
    val d = " " + desc.lowercase().replace(Regex("\\s+"), " ").trim() + " "
    for ((re, cat) in RULES) {
        if (re.containsMatchIn(d)) {
            if (cat != "_ride") return cat
            if (Regex("physio|phsio").containsMatchIn(d)) return "t_physio"
            if (Regex("""\bofc\b|office""").containsMatchIn(d)) return "t_office"
            return "t_local"
        }
    }
    return "other"
}

/* ---------- Helpers over the data ---------- */

fun LedgerData.resolve(idIn: String?): String {
    var id = idIn
    var n = 0
    while (id != null && !cats.containsKey(id) && alias.containsKey(id) && n++ < 20) id = alias[id]
    return if (id != null && cats.containsKey(id)) id else "other"
}

fun LedgerData.cat(id: String): Cat = cats[resolve(id)] ?: Cat("Other", 0xFF8B9096)

fun LedgerData.autoCat(desc: String): String {
    val d = desc.lowercase()
    for ((id, c) in cats) if (c.kw.any { it.isNotBlank() && d.contains(it.lowercase()) }) return id
    return resolve(categorize(desc))
}

fun LedgerData.incomeFor(k: String): Plan {
    val m = months[k] ?: return plan.copy(bills = plan.billList)
    return m.copy(bills = m.bills ?: plan.billList)
}

fun LedgerData.billOf(e: Entry): NamedAmount? {
    val d = e.desc.lowercase()
    return incomeFor(e.date.take(7)).billList.firstOrNull { it.name.isNotBlank() && d.contains(it.name.lowercase().trim()) }
}

fun LedgerData.bucketOf(e: Entry): Bucket = e.bucket ?: if (billOf(e) != null) Bucket.FIXED else Bucket.RECURRING

fun LedgerData.inMonth(k: String) = entries.filter { it.date.startsWith(k) }

fun LedgerData.monthsWithData(): List<String> = entries.map { it.date.take(7) }.distinct().sorted()

fun LedgerData.deliveryIds(): List<String> = listOf("swiggy", "zomato").map { resolve(it) }.distinct().filter { it != "other" }

data class Week(val a: Int, val z: Int, val rec: Double, val del: Double)
data class BillStatus(val name: String, val amount: Double, val paid: Double)

class MonthStats(
    val key: String,
    val es: List<Entry>,
    val total: Double,
    val by: Map<String, Double>,
    val cnt: Map<String, Int>,
    val b: Map<Bucket, Double>,
    val delivN: Int,
    val delivSum: Double,
    val inc: Plan,
    val saved: Double,
    val bills: List<BillStatus>,
    val pendingList: List<BillStatus>,
    val net: Double,
    val weeks: List<Week>,
    private val g: (List<String>) -> Double,
) {
    fun group(vararg ids: String) = g(ids.toList())
    val food get() = group("swiggy", "zomato", "lunch", "eatout", "snacks")
    val officeT get() = group("t_office")
    val physioT get() = group("t_physio")
    val grocery get() = group("grocery")
    val elecLaundry get() = group("electricity", "laundry")
    val recharge get() = group("recharge")
}

fun LedgerData.stats(k: String): MonthStats {
    val es = inMonth(k)
    val dl = deliveryIds()
    val by = HashMap<String, Double>()
    val cnt = HashMap<String, Int>()
    es.forEach { val c = resolve(it.cat); by[c] = (by[c] ?: 0.0) + it.amount; cnt[c] = (cnt[c] ?: 0) + 1 }
    val b = HashMap<Bucket, Double>().apply { Bucket.entries.forEach { put(it, 0.0) } }
    es.forEach { b[bucketOf(it)] = b[bucketOf(it)]!! + it.amount }
    val deliv = es.filter { dl.contains(resolve(it.cat)) }
    val inc = incomeFor(k)
    val saved = inc.saves.sumOf { it.amount }
    val total = es.sumOf { it.amount }
    val g: (List<String>) -> Double = { ids ->
        ids.map { resolve(it) }.distinct().filter { it != "other" || ids.contains("other") }.sumOf { by[it] ?: 0.0 }
    }
    val dim = YearMonth.parse(k).lengthOfMonth()
    val weeks = listOf(1 to 7, 8 to 14, 15 to 21, 22 to 31).map { (a, z) ->
        val w = es.filter { val d = it.date.substring(8).toInt(); d in a..z }
        val r = w.filter { bucketOf(it) == Bucket.RECURRING }
        Week(a, min(z, dim), r.sumOf { it.amount }, r.filter { dl.contains(resolve(it.cat)) }.sumOf { it.amount })
    }
    val bills = inc.billList.filter { it.name.isNotBlank() }.map { x ->
        val nm = x.name.lowercase().trim()
        BillStatus(x.name, x.amount, es.filter { it.desc.lowercase().contains(nm) }.sumOf { it.amount })
    }
    val pendingList = if (k >= thisMonth()) bills.filter { it.paid == 0.0 && it.amount > 0 } else emptyList()
    val pending = pendingList.sumOf { it.amount }
    return MonthStats(
        k, es, total, by, cnt, b, deliv.size, deliv.sumOf { it.amount }, inc, saved, bills, pendingList,
        inc.salary - saved - total - pending, weeks, g,
    )
}

fun LedgerData.recurringBy(st: MonthStats): Map<String, Double> {
    val o = HashMap<String, Double>()
    st.es.forEach { if (bucketOf(it) == Bucket.RECURRING) { val c = resolve(it.cat); o[c] = (o[c] ?: 0.0) + it.amount } }
    return o
}

/** Expenses used in the last 120 days, for ordering category chips. */
fun LedgerData.usage(): Map<String, Int> {
    val cut = LocalDate.now().minusDays(120).toString()
    val n = HashMap<String, Int>()
    entries.forEach { if (it.date >= cut) { val c = resolve(it.cat); n[c] = (n[c] ?: 0) + 1 } }
    return n
}

data class Suggestion(val desc: String, val n: Int, val amt: Double, val cat: String, val date: String)

fun LedgerData.suggestions(qIn: String): List<Suggestion> {
    val map = LinkedHashMap<String, Suggestion>()
    for (e in entries) {
        if (e.desc == "(unlabelled)") continue
        val k = e.desc.lowercase()
        val m = map[k]
        map[k] = when {
            m == null -> Suggestion(e.desc, 1, e.amount, e.cat, e.date)
            e.date >= m.date -> m.copy(n = m.n + 1, amt = e.amount, cat = e.cat, date = e.date)
            else -> m.copy(n = m.n + 1)
        }
    }
    val q = qIn.trim().lowercase()
    return map.values.filter { q.isEmpty() || (it.desc.lowercase().contains(q) && it.desc.lowercase() != q) }
        .sortedByDescending { it.n }.take(6)
}

/* ---------- Migration + JSON (same format as the web app's backup) ---------- */

fun thisMonth(): String = YearMonth.now().toString()
fun today(): String = LocalDate.now().toString()
fun uid(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + java.lang.Long.toString((Math.random() * 1e10).roundToLong(), 36).take(6)

private fun JSONObject.dbl(k: String): Double {
    val v = opt(k) ?: return 0.0
    return when (v) {
        is Number -> v.toDouble()
        is String -> v.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

private fun parseNamed(a: JSONArray?): List<NamedAmount> {
    if (a == null) return emptyList()
    return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { NamedAmount(it.optString("name", ""), it.dbl("amount")) } }
}

private fun parsePlan(o: JSONObject?): Plan? {
    if (o == null) return null
    return Plan(o.dbl("salary"), parseNamed(o.optJSONArray("saves")), if (o.has("bills")) parseNamed(o.optJSONArray("bills")) else null)
}

private fun parseColor(s: String?): Long {
    if (s == null) return PALETTE[0]
    val h = s.removePrefix("#")
    return try { (0xFF000000 or h.take(6).toLong(16)) } catch (e: Exception) { PALETTE[0] }
}

fun colorHex(c: Long) = "#" + String.format("%06x", c and 0xFFFFFF)

fun parseData(json: JSONObject): LedgerData {
    val catsO = json.optJSONObject("cats")
    val cats = LinkedHashMap<String, Cat>()
    val legacyBucket = HashMap<String, String>()
    if (catsO != null) {
        for (id in catsO.keys()) {
            val c = catsO.getJSONObject(id)
            val kwA = c.optJSONArray("kw")
            cats[id] = Cat(
                c.optString("name", id), parseColor(c.optString("color", null)),
                if (kwA == null) emptyList() else (0 until kwA.length()).map { kwA.optString(it) },
            )
            if (c.has("bucket")) legacyBucket[id] = c.optString("bucket")
        }
    } else cats.putAll(defaultCats())
    if (!cats.containsKey("other")) cats["other"] = Cat("Other", 0xFF8B9096)
    val aliasO = json.optJSONObject("alias")
    val alias = HashMap<String, String>()
    aliasO?.keys()?.forEach { alias[it] = aliasO.optString(it) }
    val ver = json.optInt("ver", 1)
    val base = LedgerData(cats = cats, alias = alias)
    val arr = json.optJSONArray("entries") ?: JSONArray()
    val entries = ArrayList<Entry>(arr.length())
    for (i in 0 until arr.length()) {
        val e = arr.optJSONObject(i) ?: continue
        val catRaw = e.optString("cat", "other")
        val cat = if (cats.containsKey(catRaw)) catRaw else base.resolve(catRaw)
        var bucket = Bucket.of(e.optString("bucket", ""))
        if (bucket == null && ver < 3) {
            val lb = legacyBucket[cat]
            if (lb != null && lb != "recurring") bucket = Bucket.of(lb)
        }
        entries.add(
            Entry(
                e.optString("id", uid() + i), e.optString("date"), e.optString("desc", "(unlabelled)"),
                e.dbl("amount"), cat, bucket, e.optLong("created", 0L),
            )
        )
    }
    val monthsO = json.optJSONObject("months")
    val months = HashMap<String, Plan>()
    monthsO?.keys()?.forEach { k -> parsePlan(monthsO.optJSONObject(k))?.let { months[k] = it } }
    val bo = json.optJSONObject("budgets")
    return base.copy(
        entries = entries,
        plan = parsePlan(json.optJSONObject("plan")) ?: LedgerData().plan,
        months = months,
        budgets = if (bo == null) Budgets() else Budgets(bo.dbl("recurring"), bo.dbl("delivery"), bo.dbl("oneoff")),
    )
}

private fun namedJson(l: List<NamedAmount>) = JSONArray().apply {
    l.forEach { put(JSONObject().put("name", it.name).put("amount", it.amount)) }
}

private fun planJson(p: Plan) = JSONObject().put("salary", p.salary).put("saves", namedJson(p.saves)).apply { p.bills?.let { put("bills", namedJson(it)) } }

fun LedgerData.toJson(): JSONObject {
    val o = JSONObject()
    o.put("ver", 3)
    o.put("entries", JSONArray().apply {
        entries.forEach { e ->
            put(JSONObject().put("id", e.id).put("date", e.date).put("desc", e.desc).put("amount", e.amount)
                .put("cat", e.cat).put("created", e.created).apply { e.bucket?.let { put("bucket", it.key) } })
        }
    })
    o.put("cats", JSONObject().apply {
        cats.forEach { (id, c) -> put(id, JSONObject().put("name", c.name).put("color", colorHex(c.color)).put("kw", JSONArray(c.kw))) }
    })
    o.put("alias", JSONObject(alias as Map<*, *>))
    o.put("plan", planJson(plan))
    o.put("months", JSONObject().apply { months.forEach { (k, p) -> put(k, planJson(p)) } })
    o.put("budgets", JSONObject().put("recurring", budgets.recurring).put("delivery", budgets.delivery).put("oneoff", budgets.oneoff))
    return o
}
