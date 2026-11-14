@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vinay.ledger.AppState
import com.vinay.ledger.MainActivity
import com.vinay.ledger.Reminder
import com.vinay.ledger.Route
import com.vinay.ledger.data.Budgets
import com.vinay.ledger.data.Cat
import com.vinay.ledger.data.Entry
import com.vinay.ledger.data.LedgerData
import com.vinay.ledger.data.NamedAmount
import com.vinay.ledger.data.PALETTE
import com.vinay.ledger.data.Plan
import com.vinay.ledger.data.RULE_BUCKET
import com.vinay.ledger.data.Bucket
import com.vinay.ledger.data.autoCat
import com.vinay.ledger.data.buildAllXlsx
import com.vinay.ledger.data.buildMonthXlsx
import com.vinay.ledger.data.inMonth
import com.vinay.ledger.data.incomeFor
import com.vinay.ledger.data.inr
import com.vinay.ledger.data.mFull
import com.vinay.ledger.data.mLong
import com.vinay.ledger.data.monthsWithData
import com.vinay.ledger.data.parseData
import com.vinay.ledger.data.parsePaste
import com.vinay.ledger.data.resolve
import com.vinay.ledger.data.thisMonth
import com.vinay.ledger.data.toJson
import com.vinay.ledger.data.today
import com.vinay.ledger.data.uid
import org.json.JSONObject
import java.io.File

