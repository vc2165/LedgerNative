@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinay.ledger.AppState
import com.vinay.ledger.Route
import com.vinay.ledger.data.Bucket
import com.vinay.ledger.data.Entry
import com.vinay.ledger.data.autoCat
import com.vinay.ledger.data.billOf
import com.vinay.ledger.data.inr
import com.vinay.ledger.data.r2
import com.vinay.ledger.data.resolve
import com.vinay.ledger.data.shortDate
import com.vinay.ledger.data.suggestions
import com.vinay.ledger.data.thisMonth
import com.vinay.ledger.data.today
import com.vinay.ledger.data.uid
import com.vinay.ledger.data.usage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun cleanAmount(v: String): String {
    var x = v.replace(Regex("[^\\d.]"), "")
    val p = x.split('.')
    if (p.size > 2) x = p[0] + "." + p.drop(1).joinToString("")
    val q = x.split('.')
    if (q.size == 2 && q[1].length > 2) x = q[0] + "." + q[1].take(2)
    return x
}

private fun fmtAmt(a: Double) = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(s: AppState, r: Route.EntryEdit) {
    val d = s.data
    val existing: Entry? = r.id?.let { id -> d.entries.firstOrNull { it.id == id } }
    val isNew = existing == null
    var nonce by remember { mutableStateOf(0) } // bumps after "Save & add next" to reset the form
    val defaultDate = remember { if (s.month == thisMonth()) today() else "${s.month}-01" }

    var amt by remember(nonce) { mutableStateOf(existing?.let { fmtAmt(it.amount) } ?: if (nonce == 0) r.preAmt?.let { fmtAmt(it) } ?: "" else "") }
    var desc by remember(nonce) { mutableStateOf(existing?.desc?.takeIf { it != "(unlabelled)" } ?: if (nonce == 0) r.preDesc ?: "" else "") }
    var cat by remember(nonce) { mutableStateOf(existing?.let { d.resolve(it.cat) } ?: r.preDesc?.takeIf { nonce == 0 }?.let { d.autoCat(it) }) }
    var catTouched by remember(nonce) { mutableStateOf(existing != null) }
    var bucket by remember(nonce) { mutableStateOf(existing?.bucket ?: if (r.preFixed && nonce == 0) Bucket.FIXED else null) }
    var date by remember { mutableStateOf(existing?.date ?: defaultDate) }
    var allCats by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    val amtFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val kb = LocalSoftwareKeyboardController.current
    LaunchedEffect(nonce) {
        if (isNew && amt.isEmpty()) {
            kotlinx.coroutines.delay(120)
            try { amtFocus.requestFocus(); kb?.show() } catch (_: Exception) {}
        }
    }
    if (cat == "other" && !catTouched) cat = null

    fun commit(): Entry? {
        val a = amt.toDoubleOrNull()
        if (a == null || a <= 0) { s.toast("Enter an amount"); try { amtFocus.requestFocus() } catch (_: Exception) {}; s.tick("reject"); return null }
        val c = cat ?: if (desc.isNotBlank()) d.autoCat(desc) else "other"
        var rec = Entry(
            existing?.id ?: uid(), date, desc.trim().ifEmpty { "(unlabelled)" }, r2(a), c, bucket,
            existing?.created?.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
        if (rec.bucket == null && s.data.billOf(rec) != null) rec = rec.copy(bucket = Bucket.FIXED)
        s.update { data -> data.copy(entries = if (existing == null) data.entries + rec else data.entries.map { if (it.id == rec.id) rec else it }) }
        s.month = rec.date.take(7)
        s.tick("confirm")
        return rec
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New expense" else "Edit expense") },
                navigationIcon = { IconButton(onClick = { s.pop() }) { Icon(Icons.Filled.Close, "Close") } },
                actions = {
                    if (existing != null) IconButton(onClick = { s.pop(); deleteEntry(s, existing) }) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = Bad)
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp, 8.dp, 16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isNew) OutlinedButton(onClick = {
                        val rec = commit() ?: return@OutlinedButton
                        s.toast("Saved ${inr(rec.amount, true)}")
                        date = rec.date; nonce++
                    }, Modifier.weight(1f).height(52.dp)) { Text("Save & add next") }
                    Button(onClick = {
                        val rec = commit() ?: return@Button
                        s.pop(); s.toast(if (isNew) "Saved ${inr(rec.amount, true)}" else "Changes saved")
                    }, Modifier.weight(1f).height(52.dp)) { Text(if (isNew) "Save" else "Save changes") }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // Amount
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("₹", fontSize = 34.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                BasicTextField(
                    value = amt,
                    onValueChange = { amt = cleanAmount(it) },
                    textStyle = BigAmount.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Start),
                    singleLine = true,
                    cursorBrush = SolidColor(Purple),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.focusRequester(amtFocus).widthIn(min = 40.dp, max = 260.dp),
                    decorationBox = { inner ->
                        if (amt.isEmpty()) Text("0", style = BigAmount.copy(color = MaterialTheme.colorScheme.outlineVariant))
                        inner()
                    },
                )
            }
            // Description
            TextField(
                value = desc,
                onValueChange = {
                    desc = it
                    if (!catTouched) { val g = if (it.isNotBlank()) d.autoCat(it) else null; cat = if (g == "other") null else g }
                },
                placeholder = { Text("What was it for?") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            // Suggestions from your history
            val sugg = remember(desc, d) { d.suggestions(desc) }
            if (sugg.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sugg.forEach { m ->
                        SuggestionChip(onClick = {
                            desc = m.desc
                            if (amt.isEmpty()) amt = fmtAmt(m.amt)
                            if (!catTouched) cat = d.resolve(m.cat)
                            s.tick("tick"); focus.clearFocus()
                        }, label = { Text("${m.desc}  ${inr(m.amt, true)}") })
                    }
                }
            }
            // Date
            FieldLabel("Date")
            val t = today()
            val y = LocalDate.now().minusDays(1).toString()
            val y2 = LocalDate.now().minusDays(2).toString()
            val custom = date != t && date != y && date != y2
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(t to "Today", y to "Yesterday", y2 to LocalDate.parse(y2).format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))).forEach { (v, l) ->
                    FilterChip(selected = date == v, onClick = { date = v; s.tick("tick") }, label = { Text(l) },
                        leadingIcon = if (date == v) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null)
                }
                FilterChip(selected = custom, onClick = { pickDate = true }, label = { Text(if (custom) shortDate(date) else "Pick date") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp)) })
            }
            // Category
            FieldLabel("Category")
            val u = remember(d) { d.usage() }
            val ids = remember(d) { d.cats.keys.sortedWith(compareByDescending<String> { u[it] ?: 0 }.thenBy { d.cats[it]!!.name.lowercase() }) }
            var show = if (allCats) ids else ids.take(8)
            cat?.let { c -> if (!show.contains(c)) show = listOf(c) + show.take(7) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                show.forEach { id ->
                    val sel = id == cat
                    FilterChip(
                        selected = sel,
                        onClick = { cat = id; catTouched = true; s.tick("tick") },
                        label = { Text(d.cats[id]!!.name) },
                        leadingIcon = {
                            if (sel) Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                            else Icon(d.catIcon(id), null, Modifier.size(18.dp), tint = d.catColor(id))
                        },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
                if (ids.size > 8) TextButton(onClick = { allCats = !allCats }) { Text(if (allCats) "Fewer" else "All categories") }
            }
            // Type: optional, tucked away like the original app. Nothing set = counts as recurring.
            var showType by remember { mutableStateOf(bucket != null) }
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(12.dp)).clickable { showType = !showType }.padding(vertical = 10.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Type: " + (bucket?.label ?: "not set"), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (showType) "Hide" else "Change", color = Purple, fontWeight = FontWeight.Medium)
            }
            androidx.compose.animation.AnimatedVisibility(showType) {
                Column {
                    val opts = listOf<Pair<Bucket?, String>>(null to "None", Bucket.FIXED to "Fixed", Bucket.ONEOFF to "One-off")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        opts.forEachIndexed { i, (b, l) ->
                            SegmentedButton(
                                selected = bucket == b,
                                onClick = { bucket = b; s.tick("tick") },
                                shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                            ) { Text(l) }
                        }
                    }
                    Muted("Optional. Expenses without a type count as recurring in reports and budgets.", Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickDate) {
        val init = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val ps = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    ps.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(ps) }
    }
}
