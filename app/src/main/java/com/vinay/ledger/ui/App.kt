@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vinay.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vinay.ledger.AppState
import com.vinay.ledger.Route
import com.vinay.ledger.Tab

private data class TabDef(val tab: Tab, val label: String, val on: ImageVector, val off: ImageVector)

private val TABS = listOf(
    TabDef(Tab.LOG, "Ledger", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    TabDef(Tab.REPORT, "Report", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    TabDef(Tab.TREND, "Trend", Icons.Filled.Insights, Icons.Outlined.Insights),
    TabDef(Tab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun LedgerApp(s: AppState, locked: Boolean, onUnlock: () -> Unit) {
    if (locked) { LockScreen(onUnlock); return }

    BackHandler(enabled = s.stack.isNotEmpty()) { s.pop() }
    BackHandler(enabled = s.stack.isEmpty() && s.tab != Tab.LOG) { s.tab = Tab.LOG }

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.LocalOverscrollConfiguration provides null) {
    Box(Modifier.fillMaxSize()) {
        MainTabs(s)
        // Sub-screens slide in over the tabs, like a native navigation stack
        AnimatedContent(
            targetState = s.stack.size to s.stack.lastOrNull(),
            transitionSpec = {
                // like the original app's bottom sheets: slide up to open, slide down to close
                if (targetState.first > initialState.first)
                    ((slideInVertically(tween(300, easing = EaseSheet)) { it / 6 } + fadeIn(tween(220))) togetherWith ExitTransition.KeepUntilTransitionsFinished)
                        .apply { targetContentZIndex = 1f }
                else
                    (EnterTransition.None togetherWith (slideOutVertically(tween(200, easing = EaseSheetOut)) { it / 6 } + fadeOut(tween(180))))
                        .apply { targetContentZIndex = -1f }
            },
            contentKey = { it.first },
            label = "routes",
        ) { (_, route) ->
            if (route != null) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { RouteScreen(s, route) }
            } else Box(Modifier)
        }
    }
    }
}

@Composable
private fun RouteScreen(s: AppState, r: Route) {
    when (r) {
        is Route.EntryEdit -> EntryScreen(s, r)
        Route.Search -> SearchScreen(s)
        is Route.MoneyOut -> MoneyOutScreen(s, r.month)
        is Route.TypeList -> TypeListScreen(s, r.month, r.bucket)
        is Route.PlanEdit -> PlanScreen(s, r.month)
        Route.Budgets -> BudgetsScreen(s)
        Route.Categories -> CategoriesScreen(s)
        is Route.CatEdit -> CatEditScreen(s, r.id)
        Route.ReminderSettings -> ReminderScreen(s)
        Route.Import -> ImportScreen(s)
        Route.Export -> ExportScreen(s)
    }
}

@Composable
private fun MainTabs(s: AppState) {
    var fabExpanded by remember { mutableStateOf(true) }
    val shift = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.roundToPx() }
    Scaffold(
        snackbarHost = { SnackbarHost(s.snackbar) },
        floatingActionButton = {
            AnimatedVisibility(s.tab == Tab.LOG, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                ExtendedFloatingActionButton(
                    text = { Text("Add expense") },
                    icon = { Icon(Icons.Filled.Add, null) },
                    expanded = fabExpanded,
                    onClick = { s.tick(); s.push(Route.EntryEdit(null)) },
                    containerColor = Purple,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = s.tab == t.tab,
                        onClick = { if (s.tab != t.tab) { s.tick("tick"); s.tab = t.tab } },
                        icon = { Icon(if (s.tab == t.tab) t.on else t.off, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        val mod = Modifier.padding(bottom = pad.calculateBottomPadding())
        AnimatedContent(
            targetState = s.tab,
            transitionSpec = {
                val fwd = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(260, easing = EaseStd)) { if (fwd) shift else -shift } + fadeIn(tween(260, easing = EaseStd))) togetherWith fadeOut(tween(90))
            },
            label = "tabs",
        ) { tab ->
            when (tab) {
                Tab.LOG -> LogScreen(s, mod) { fabExpanded = it }
                Tab.REPORT -> ReportScreen(s, mod)
                Tab.TREND -> TrendScreen(s, mod)
                Tab.SETTINGS -> SettingsScreen(s, mod)
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Fingerprint, null, tint = Purple, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Ledger is locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Unlock") }
        }
    }
}
