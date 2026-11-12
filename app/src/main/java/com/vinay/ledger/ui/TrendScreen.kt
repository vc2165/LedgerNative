@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinay.ledger.AppState
import com.vinay.ledger.Tab
import com.vinay.ledger.data.Bucket
import com.vinay.ledger.data.MonthStats
import com.vinay.ledger.data.YearMonthDays
import com.vinay.ledger.data.cat
import com.vinay.ledger.data.inr
import com.vinay.ledger.data.mLong
import com.vinay.ledger.data.mShort
import com.vinay.ledger.data.monthsWithData
import com.vinay.ledger.data.recurringBy
import com.vinay.ledger.data.stats
import com.vinay.ledger.data.thisMonth
import java.time.LocalDate
import kotlin.math.abs

private fun avg(l: List<Double>) = if (l.isEmpty()) 0.0 else l.sum() / l.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(s: AppState, modifier: Modifier) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val d = s.data
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Trend") }, scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        val ms = remember(d) { d.monthsWithData() }
        if (ms.isEmpty()) {
            Column(Modifier.padding(pad)) { SectionCard { Text("No history yet", style = MaterialTheme.typography.titleMedium); Muted("Add expenses to see month-by-month trends.") } }
        } else TrendBody(s, pad, ms)
    }
}

