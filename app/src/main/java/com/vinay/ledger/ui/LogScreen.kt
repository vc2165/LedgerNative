package com.vinay.ledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinay.ledger.AppState
import com.vinay.ledger.Route
import com.vinay.ledger.data.Bucket
import com.vinay.ledger.data.Entry
import com.vinay.ledger.data.MonthStats
import com.vinay.ledger.data.YearMonthDays
import com.vinay.ledger.data.addMonth
import com.vinay.ledger.data.bucketOf
import com.vinay.ledger.data.cat
import com.vinay.ledger.data.dayLabel
import com.vinay.ledger.data.inr
import com.vinay.ledger.data.mLong
import com.vinay.ledger.data.monthsWithData
import com.vinay.ledger.data.stats
import com.vinay.ledger.data.thisMonth
import java.time.LocalDate

/** Month title + arrows, shared by Ledger and Report. Swiping sideways also changes month. */
fun Modifier.monthSwipe(s: AppState): Modifier = this.pointerInput(Unit) {
    var dx = 0f
    detectHorizontalDragGestures(
        onDragStart = { dx = 0f },
        onDragEnd = { if (dx < -140) shiftMonth(s, 1) else if (dx > 140) shiftMonth(s, -1) },
        onHorizontalDrag = { _, d -> dx += d },
    )
}

