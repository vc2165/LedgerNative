@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.vinay.ledger.AppState
import com.vinay.ledger.Route
import com.vinay.ledger.data.Bucket
import com.vinay.ledger.data.MonthStats
import com.vinay.ledger.data.bucketOf
import com.vinay.ledger.data.cat
import com.vinay.ledger.data.dayLabel
import com.vinay.ledger.data.inr
import com.vinay.ledger.data.mLong
import com.vinay.ledger.data.mShort
import com.vinay.ledger.data.mFull
import com.vinay.ledger.data.pct
import com.vinay.ledger.data.resolve
import com.vinay.ledger.data.stats

val BucketColor = mapOf(Bucket.FIXED to CFixed, Bucket.RECURRING to Purple, Bucket.ONEOFF to COneoff)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(s: AppState, modifier: Modifier) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { MonthBar(s, scroll, search = false) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        val shift = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.roundToPx() }
        AnimatedContent(
            targetState = s.month,
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(tween(260, easing = EaseStd)) { if (fwd) shift else -shift } + fadeIn(tween(260, easing = EaseStd))) togetherWith
                    fadeOut(tween(90))
            },
            modifier = Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize().monthSwipe(s),
            label = "month",
        ) { k -> MonthReport(s, k) }
    }
}

@Composable
private fun MonthReport(s: AppState, k: String) {
    val d = s.data
    val st = remember(d, k) { d.stats(k) }
    if (st.es.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            SectionCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No expenses in ${mLong(k)}", style = MaterialTheme.typography.titleMedium)
                    Muted("Pick another month, or add expenses on the Ledger tab.")
                }
            }
        }
        return
    }
    val total = st.total
    val cats = st.by.entries.sortedByDescending { it.value }
    val top = cats.take(6).map { Triple(d.cat(it.key).name, it.value, d.catColor(it.key)) }.toMutableList()
    if (cats.size > 6) top += Triple("Everything else", cats.drop(6).sumOf { it.value }, Color(0xFFC9CCC6))
    var full by remember { mutableStateOf(false) }

    LazyColumn(flingBehavior = rememberCalmFling(), contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Donut(top.map { it.second to it.third }, total, Modifier.size(150.dp))
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        top.forEach { (nm, v, col) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(col))
                                Spacer(Modifier.width(8.dp))
                                Text(nm, Modifier.weight(1f), fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(pct(v, total), fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Fixed, recurring and one-off") {
                SplitBar(Bucket.entries.map { (st.b[it] ?: 0.0) to BucketColor[it]!! }, total)
                Spacer(Modifier.height(6.dp))
                Bucket.entries.forEach { b ->
                    KvRow(b.label, inr(st.b[b]!!), dot = BucketColor[b], sub = pct(st.b[b]!!, total)) { s.tick(); s.push(Route.TypeList(k, b)) }
                }
            }
        }
        item {
            SectionCard(title = "Money in and out") {
                KvRow("Salary", inr(st.inc.salary))
                st.inc.saves.forEach { KvRow(it.name, "−" + inr(it.amount)) }
                KvRow("Spent", "−" + inr(total))
                st.pendingList.forEach { KvRow("${it.name} (not paid yet)", "−" + inr(it.amount)) }
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                KvRow("Left after savings" + if (st.bills.isNotEmpty()) " and bills" else "", inr(st.net), bold = true, valueColor = if (st.net < 0) Bad else Good)
                TextButton(onClick = { s.push(Route.PlanEdit(k)) }) { Text("Change salary, savings or bills for ${mLong(k)}") }
            }
        }
        item {
            SectionCard(title = "Food delivery") {
                KvRow("Orders", st.delivN.toString())
                KvRow("Total", inr(st.delivSum))
                KvRow("Average order", if (st.delivN > 0) inr(st.delivSum / st.delivN) else "—")
            }
        }
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { full = !full }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Full report", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(if (full) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Purple)
                }
                AnimatedVisibility(full, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { FullReport(s, k, st) }
            }
        }
    }
}

