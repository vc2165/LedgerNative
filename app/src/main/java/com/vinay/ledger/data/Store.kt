package com.vinay.ledger.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Saves everything to one JSON file in the app's private storage (same shape as a backup). */
class Store(private val ctx: Context) {
    private val file = File(ctx.filesDir, "ledger.json")
    private val prefs = ctx.getSharedPreferences("ledger.settings", Context.MODE_PRIVATE)

    fun load(): LedgerData = try {
        if (file.exists()) parseData(JSONObject(file.readText())) else LedgerData()
    } catch (e: Exception) {
        // keep the broken file aside rather than losing it
        try { file.copyTo(File(ctx.filesDir, "ledger-broken-${System.currentTimeMillis()}.json")) } catch (_: Exception) {}
        LedgerData()
    }

    fun save(d: LedgerData) {
        val tmp = File(ctx.filesDir, "ledger.json.tmp")
        tmp.writeText(d.toJson().toString())
        if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
    }

    var lock: Boolean
        get() = prefs.getBoolean("lock", false)
        set(v) = prefs.edit().putBoolean("lock", v).apply()
    var haptic: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(v) = prefs.edit().putBoolean("haptic", v).apply()
    var remindOn: Boolean
        get() = prefs.getBoolean("remindOn", false)
        set(v) = prefs.edit().putBoolean("remindOn", v).apply()
    var remindHour: Int
        get() = prefs.getInt("remindHour", 21)
        set(v) = prefs.edit().putInt("remindHour", v).apply()
    var remindMinute: Int
        get() = prefs.getInt("remindMinute", 30)
        set(v) = prefs.edit().putInt("remindMinute", v).apply()
}

/* ---------- Formatting ---------- */

private val IN = Locale("en", "IN")
private val nf0 = NumberFormat.getNumberInstance(IN).apply { maximumFractionDigits = 0 }
private val nf2 = NumberFormat.getNumberInstance(IN).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

/** ₹1,23,456 — with paise only when [paise] is true and the amount has them. */
fun inr(n: Double, paise: Boolean = false): String {
    val v = if (paise) Math.round(abs(n) * 100) / 100.0 else abs(n).roundToLong().toDouble()
    val frac = paise && v % 1.0 != 0.0
    return (if (n < -0.004) "−" else "") + "₹" + (if (frac) nf2 else nf0).format(v)
}

fun pct(a: Double, b: Double) = if (b != 0.0) "${Math.round(a / b * 100)}%" else "—"

fun ym(k: String): YearMonth = YearMonth.parse(k)
fun mLong(k: String): String = ym(k).month.getDisplayName(TextStyle.FULL, IN)
fun mShort(k: String): String = ym(k).month.getDisplayName(TextStyle.SHORT, IN)
fun mFull(k: String) = mLong(k) + " " + k.take(4)
fun addMonth(k: String, n: Int): String = ym(k).plusMonths(n.toLong()).toString()
fun day(s: String): LocalDate = LocalDate.parse(s)
fun dayLabel(s: String): String = day(s).format(DateTimeFormatter.ofPattern("EEE, d MMM", IN))
fun dayLabelYear(s: String): String = day(s).format(DateTimeFormatter.ofPattern("d MMM yyyy", IN))
fun shortDate(s: String): String {
    val d = day(s)
    return if (d.year == LocalDate.now().year) d.format(DateTimeFormatter.ofPattern("d MMM", IN)) else d.format(DateTimeFormatter.ofPattern("d MMM yyyy", IN))
}

fun parseAmount(s: String): Double? =
    s.replace(Regex("[₹,\\s]|rs\\.?", RegexOption.IGNORE_CASE), "").toDoubleOrNull()?.takeIf { it.isFinite() }

fun r2(n: Double) = Math.round(n * 100) / 100.0

@Suppress("FunctionName")
fun YearMonthDays(k: String): Int = YearMonth.parse(k).lengthOfMonth()
