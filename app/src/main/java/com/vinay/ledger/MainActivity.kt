package com.vinay.ledger

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vinay.ledger.data.LedgerData
import com.vinay.ledger.data.Store
import com.vinay.ledger.data.thisMonth
import com.vinay.ledger.ui.LedgerApp
import com.vinay.ledger.ui.LedgerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class Tab { LOG, REPORT, TREND, SETTINGS }

sealed interface Route {
    data class EntryEdit(val id: String?, val preDesc: String? = null, val preAmt: Double? = null, val preFixed: Boolean = false) : Route
    data object Search : Route
    data class MoneyOut(val month: String) : Route
    data class TypeList(val month: String, val bucket: com.vinay.ledger.data.Bucket) : Route
    data class PlanEdit(val month: String?) : Route
    data object Budgets : Route
    data object Categories : Route
    data class CatEdit(val id: String?) : Route
    data object ReminderSettings : Route
    data object Import : Route
    data object Export : Route
}

/** All app state lives here; every change is saved straight away. */
class AppState(val store: Store, private val activity: MainActivity) {
    var data by mutableStateOf(store.load())
        private set
    var tab by mutableStateOf(Tab.LOG)
    var month by mutableStateOf(thisMonth())
    val stack = mutableStateListOf<Route>()
    val snackbar = SnackbarHostState()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var haptic by mutableStateOf(store.haptic)
    var lockOn by mutableStateOf(store.lock)
    var remindOn by mutableStateOf(store.remindOn)
    var remindHour by mutableStateOf(store.remindHour)
    var remindMinute by mutableStateOf(store.remindMinute)

    fun update(f: (LedgerData) -> LedgerData) {
        data = f(data)
        store.save(data)
    }

    fun replaceAll(d: LedgerData) { data = d; store.save(d) }

    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) }

    fun toast(msg: String, action: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(msg, action, withDismissAction = false, duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun tick(kind: String = "tap", view: View? = null) {
        if (!haptic) return
        val v = view ?: activity.window.decorView
        val c = when (kind) {
            "tick" -> HapticFeedbackConstants.CLOCK_TICK
            "confirm" -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            "reject" -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            else -> HapticFeedbackConstants.CONTEXT_CLICK
        }
        v.performHapticFeedback(c)
    }

    fun setReminder(on: Boolean, hour: Int = remindHour, minute: Int = remindMinute) {
        remindOn = on; remindHour = hour; remindMinute = minute
        store.remindOn = on; store.remindHour = hour; store.remindMinute = minute
        Reminder.schedule(activity)
    }

    fun authenticate(title: String, onOk: () -> Unit) = activity.authenticate(title, onOk)
}

class MainActivity : FragmentActivity() {
    lateinit var state: AppState
    private var locked by mutableStateOf(false)
    private var hiddenAt = 0L
    var pickingFile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        state = AppState(Store(this), this)
        locked = state.lockOn
        Reminder.schedule(this)
        setContent {
            LedgerTheme {
                LedgerApp(state, locked = locked, onUnlock = { unlock() })
            }
        }
        if (locked) window.decorView.post { unlock() }
    }

    override fun onStop() {
        super.onStop()
        hiddenAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (hiddenAt > 0 && state.lockOn && !pickingFile && SystemClock.elapsedRealtime() - hiddenAt > 60_000) {
            locked = true
            state.stack.clear()
            window.decorView.post { unlock() }
        }
        pickingFile = false
    }

    private fun unlock() = authenticate("Unlock Ledger") { locked = false }

    fun canAuthenticate(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators()) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticators() =
        if (Build.VERSION.SDK_INT >= 30) BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        else BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun authenticate(title: String, onOk: () -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(authenticators()).apply {
            if (Build.VERSION.SDK_INT < 30) setNegativeButtonText("Cancel")
        }.build()
        try { prompt.authenticate(info) } catch (e: Exception) { state.toast("Fingerprint isn't available: ${e.message}") }
    }
}