fun shiftMonth(s: AppState, n: Int) { s.tick("tick"); s.month = addMonth(s.month, n) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthBar(s: AppState, scroll: androidx.compose.material3.TopAppBarScrollBehavior, search: Boolean) {
    LargeTopAppBar(
        title = {
            Column {
                Text(mLong(s.month) + if (s.month.take(4) != thisMonth().take(4)) " " + s.month.take(4) else "")
            }
        },
        actions = {
            IconButton(onClick = { shiftMonth(s, -1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            IconButton(onClick = { shiftMonth(s, 1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
            if (search) IconButton(onClick = { s.push(Route.Search) }) { Icon(Icons.Filled.Search, "Search") }
        },
        scrollBehavior = scroll,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(s: AppState, modifier: Modifier, onFab: (Boolean) -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { MonthBar(s, scroll, search = true) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        var prev by remember { mutableStateOf(s.month) }
        AnimatedContent(
            targetState = s.month,
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(tween(280)) { if (fwd) it / 4 else -it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (fwd) -it / 4 else it / 4 } + fadeOut(tween(160)))
            },
            modifier = Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize().monthSwipe(s),
            label = "month",
        ) { month -> MonthLog(s, month, onFab) }
        LaunchedEffect(s.month) { prev = s.month }
    }
}

@Composable
private fun MonthLog(s: AppState, k: String, onFab: (Boolean) -> Unit) {
    val d = s.data
    val st = remember(d, k) { d.stats(k) }
    val days = remember(st) {
        st.es.groupBy { it.date }.toSortedMap(compareByDescending { it }).map { (date, l) -> date to l.sortedByDescending { it.created } }
    }
    val list = rememberLazyListState()
    // Shrink the Add button while scrolling down, grow it back when scrolling up
    LaunchedEffect(list) {
        var last = 0
        snapshotFlow { list.firstVisibleItemIndex * 10000 + list.firstVisibleItemScrollOffset }.collect { y ->
            if (y > last + 20) onFab(false) else if (y < last - 20 || y < 50) onFab(true)
            last = y
        }
    }
    LazyColumn(state = list, contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.fillMaxSize()) {
        item(key = "hero") { Hero(s, st) }
        if (k == thisMonth()) {
            val notes = monthNotes(s, st)
            if (notes.isNotEmpty()) item(key = "notes") {
                SectionCard {
                    notes.forEachIndexed { i, n ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                        Text(n, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (days.isEmpty()) {
            item(key = "empty") {
                SectionCard {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing logged in ${mLong(k)}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Muted(if (d.entries.isNotEmpty()) "Add an expense to start this month." else "Add your first expense, or restore a backup from Settings.")
                    }
                }
            }
        }
        days.forEach { (date, es) ->
            item(key = "d$date") { DayCard(s, date, es) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(s: AppState, st: MonthStats) {
    val b = s.data.budgets
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(PurpleDeep, Purple, Color(0xFF6A3FD6)))).padding(20.dp)
        ) {
            Text("Spent this month", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
            val shown by animateFloatAsState(st.total.toFloat(), tween(450), label = "total")
            Text(inr(shown.toDouble()), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val f = { used: Double, bud: Double -> if (bud > 0) (used / bud).toFloat() else null }
                        val of = { bud: Double -> if (bud > 0) "of ${inr(bud)}" else "No budget" }
                        Meter("Recurring", inr(st.b[Bucket.RECURRING]!!), f(st.b[Bucket.RECURRING]!!, b.recurring), of(b.recurring), Modifier.weight(1f))
                        Meter("Delivery", inr(st.delivSum), f(st.delivSum, b.delivery), of(b.delivery), Modifier.weight(1f))
                        Meter("One-offs", inr(st.b[Bucket.ONEOFF]!!), f(st.b[Bucket.ONEOFF]!!, b.oneoff), of(b.oneoff), Modifier.weight(1f))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Left after savings" + if (st.bills.isNotEmpty()) " and bills" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(inr(st.net), fontWeight = FontWeight.SemiBold, color = if (st.net < 0) Bad else Good)
                    }
                    if (st.pendingList.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Muted("Not paid yet, already counted above")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            st.pendingList.forEach { bill ->
                                AssistChip(
                                    onClick = { s.push(Route.EntryEdit(null, preDesc = bill.name, preAmt = bill.amount, preFixed = true)) },
                                    label = { Text("${bill.name}  ${inr(bill.amount)}") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun monthNotes(s: AppState, st: MonthStats): List<String> {
    val d = s.data
    if (st.es.isEmpty()) return emptyList()
    val today = LocalDate.now().dayOfMonth
    val n = YearMonthDays(st.key)
    val out = ArrayList<String>()
    val rec = st.b[Bucket.RECURRING]!!
    val bud = d.budgets.recurring
    if (today >= 5 && rec > 0) {
        val p = rec / today * n
        out += "At this pace recurring spend ends near ${inr(p)}" +
            (if (bud > 0) (if (p > bud) ", ${inr(p - bud)} over budget." else ", ${inr(bud - p)} under budget.") else ".")
    }
    if (st.delivN > 0) out += "${st.delivN} delivery order${if (st.delivN > 1) "s" else ""} so far, averaging ${inr(st.delivSum / st.delivN)}."
    val prev = (1..3).map { d.stats(addMonth(st.key, -it)) }.filter { it.es.isNotEmpty() }
    if (prev.isNotEmpty() && today >= 10) {
        val avg = prev.sumOf { it.b[Bucket.RECURRING]!! } / prev.size
        val p = rec / today * n
        out += "Your recent months average ${inr(avg)} recurring; this month is tracking " +
            (if (p > avg) "${inr(p - avg)} higher." else "${inr(avg - p)} lower.")
    }
    return out
}

@Composable
private fun DayCard(s: AppState, date: String, es: List<Entry>) {
    SectionCard {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(dayLabel(date), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(inr(es.sumOf { it.amount }), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        es.forEachIndexed { i, e ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            androidx.compose.runtime.key(e.id) { SwipeEntry(s, e) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeEntry(s: AppState, e: Entry) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { deleteEntry(s, e); true } else false
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(if (active) Bad else Bad.copy(alpha = .75f), label = "bg")
            val sc by animateFloatAsState(if (active) 1.15f else 0.9f, label = "icon")
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(bg).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Outlined.Delete, "Delete", tint = Color.White, modifier = Modifier.scale(sc))
            }
        },
    ) { EntryRow(s, e) { s.push(Route.EntryEdit(e.id)) } }
}

@Composable
fun EntryRow(s: AppState, e: Entry, showDate: Boolean = false, onClick: () -> Unit) {
    val d = s.data
    val c = d.cat(e.cat)
    val b = d.bucketOf(e)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatAvatar(d.catIcon(e.cat), d.catColor(e.cat))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.desc, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row {
                Text(c.name + if (showDate) ", ${com.vinay.ledger.data.shortDate(e.date)}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (b != Bucket.RECURRING) Text("  ${b.label}", style = MaterialTheme.typography.bodySmall, color = Warn)
            }
        }
        Text(inr(e.amount, true), fontWeight = FontWeight.SemiBold)
    }
}

fun deleteEntry(s: AppState, e: Entry) {
    s.tick("reject")
    s.update { it.copy(entries = it.entries.filter { x -> x.id != e.id }) }
    s.toast("Deleted ${e.desc}", "Undo") { s.update { it.copy(entries = it.entries + e) } }
}

@Composable
fun rememberMonths(s: AppState) = remember(s.data) { s.data.monthsWithData() }