@Composable
private fun FullReport(s: AppState, k: String, st: MonthStats) {
    val d = s.data
    val total = st.total
    fun c(id: String) = d.cat(id).name
    fun by(id: String) = st.group(id)
    fun n(id: String) = st.cnt[d.resolve(id)] ?: 0
    Column {
        SubHead("All categories")
        Table(listOf("Category", "Entries", "Amount", "Share"),
            st.by.entries.sortedByDescending { it.value }.map { listOf(c(it.key), (st.cnt[it.key] ?: 0).toString(), inr(it.value), pct(it.value, total)) } +
                listOf(listOf("Total", st.es.size.toString(), inr(total), "100%")), boldLast = true)
        SubHead("Food split")
        Table(listOf("Sub-category", "Amount"),
            listOf("zomato", "swiggy", "lunch", "eatout", "snacks").map { listOf(c(it), inr(by(it))) } + listOf(listOf("Total food", inr(st.food))), boldLast = true)
        Muted("Groceries (${inr(st.grocery)}) are kept outside food.")
        SubHead("Food delivery by app")
        Table(listOf("", "Orders", "Total", "Average"),
            listOf("zomato", "swiggy").map { listOf(c(it), n(it).toString(), inr(by(it)), if (n(it) > 0) inr(by(it) / n(it)) else "—") })
        SubHead("Transport")
        Table(listOf("Type", "Amount"),
            listOf("t_office", "t_physio", "t_local").map { listOf(c(it), inr(by(it))) } + listOf(listOf("Total", inr(st.group("t_office", "t_physio", "t_local")))), boldLast = true)
        SubHead("Week by week, recurring only")
        Table(listOf("Week", "Recurring", "Delivery"),
            st.weeks.map { listOf("${it.a}–${it.z} ${mShort(k)}", inr(it.rec), inr(it.del)) } +
                listOf(listOf("Total", inr(st.b[Bucket.RECURRING]!!), inr(st.delivSum))), boldLast = true)
        SubHead("One-offs")
        val one = st.es.filter { d.bucketOf(it) == Bucket.ONEOFF }.sortedByDescending { it.amount }
        if (one.isEmpty()) Muted("No one-offs this month.")
        else Table(listOf("Item", "Date", "Amount"),
            one.map { listOf(it.desc, "${it.date.takeLast(2).toInt()} ${mShort(k)}", inr(it.amount)) } + listOf(listOf("Total", "", inr(st.b[Bucket.ONEOFF]!!))), boldLast = true)
    }
}