private fun fmtTime(h: Int, m: Int) = "${if (h % 12 == 0) 12 else h % 12}:${"%02d".format(m)} ${if (h < 12) "AM" else "PM"}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(s: AppState, modifier: Modifier) {
    val d = s.data
    val ctx = LocalContext.current
    val act = ctx as MainActivity
    var confirmRestore by remember { mutableStateOf<LedgerData?>(null) }
    var wipe by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) try {
            ctx.contentResolver.openOutputStream(uri)?.use {
                it.write(JSONObject().put("app", "ledger").put("v", 2).put("saved", System.currentTimeMillis()).put("data", s.data.toJson()).toString().toByteArray())
            }
            s.toast("Backup saved")
        } catch (e: Exception) { s.toast("Couldn't save: ${e.message}") }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) try {
            val txt = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            val j = JSONObject(txt)
            if (j.optString("app") != "ledger" || j.optJSONObject("data") == null) s.toast("That file is not a Ledger backup")
            else confirmRestore = parseData(j.getJSONObject("data"))
        } catch (e: Exception) { s.toast("That file is not a Ledger backup") }
    }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text("Settings") }, scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer))
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        val saveTot = d.plan.saves.sumOf { it.amount }
        val bills = d.plan.billList
        LazyColumn(flingBehavior = rememberCalmFling(), Modifier.padding(top = pad.calculateTopPadding()), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard(title = "Money") {
                    NavRow("Monthly plan", "Salary ${inr(d.plan.salary)}, saving ${inr(saveTot)}" + (if (bills.isNotEmpty()) ", bills ${inr(bills.sumOf { it.amount })}" else "") + " a month",
                        Icons.Outlined.AccountBalanceWallet) { s.push(Route.PlanEdit(null)) }
                    val b = d.budgets
                    NavRow("Budgets", listOfNotNull(
                        if (b.recurring > 0) "Recurring ${inr(b.recurring)}" else null,
                        if (b.delivery > 0) "delivery ${inr(b.delivery)}" else null,
                        if (b.oneoff > 0) "one-offs ${inr(b.oneoff)}" else null,
                    ).joinToString(", ").ifEmpty { "Not set" }, Icons.Outlined.Savings) { s.push(Route.Budgets) }
                    NavRow("Categories", "${d.cats.size} categories", Icons.Outlined.Category) { s.push(Route.Categories) }
                }
            }
            item {
                SectionCard(title = "Reminders") {
                    NavRow("Daily reminder", if (s.remindOn) "On at ${fmtTime(s.remindHour, s.remindMinute)}" else "Off", Icons.Outlined.Notifications) { s.push(Route.ReminderSettings) }
                }
            }
            item {
                SectionCard(title = "Your data") {
                    NavRow("Import from Excel", "Paste rows copied from your sheet", Icons.Outlined.ContentPaste) { s.push(Route.Import) }
                    NavRow("Export to Excel", if (d.entries.isNotEmpty()) "One month or all months" else "Nothing to export yet", Icons.Outlined.TableChart) {
                        if (d.entries.isNotEmpty()) s.push(Route.Export)
                    }
                    NavRow("Save a backup", "A file you can keep in Google Drive", Icons.Outlined.Backup) {
                        act.pickingFile = true; backupLauncher.launch("ledger-backup-${today()}.json")
                    }
                    NavRow("Restore a backup", "Also moves your data over from the old Ledger app", Icons.Outlined.Restore) {
                        act.pickingFile = true; restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                    }
                }
            }
            item {
                SectionCard(title = "Feel and privacy") {
                    NavRow("Vibration", "A light tick when you tap, save or delete", Icons.Outlined.Vibration,
                        trailing = { Switch(s.haptic, { s.haptic = it; s.store.haptic = it; if (it) s.tick("confirm") }) }) {
                        s.haptic = !s.haptic; s.store.haptic = s.haptic; if (s.haptic) s.tick("confirm")
                    }
                    val toggleLock = {
                        if (s.lockOn) { s.lockOn = false; s.store.lock = false; s.toast("Fingerprint lock off") }
                        else if (!act.canAuthenticate()) s.toast("Set up a fingerprint or screen lock first")
                        else s.authenticate("Confirm it's you") { s.lockOn = true; s.store.lock = true; s.toast("Fingerprint lock on") }
                    }
                    NavRow("Fingerprint lock", "Ask for your fingerprint when Ledger opens", Icons.Outlined.Fingerprint,
                        trailing = { Switch(s.lockOn, { toggleLock() }) }) { toggleLock() }
                }
            }
            item {
                SectionCard(title = "Danger zone") {
                    NavRow("Erase all data", "Removes every expense, category change, budget and plan", Icons.Outlined.DeleteForever, titleColor = Bad) { wipe = true }
                }
            }
            item { Muted("Ledger stores everything on this phone and never connects to the internet.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
        }
    }

    confirmRestore?.let { nd ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Restore this backup?") },
            text = { Text("It contains ${nd.entries.size} expenses. Everything in Ledger now will be replaced.") },
            confirmButton = { TextButton(onClick = {
                s.replaceAll(nd); s.month = nd.monthsWithData().lastOrNull() ?: thisMonth(); confirmRestore = null; s.toast("Backup restored")
            }) { Text("Restore", color = Bad) } },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
        )
    }
    if (wipe) {
        var typed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { wipe = false },
            title = { Text("Erase all data?") },
            text = {
                Column {
                    Text("Type DELETE to remove everything from Ledger on this phone.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(typed, { typed = it }, singleLine = true, placeholder = { Text("DELETE") })
                }
            },
            confirmButton = { TextButton(enabled = typed == "DELETE", onClick = { s.replaceAll(LedgerData()); wipe = false; s.toast("All data erased") }) { Text("Erase", color = Bad) } },
            dismissButton = { TextButton(onClick = { wipe = false }) { Text("Cancel") } },
        )
    }
}

/* ---------- Monthly plan (or one month's own numbers) ---------- */

private class Line(name: String, amount: String) {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
}

private fun num(s: String) = s.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
private fun show(a: Double) = if (a == 0.0) "" else if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()

