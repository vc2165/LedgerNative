@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vinay.ledger.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vinay.ledger.AppState
import com.vinay.ledger.Route
import com.vinay.ledger.data.Bucket
import com.vinay.ledger.data.Entry
import com.vinay.ledger.data.inr
import com.vinay.ledger.data.mLong
import com.vinay.ledger.data.pct
import com.vinay.ledger.data.stats

/** Where this month's salary is going: savings and loan, bills, and every expense. */
@Composable
fun MoneyOutScreen(s: AppState, k: String) {
    val d = s.data
    val st = remember(d, k) { d.stats(k) }
    val pending = st.pendingList.sumOf { it.amount }
    val out = st.total + st.saved + pending
    val salary = st.inc.salary
    val es = remember(st) { st.es.sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.created }) }
    Scaffold(topBar = { SubBar("Going out in ${mLong(k)}", { s.pop() }) }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), modifier = Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard {
                    KvRow("Salary", inr(salary))
                    KvRow("Total going out", inr(out), bold = true, sub = if (salary > 0) pct(out, salary) else null)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    KvRow("Left after savings" + if (st.bills.isNotEmpty()) " and bills" else "", inr(st.net), bold = true, valueColor = if (st.net < 0) Bad else Good)
                }
            }
            item {
                SectionCard(title = "Savings and loan") {
                    if (st.inc.saves.isEmpty()) Muted("No savings set. Add them in Settings, Monthly plan.")
                    st.inc.saves.forEach { KvRow(it.name, inr(it.amount), sub = if (salary > 0) pct(it.amount, salary) else null) }
                    if (st.inc.saves.size > 1) KvRow("Total saved", inr(st.saved), bold = true)
                    TextButton(onClick = { s.push(Route.PlanEdit(k)) }) { Text("Change for ${mLong(k)}") }
                }
            }
            if (st.pendingList.isNotEmpty()) item {
                SectionCard(title = "Bills not paid yet") {
                    st.pendingList.forEach { KvRow(it.name, inr(it.amount)) }
                    Muted("Counted until you log them as an expense.")
                }
            }
            item {
                SectionCard(title = "Spent") {
                    Bucket.entries.forEach { b ->
                        KvRow(b.label, inr(st.b[b]!!), dot = BucketColor[b], sub = pct(st.b[b]!!, st.total)) { s.push(Route.TypeList(k, b)) }
                    }
                    KvRow("Total spent", inr(st.total, true), bold = true)
                }
            }
            if (es.isNotEmpty()) {
                item { Text("All ${es.size} expenses", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp)) }
                items(es, key = { it.id }) { e ->
                    Box(Modifier.padding(horizontal = 16.dp)) { EntryRow(s, e, showDate = true) { s.push(Route.EntryEdit(e.id)) } }
                }
            }
        }
    }
}