@Composable
fun SubHead(t: String) = Text(t, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

@Composable
fun Table(head: List<String>, rows: List<List<String>>, boldLast: Boolean = false) {
    Column {
        Row(Modifier.padding(vertical = 6.dp)) {
            head.forEachIndexed { i, h ->
                Text(h, Modifier.weight(if (i == 0) 1.6f else 1f), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (i == 0) null else androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        rows.forEachIndexed { ri, r ->
            val bold = boldLast && ri == rows.size - 1
            Row(Modifier.padding(vertical = 7.dp)) {
                r.forEachIndexed { i, v ->
                    Text(v, Modifier.weight(if (i == 0) 1.6f else 1f), fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (bold) FontWeight.SemiBold else null,
                        textAlign = if (i == 0) null else androidx.compose.ui.text.style.TextAlign.End)
                }
            }
            if (ri < rows.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        }
    }
}

/** Donut chart that sweeps in. */
@Composable
fun Donut(parts: List<Pair<Double, Color>>, total: Double, modifier: Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(parts) { p.snapTo(0f); p.animateTo(1f, tween(700, easing = EaseStd)) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(450, easing = EaseStd)) }
    Box(modifier.graphicsLayer { val k = pop.value; scaleX = 0.88f + 0.12f * k; scaleY = scaleX; rotationZ = -20f * (1 - k); alpha = k }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 18.dp.toPx()
            val r = size.minDimension / 2 - sw / 2
            val tl = Offset(center.x - r, center.y - r)
            val sz = Size(r * 2, r * 2)
            drawArc(Color(0xFFEEF0EC), 0f, 360f, false, tl, sz, style = Stroke(sw))
            var start = -90f
            parts.forEach { (v, col) ->
                val sweep = (v / total * 360f).toFloat()
                val drawn = (sweep * p.value - 1.2f).coerceAtLeast(0f)
                drawArc(col, start + 360f * 0f, drawn, false, tl, sz, style = Stroke(sw))
                start += sweep * p.value
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(inr(total), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text("spent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SplitBar(parts: List<Pair<Double, Color>>, total: Double) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(parts) { p.animateTo(1f, tween(700, easing = EaseStd)) }
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        parts.filter { it.first > 0 }.forEach { (v, col) ->
            Box(Modifier.weight((v / total).toFloat() * p.value + 0.0001f).fillMaxSize().background(col))
        }
        if (p.value < 1f) Box(Modifier.weight(1f - p.value + 0.0001f))
    }
}

/* ---------- All expenses of one type in a month ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeListScreen(s: AppState, k: String, b: Bucket) {
    val d = s.data
    val st = remember(d, k) { d.stats(k) }
    val es = st.es.filter { d.bucketOf(it) == b }.sortedWith(compareByDescending<com.vinay.ledger.data.Entry> { it.date }.thenByDescending { it.created })
    val days = es.groupBy { it.date }
    val hint = when (b) {
        Bucket.FIXED -> "Rent, bills and anything you marked Fixed."
        Bucket.RECURRING -> "Everyday spending, everything not marked Fixed or One-off."
        Bucket.ONEOFF -> "Things you marked One-off."
    }
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(Modifier.nestedScroll(scroll.nestedScrollConnection), topBar = { SubBar("${b.label}, ${mShort(k)}", { s.pop() }, scroll) }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard {
                    KvRow("${es.size} expense${if (es.size == 1) "" else "s"}", inr(st.b[b]!!, true), bold = true, sub = pct(st.b[b]!!, st.total))
                    Muted("$hint Tap one to edit or change its type.")
                }
            }
            if (es.isEmpty()) item { SectionCard { Muted("No ${b.label.lowercase()} expenses in ${mLong(k)}.") } }
            days.forEach { (date, l) ->
                item(key = date) {
                    SectionCard {
                        Row(Modifier.fillMaxWidth()) {
                            Text(dayLabel(date), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(inr(l.sumOf { it.amount }, true), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        l.forEach { e -> EntryRow(s, e) { s.push(Route.EntryEdit(e.id)) } }
                    }
                }
            }
        }
    }
}

/* ---------- Search ---------- */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(s: AppState) {
    val d = s.data
    var q by remember { mutableStateOf("") }
    var allTime by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(250); try { fr.requestFocus() } catch (_: Exception) {} }
    val results = remember(d, q, allTime, picked, s.month) {
        val t = q.trim().lowercase()
        val amt = t.replace(Regex("[₹,]"), "").toDoubleOrNull()
        d.entries.filter { e ->
            (allTime || e.date.startsWith(s.month)) && (picked.isEmpty() || picked.contains(d.resolve(e.cat))) &&
                (t.isEmpty() || e.desc.lowercase().contains(t) || d.cat(e.cat).name.lowercase().contains(t) || (amt != null && Math.round(e.amount) == Math.round(amt)))
        }.sortedWith(compareByDescending<com.vinay.ledger.data.Entry> { it.date }.thenByDescending { it.created })
    }
    Scaffold(topBar = { SubBar("Search", { s.pop() }) }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    TextField(
                        q, { q = it }, Modifier.fillMaxWidth().focusRequester(fr),
                        placeholder = { Text("Description, category or amount") }, singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.Search, null) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(allTime, { allTime = true }, { Text("All time") })
                        FilterChip(!allTime, { allTime = false }, { Text(mFull(s.month)) })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.cats.entries.sortedBy { it.value.name.lowercase() }.forEach { (id, c) ->
                            FilterChip(picked.contains(id), { picked = if (picked.contains(id)) picked - id else picked + id }, { Text(c.name) },
                                leadingIcon = { Icon(d.catIcon(id), null, Modifier.size(18.dp), tint = d.catColor(id)) })
                        }
                    }
                }
            }
            if (q.isBlank() && picked.isEmpty()) {
                item { Muted("Type to search, or pick categories to filter.", Modifier.padding(16.dp)) }
            } else {
                item {
                    SectionCard { KvRow("${results.size} expense${if (results.size == 1) "" else "s"}", inr(results.sumOf { it.amount }, true), bold = true) }
                }
                items(results.take(200), key = { it.id }) { e ->
                    Box(Modifier.padding(horizontal = 16.dp)) { EntryRow(s, e, showDate = true) { s.push(Route.EntryEdit(e.id)) } }
                }
            }
        }
    }
}
