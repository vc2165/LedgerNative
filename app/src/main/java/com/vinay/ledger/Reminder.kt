package com.vinay.ledger

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vinay.ledger.data.Store
import com.vinay.ledger.data.today
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminder {
    private const val CHANNEL = "daily"
    private const val REQ = 9100

    private fun pending(ctx: Context, test: Boolean = false): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, if (test) REQ + 1 else REQ,
            Intent(ctx, ReminderReceiver::class.java).putExtra("test", test),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Schedules the next reminder (or cancels it when switched off). */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val store = Store(ctx)
        am.cancel(pending(ctx))
        if (!store.remindOn) return
        val now = LocalDateTime.now()
        var at = LocalDate.now().atTime(store.remindHour, store.remindMinute)
        if (!at.isAfter(now.plusMinutes(1))) at = at.plusDays(1)
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pending(ctx))
    }

    fun test(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10_000, pending(ctx, true))
    }

    fun show(ctx: Context, test: Boolean) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminder to log the day's expenses"
            })
        }
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_ledger)
            .setColor(0xFF5B2FC4.toInt())
            .setContentTitle("Anything to log today?")
            .setContentText(if (test) "This is a test. Your daily reminder will look like this." else "Add today's expenses to Ledger while you remember them.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(if (test) 2 else 1, n) } catch (_: SecurityException) {}
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val test = intent.getBooleanExtra("test", false)
        if (test) { Reminder.show(ctx, true); return }
        // Skip the reminder on days something is already logged
        val loggedToday = try { Store(ctx).load().entries.any { it.date == today() } } catch (_: Exception) { false }
        if (!loggedToday) Reminder.show(ctx, false)
        Reminder.schedule(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminder.schedule(ctx)
    }
}