@Composable
private fun AmountLines(lines: MutableList<Line>, namePh: String) {
    lines.forEachIndexed { i, l ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledField(l.name, { l.name = it }, namePh, Modifier.weight(1.4f))
            FilledField(l.amount, { l.amount = it.replace(Regex("[^\\d.]"), "") }, "Amount", Modifier.weight(1f), numeric = true)
            IconButton(onClick = { lines.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove") }
        }
    }
}

@Composable
fun FilledField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, numeric: Boolean = false, singleLine: Boolean = true, minLines: Int = 1) {
    TextField(
        value, onChange, modifier, placeholder = { Text(placeholder) }, singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Transparent),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

@Composable
private fun SaveBar(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp, 8.dp, 16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun PlanScreen(s: AppState, k: String?) {
    val d = s.data
    val cur = remember { if (k != null) d.incomeFor(k) else d.plan }
    var salary by remember { mutableStateOf(show(cur.salary)) }
    val saves = remember { mutableStateListOf<Line>().apply { addAll(cur.saves.map { Line(it.name, show(it.amount)) }) } }
    val bills = remember { mutableStateListOf<Line>().apply { addAll(cur.billList.map { Line(it.name, show(it.amount)) }) } }
    Scaffold(
        topBar = { SubBar(if (k != null) "${mLong(k)} plan" else "Monthly plan", { s.pop() }) },
        bottomBar = {
            SaveBar {
                if (k != null && d.months.containsKey(k)) OutlinedButton(onClick = {
                    s.update { it.copy(months = it.months - k) }; s.pop(); s.toast("Using monthly plan")
                }, Modifier.weight(1f).height(52.dp)) { Text("Use monthly plan") }
                Button(onClick = {
                    val p = Plan(
                        num(salary),
                        saves.filter { it.name.isNotBlank() || num(it.amount) > 0 }.map { NamedAmount(it.name.trim(), num(it.amount)) },
                        bills.filter { it.name.isNotBlank() }.map { NamedAmount(it.name.trim(), num(it.amount)) },
                    )
                    s.update { if (k != null) it.copy(months = it.months + (k to p)) else it.copy(plan = p) }
                    s.tick("confirm"); s.pop(); s.toast("Saved")
                }, Modifier.weight(1f).height(52.dp)) { Text("Save") }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState(), flingBehavior = rememberCalmFling()).padding(horizontal = 20.dp)) {
            Muted(if (k != null) "Applies to this month only. Your usual numbers live in Settings, Monthly plan." else "Used for every month unless you change a particular month from its report.")
            FieldLabel("Salary")
            FilledField(salary, { salary = it.replace(Regex("[^\\d.]"), "") }, "Salary", Modifier.fillMaxWidth(), numeric = true)
            FieldLabel("Savings and investments")
            AmountLines(saves, "Name")
            TextButton(onClick = { saves.add(Line("", "")) }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add a saving, for example ELSS") }
            FieldLabel("Fixed bills")
            Muted("Counted from the 1st of the month until you log them. An expense whose description contains the bill's name counts as paid and is marked Fixed. Use a specific name, like \"PG rent\" or \"Physio bill\".", Modifier.padding(bottom = 8.dp))
            AmountLines(bills, "Name")
            TextButton(onClick = { bills.add(Line("", "")) }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add a bill, for example PG rent") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------- Budgets ---------- */

@Composable
fun BudgetsScreen(s: AppState) {
    val b = s.data.budgets
    var rec by remember { mutableStateOf(show(b.recurring)) }
    var del by remember { mutableStateOf(show(b.delivery)) }
    var one by remember { mutableStateOf(show(b.oneoff)) }
    Scaffold(
        topBar = { SubBar("Budgets", { s.pop() }) },
        bottomBar = {
            SaveBar {
                Button(onClick = {
                    s.update { it.copy(budgets = Budgets(num(rec), num(del), num(one))) }
                    s.tick("confirm"); s.pop(); s.toast("Budgets saved")
                }, Modifier.weight(1f).height(52.dp)) { Text("Save budgets") }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 20.dp)) {
            Muted("Monthly limits shown on the Ledger tab. Leave empty for no limit.")
            FieldLabel("Recurring spending")
            FilledField(rec, { rec = it.replace(Regex("[^\\d]"), "") }, "17000", Modifier.fillMaxWidth(), numeric = true)
            FieldLabel("Food delivery, Swiggy and Zomato")
            FilledField(del, { del = it.replace(Regex("[^\\d]"), "") }, "4000", Modifier.fillMaxWidth(), numeric = true)
            FieldLabel("One-offs")
            FilledField(one, { one = it.replace(Regex("[^\\d]"), "") }, "15000", Modifier.fillMaxWidth(), numeric = true)
        }
    }
}

/* ---------- Categories ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(s: AppState) {
    val d = s.data
    val cnt = remember(d) { d.entries.groupingBy { d.resolve(it.cat) }.eachCount() }
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { SubBar("Categories", { s.pop() }, scroll) { IconButton(onClick = { s.push(Route.CatEdit(null)) }) { Icon(Icons.Outlined.Add, "New category") } } },
    ) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard {
                    d.cats.entries.sortedBy { it.value.name.lowercase() }.forEach { (id, c) ->
                        val n = cnt[id] ?: 0
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { s.push(Route.CatEdit(id)) }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CatAvatar(d.catIcon(id), d.catColor(id), 38)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                Text("$n expense${if (n == 1) "" else "s"}" + if (c.kw.isNotEmpty()) ", matches ${c.kw.joinToString(", ")}" else "",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatEditScreen(s: AppState, id: String?) {
    val d = s.data
    val c = id?.let { d.cats[it] }
    val isNew = c == null
    var name by remember { mutableStateOf(c?.name ?: "") }
    var color by remember { mutableStateOf(c?.color ?: PALETTE[d.cats.size % PALETTE.size]) }
    var kw by remember { mutableStateOf(c?.kw?.joinToString(", ") ?: "") }
    var deleting by remember { mutableStateOf(false) }
    var moveTo by remember { mutableStateOf("other") }
    var menu by remember { mutableStateOf(false) }
    val n = remember(d) { if (id == null) 0 else d.entries.count { d.resolve(it.cat) == id } }
    Scaffold(
        topBar = { SubBar(if (isNew) "New category" else "Edit category", { s.pop() }) },
        bottomBar = {
            SaveBar {
                if (!isNew && id != "other" && !deleting) OutlinedButton(onClick = { deleting = true }, Modifier.weight(1f).height(52.dp)) { Text("Delete", color = Bad) }
                if (deleting) {
                    OutlinedButton(onClick = { deleting = false }, Modifier.weight(1f).height(52.dp)) { Text("Keep") }
                    Button(onClick = {
                        val dest = moveTo
                        val cid = id ?: return@Button
                        s.update { dd ->
                            val ents = dd.entries.map { if (dd.resolve(it.cat) == cid) it.copy(cat = dest) else it }
                            val al = dd.alias.mapValues { if (it.value == cid) dest else it.value } + Pair(cid, dest)
                            dd.copy(entries = ents, alias = al, cats = dd.cats - cid)
                        }
                        s.pop(); s.toast("Deleted, $n moved to ${s.data.cats[dest]?.name}")
                    }, Modifier.weight(1f).height(52.dp)) { Text("Delete and move") }
                } else Button(onClick = {
                    val nm = name.trim()
                    if (nm.isEmpty()) { s.toast("Give the category a name"); return@Button }
                    if (d.cats.any { (x, v) -> x != id && v.name.equals(nm, true) }) { s.toast("A category with that name exists"); return@Button }
                    val words = kw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                    val nid = id ?: ("c_" + uid())
                    s.update { it.copy(cats = it.cats + (nid to Cat(nm, color, words))) }
                    s.tick("confirm"); s.pop(); s.toast(if (isNew) "Category created" else "Category saved")
                }, Modifier.weight(1f).height(52.dp)) { Text(if (isNew) "Create category" else "Save changes") }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState(), flingBehavior = rememberCalmFling()).padding(horizontal = 20.dp)) {
            FieldLabel("Name")
            FilledField(name, { name = it }, "e.g. Gym", Modifier.fillMaxWidth())
            FieldLabel("Colour")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PALETTE.forEach { col ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(col)).clickable { color = col; s.tick("tick") }
                            .then(if (col == color) Modifier.border(3.dp, Color.White, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (col == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            FieldLabel("Auto-match words")
            FilledField(kw, { kw = it }, "e.g. uber, ola, rapido", Modifier.fillMaxWidth())
            Muted("Expenses whose description contains any of these words go into this category automatically.", Modifier.padding(top = 6.dp))
            if (deleting && id != null) {
                FieldLabel("Move its $n expense${if (n == 1) "" else "s"} to")
                Box {
                    OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text(d.cats[moveTo]?.name ?: "Other") }
                    DropdownMenu(menu, { menu = false }) {
                        d.cats.entries.filter { it.key != id }.sortedBy { it.value.name.lowercase() }.forEach { (cid, cc) ->
                            DropdownMenuItem(text = { Text(cc.name) }, onClick = { moveTo = cid; menu = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------- Daily reminder ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(s: AppState) {
    val ctx = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { s.setReminder(true); s.toast("Reminder set for ${fmtTime(s.remindHour, s.remindMinute)}") }
        else s.toast("Allow notifications for Ledger in phone settings")
    }
    fun turnOn() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) perm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else { s.setReminder(true); s.toast("Reminder set for ${fmtTime(s.remindHour, s.remindMinute)}") }
    }
    Scaffold(topBar = { SubBar("Daily reminder", { s.pop() }) }) { pad ->
        Column(Modifier.padding(pad)) {
            SectionCard {
                NavRow("Remind me to log expenses", "Skipped on days you've already logged something", Icons.Outlined.Notifications,
                    trailing = { Switch(s.remindOn, { if (it) turnOn() else { s.setReminder(false); s.toast("Reminder off") } }) }) {
                    if (!s.remindOn) turnOn() else { s.setReminder(false); s.toast("Reminder off") }
                }
                if (s.remindOn) {
                    NavRow("Time", fmtTime(s.remindHour, s.remindMinute)) { picking = true }
                    NavRow("Send a test", "Arrives in about 10 seconds", trailing = null) { Reminder.test(ctx); s.toast("Test arrives in about 10 seconds") }
                }
            }
        }
    }
    if (picking) {
        val tp = rememberTimePickerState(s.remindHour, s.remindMinute, false)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { s.setReminder(true, tp.hour, tp.minute); picking = false; s.toast("Reminder set for ${fmtTime(tp.hour, tp.minute)}") }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
            text = { TimePicker(tp) },
        )
    }
}

/* ---------- Import from Excel ---------- */

private class Prepared(val fresh: List<Entry>, val byMonth: Map<String, Pair<Int, Double>>, val dup: Int, val skipped: List<String>)

@Composable
fun ImportScreen(s: AppState) {
    var text by remember { mutableStateOf("") }
    var prep by remember { mutableStateOf<Prepared?>(null) }
    var checked by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { SubBar("Import from Excel", { s.pop() }) },
        bottomBar = {
            SaveBar {
                OutlinedButton(onClick = {
                    val d = s.data
                    val res = parsePaste(text)
                    val now = System.currentTimeMillis()
                    val rows = res.rows.mapIndexed { i, r ->
                        val c = if (r.desc == "(unlabelled)") d.resolve(r.rule) else d.autoCat(r.desc)
                        val b = RULE_BUCKET[r.rule]?.takeIf { it != "recurring" }?.let { Bucket.of(it) }
                        Entry(uid() + i, r.date.toString(), r.desc, r.amount, c, b, now + i)
                    }
                    val have = d.entries.map { "${it.date}|${it.desc.lowercase()}|${it.amount}" }.toHashSet()
                    val fresh = rows.filter { !have.contains("${it.date}|${it.desc.lowercase()}|${it.amount}") }
                    val byM = fresh.groupBy { it.date.take(7) }.mapValues { (_, l) -> l.size to l.sumOf { it.amount } }
                    prep = Prepared(fresh, byM, rows.size - fresh.size, res.skipped); checked = true
                }, Modifier.weight(1f).height(52.dp)) { Text("Check") }
                Button(enabled = (prep?.fresh?.isNotEmpty() == true), onClick = {
                    val p = prep ?: return@Button
                    s.update { it.copy(entries = it.entries + p.fresh) }
                    s.month = s.data.monthsWithData().lastOrNull() ?: s.month
                    s.pop(); s.toast("Imported ${p.fresh.size} expenses")
                }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Import") }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState(), flingBehavior = rememberCalmFling()).padding(horizontal = 20.dp)) {
            Muted("Copy the date, description and amount columns from your sheet and paste them below. A row without a date uses the date above it.")
            Spacer(Modifier.height(12.dp))
            TextField(
                text, { text = it; prep = null; checked = false }, Modifier.fillMaxWidth().heightIn(min = 170.dp),
                placeholder = { Text("1/11/26\tPG to Ofc\t₹82.00\n\tLunch at Ofc\t₹179.00", fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Transparent),
            )
            val p = prep
            if (checked && p != null) {
                Spacer(Modifier.height(14.dp))
                if (p.fresh.isEmpty() && p.dup == 0) Text("No expenses found. Copy three columns: date, description and amount.", color = Bad)
                else {
                    Table(listOf("Month", "New", "Total"), p.byMonth.keys.sorted().map { m -> listOf(mFull(m), p.byMonth[m]!!.first.toString(), inr(p.byMonth[m]!!.second)) })
                    val notes = buildString {
                        if (p.dup > 0) append("${p.dup} already in Ledger and will be skipped. ")
                        if (p.skipped.isNotEmpty()) append("${p.skipped.size} line${if (p.skipped.size > 1) "s" else ""} couldn't be read, for example: ${p.skipped[0].take(60)}")
                    }
                    if (notes.isNotEmpty()) Muted(notes, Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------- Export to Excel ---------- */

fun shareBytes(ctx: android.content.Context, bytes: ByteArray, name: String, mime: String) {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, name)
    f.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
    val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, "Save or send").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
}

private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@Composable
fun ExportScreen(s: AppState) {
    val d = s.data
    val ctx = LocalContext.current
    val ms = remember(d) { d.monthsWithData().reversed() }
    fun month(k: String) {
        if (d.inMonth(k).isEmpty()) { s.toast("No expenses in ${mFull(k)}"); return }
        (ctx as MainActivity).pickingFile = true
        shareBytes(ctx, d.buildMonthXlsx(k), "ledger-${mLong(k).lowercase()}-${k.take(4)}.xlsx", XLSX)
    }
    Scaffold(topBar = { SubBar("Export to Excel", { s.pop() }) }) { pad ->
        LazyColumn(flingBehavior = rememberCalmFling(), Modifier.padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard {
                    NavRow(mFull(thisMonth()), "${d.inMonth(thisMonth()).size} expenses, laid out like your monthly report", Icons.Outlined.TableChart) { month(thisMonth()) }
                    NavRow("All months", "${d.entries.size} expenses, with a monthly summary and category trend", Icons.Outlined.TableChart) {
                        (ctx as MainActivity).pickingFile = true
                        shareBytes(ctx, d.buildAllXlsx(), "ledger-all-${today()}.xlsx", XLSX)
                    }
                }
            }
            item { Muted("Or pick a month. Every file includes your salary, each saving and loan, bills and spending, plus a Read me sheet explaining the numbers.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
            item {
                SectionCard {
                    ms.forEach { m -> NavRow(mFull(m), "${d.inMonth(m).size} expenses") { month(m) } }
                }
            }
        }
    }
}