@Composable
private fun TrendBody(s: AppState, pad: PaddingValues, ms: List<String>) {
    val d = s.data
    run {
        val last = ms.takeLast(6)
        val s6 = remember(d) { last.map { d.stats(it) } }
        val cur = thisMonth()
        val past = last.filter { it != cur }
        val ps = remember(d) { past.map { d.stats(it) } }
        val pr = remember(d) { ps.map { d.recurringBy(it) } }
        val goMonth = { m: String -> s.month = m; s.tab = Tab.REPORT }

        LazyColumn(Modifier.padding(top = pad.calculateTopPadding()), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard(title = "Recurring spend, month by month") {
                    Bars(last.map { mShort(it) }, s6.map { it.b[Bucket.RECURRING]!! }, last.indexOf(cur))
                }
            }
            item {
                SectionCard(title = "This month against your average") {
                    val avgRec = avg(ps.map { it.b[Bucket.RECURRING]!! })
                    if (ms.contains(cur) && ps.isNotEmpty()) {
                        val sc = d.stats(cur)
                        val day = LocalDate.now().dayOfMonth
                        if (day >= 5) {
                            val p = sc.b[Bucket.RECURRING]!! / day * YearMonthDays(cur)
                            val diff = p - avgRec
                            BigDiff(diff)
                            Muted("${mLong(cur)} is heading for ${inr(p)} in recurring spending, ${if (diff > 0) "above" else "below"} your average of ${inr(avgRec)} over the last ${ps.size} month${if (ps.size > 1) "s" else ""}.")
                        } else Muted("It's early in ${mLong(cur)}. From the 5th, this shows how the month compares with your average of ${inr(avgRec)}.")
                    } else if (ps.size >= 2) {
                        val ls = ps.last()
                        val o = avg(ps.dropLast(1).map { it.b[Bucket.RECURRING]!! })
                        val diff = ls.b[Bucket.RECURRING]!! - o
                        BigDiff(diff)
                        Muted("${mLong(past.last())} recurring was ${inr(ls.b[Bucket.RECURRING]!!)}, ${if (diff > 0) "above" else "below"} your earlier average of ${inr(o)}.")
                    } else Muted("Needs at least two months of history.")
                }
            }
            item {
                SectionCard(title = "Best and worst months") {
                    if (ps.size >= 2) {
                        val idx = ps.indices.sortedBy { ps[it].b[Bucket.RECURRING]!! }
                        val lo = idx.first()
                        val hi = idx.last()
                        KvRow("Lowest: ${mLong(past[lo])}", inr(ps[lo].b[Bucket.RECURRING]!!), valueColor = Good) { goMonth(past[lo]) }
                        KvRow("Highest: ${mLong(past[hi])}", inr(ps[hi].b[Bucket.RECURRING]!!), valueColor = Bad) { goMonth(past[hi]) }
                        val others = pr.filterIndexed { i, _ -> i != hi }
                        var drv: String? = null
                        var up = 0.0
                        pr[hi].forEach { (c, v) -> val u = v - avg(others.map { it[c] ?: 0.0 }); if (u > up) { up = u; drv = c } }
                        drv?.let { Muted("${mLong(past[hi])} was mostly pushed up by ${d.cat(it).name}, ${inr(up)} more than its usual month.", Modifier.padding(top = 6.dp)) }
                    } else Muted("Needs at least two months of history.")
                }
            }
            item {
                SectionCard(title = "Changes to watch") {
                    if (pr.size >= 3) {
                        val l3 = pr.takeLast(3)
                        Muted("Your last three months: ${past.takeLast(3).joinToString(", ") { mShort(it) }}")
                        val catsIds = l3.flatMap { it.keys }.distinct()
                        val ups = ArrayList<Pair<String, List<Double>>>()
                        val downs = ArrayList<Pair<String, List<Double>>>()
                        catsIds.forEach { c ->
                            val v = l3.map { it[c] ?: 0.0 }
                            if (v[0] < v[1] && v[1] < v[2] && v[2] - v[0] >= 300) ups += c to v
                            if (v[0] > v[1] && v[1] > v[2] && v[0] - v[2] >= 300) downs += c to v
                        }
                        ups.sortByDescending { it.second[2] - it.second[0] }
                        downs.sortBy { it.second[2] - it.second[0] }
                        if (ups.isNotEmpty()) { SubHead("Rising every month"); ups.take(4).forEach { (c, v) -> KvRow(d.cat(c).name, v.joinToString(" → ") { inr(it) } + " ▲", valueColor = Bad) } }
                        if (downs.isNotEmpty()) { SubHead("Falling every month"); downs.take(4).forEach { (c, v) -> KvRow(d.cat(c).name, v.joinToString(" → ") { inr(it) } + " ▼", valueColor = Good) } }
                        if (ups.isEmpty() && downs.isEmpty()) Muted("No category has gone steadily up or down over these three months.", Modifier.padding(top = 6.dp))
                    } else Muted("Needs at least three months of history.")
                }
            }
            item {
                SectionCard(title = "Your averages") {
                    KvRow("Recurring per month", inr(avg(s6.map { it.b[Bucket.RECURRING]!! })))
                    KvRow("One-offs per month", inr(avg(s6.map { it.b[Bucket.ONEOFF]!! })))
                    KvRow("Food delivery per month", "${inr(avg(s6.map { it.delivSum }))}, ${"%.0f".format(avg(s6.map { it.delivN.toDouble() }))} orders")
                }
            }
            item {
                SectionCard(title = "Month by month") {
                    MonthTable(last, s6, goMonth)
                    Muted("Tap a month to open its report.", Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun BigDiff(diff: Double) {
    Text(
        (if (diff > 0) "▲ " else "▼ ") + inr(abs(diff)),
        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = if (diff > 0) Bad else Good,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun Bars(labels: List<String>, values: List<Double>, highlight: Int) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(values) { p.snapTo(0f); p.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Row(Modifier.fillMaxWidth().height(180.dp).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (v >= 1000) "${"%.1f".format(v / 1000)}k" else v.toInt().toString(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight((v / max).toFloat() * 0.82f * p.value + 0.01f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (i == highlight) Purple else Purple.copy(alpha = .35f))
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun MonthTable(months: List<String>, st: List<MonthStats>, goMonth: (String) -> Unit) {
    val rows: List<Pair<String, (MonthStats) -> Double>> = listOf(
        "Total spent" to { it.total }, "Fixed" to { it.b[Bucket.FIXED]!! }, "Recurring" to { it.b[Bucket.RECURRING]!! },
        "One-offs" to { it.b[Bucket.ONEOFF]!! }, "Food, all" to { it.food }, "Swiggy and Zomato" to { it.delivSum },
        "Office transport" to { it.officeT }, "Physio transport" to { it.physioT }, "Groceries" to { it.grocery },
        "Electricity and laundry" to { it.elecLaundry }, "Recharges" to { it.recharge }, "Saved" to { it.saved },
        "Left after savings" to { it.net },
    )
    Row {
        Column(Modifier.width(150.dp)) {
            Text("", Modifier.padding(vertical = 8.dp))
            rows.forEachIndexed { i, (l, _) ->
                Text(l, Modifier.padding(vertical = 7.dp), fontSize = 13.5.sp, maxLines = 1, fontWeight = if (i == rows.size - 1) FontWeight.SemiBold else null)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            months.forEachIndexed { mi, m ->
                Column(Modifier.width(84.dp), horizontalAlignment = Alignment.End) {
                    Text(mShort(m), Modifier.clip(RoundedCornerShape(8.dp)).clickable { goMonth(m) }.padding(vertical = 8.dp, horizontal = 4.dp),
                        color = Purple, fontWeight = FontWeight.SemiBold)
                    rows.forEachIndexed { i, (_, f) ->
                        val v = f(st[mi])
                        Text(inr(v), Modifier.padding(vertical = 7.dp), fontSize = 13.5.sp, maxLines = 1,
                            color = if (i == rows.size - 1 && v < 0) Bad else Color.Unspecified,
                            fontWeight = if (i == rows.size - 1) FontWeight.SemiBold else null)
                    }
                }
            }
        }
    }
}
