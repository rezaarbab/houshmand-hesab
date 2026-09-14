package com.houshmandhesab.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.houshmandhesab.app.MainActivity
import com.houshmandhesab.app.R
import com.houshmandhesab.app.data.repo.WalletRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class AlertsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val wallet: WalletRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!wallet.settings.notificationsEnabled.first()) return Result.success()

        wallet.processRecurring()

        val todayDay = System.currentTimeMillis() / 86_400_000L
        if (wallet.settings.lastNotifyDay() == todayDay) return Result.success()

        val (jy, jm) = wallet.currentMonth()
        val (from, to) = wallet.monthRange(jy, jm)
        val overs = wallet.budgetProgress(from, to).first()
            .filter { it.limitAmount > 0 && it.spent >= it.limitAmount }
        val now = System.currentTimeMillis()
        val dueSoon = wallet.debtDao.dueBetween(now - 3L * 86_400_000L, now + 3L * 86_400_000L)

        if (overs.isNotEmpty()) {
            val name = overs.first().name ?: applicationContext.getString(R.string.unknown_category)
            notify(
                applicationContext.getString(R.string.notif_budget_title),
                applicationContext.getString(R.string.budget_over) + " (" + name + ")"
            )
        } else if (dueSoon.isNotEmpty()) {
            val d = dueSoon.first()
            notify(
                applicationContext.getString(R.string.notif_debt_title),
                d.person + " — " + d.amount.toString()
            )
        } else {
            return Result.success()
        }
        wallet.settings.setLastNotifyDay(todayDay)
        return Result.success()
    }

    private fun notify(title: String, text: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notification)
    }

    companion object {
        const val CHANNEL_ID = "alerts"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        fun schedule(context: Context) {
            val target = LocalTime.of(8, 30)
            var next = LocalDateTime.now().with(target)
            if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
            val delay = Duration.between(LocalDateTime.now(), next)
            val request = PeriodicWorkRequestBuilder<AlertsWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_alerts",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
