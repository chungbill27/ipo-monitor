package com.ipomonitor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ipomonitor.data.repository.IPORepository
import com.ipomonitor.ui.MainActivity
import com.ipomonitor.util.CheckFrequency
import com.ipomonitor.util.SecurePrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val TAG = "IPOCheckWorker"
private const val CHANNEL_ID = "ipo_updates"
private const val NOTIFICATION_ID_BASE = 10000

/**
 * Background worker that:
 * 1. Checks HKEX JSON API for new IPO applications
 * 2. Stores new entries in Room DB with status = PENDING
 * 3. Shows local notification for each new discovery
 * 
 * Supports:
 * - Dynamic check frequency (15min / 30min / 1hr / 3hr / manual)
 * - Work hours only mode (Mon-Fri 9:00-18:00 HKT)
 * 
 * NOTE: Does NOT auto-analyze. User manually triggers analysis per company.
 */
@HiltWorker
class IPOCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: IPORepository,
    private val securePrefs: SecurePrefs
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Skip if not setup
        if (!securePrefs.isSetupComplete()) {
            Log.w(TAG, "Setup not complete, skipping check")
            return Result.success()
        }

        // Skip if work hours only mode is enabled and current time is outside work hours
        if (securePrefs.isWorkHoursOnly() && !isWithinWorkHours()) {
            Log.i(TAG, "Outside work hours, skipping check")
            return Result.success()
        }

        Log.i(TAG, "Starting HKEX check (attempt ${runAttemptCount + 1})")
        createNotificationChannel()

        return try {
            // Sync from HKEX - only adds new entries, does NOT analyze
            val newCount = repository.syncFromHKEX()

            if (newCount > 0) {
                Log.i(TAG, "Found $newCount new listings")
                showNotification(
                    id = NOTIFICATION_ID_BASE,
                    title = "發現 $newCount 間新入表公司",
                    body = "打開 APP 查看詳情，選擇感興趣的公司進行分析"
                )
            } else {
                Log.i(TAG, "No new listings found")
            }

            securePrefs.setLastCheckTime(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "HKEX check failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Check if current time is within HK work hours (Mon-Fri 9:00-18:00).
     */
    private fun isWithinWorkHours(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Weekend check
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }

        // Hour check (9:00 - 18:00)
        return hour in 9..17
    }

    private fun showNotification(id: Int, title: String, body: String) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IPO 入表通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "港股新股入表監控通知"
                enableLights(true)
                enableVibration(true)
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val WORK_NAME = "ipo_check_periodic"

        /**
         * Create a periodic work request with the given frequency.
         */
        fun createPeriodicRequest(frequency: CheckFrequency): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return PeriodicWorkRequestBuilder<IPOCheckWorker>(
                repeatInterval = frequency.minutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag("ipo_check")
                .build()
        }

        /**
         * Schedule periodic check with the given frequency.
         * Cancels any existing schedule and creates a new one.
         */
        fun schedule(context: Context, frequency: CheckFrequency) {
            val workManager = WorkManager.getInstance(context)

            if (frequency == CheckFrequency.MANUAL) {
                // Cancel periodic work if set to manual
                workManager.cancelUniqueWork(WORK_NAME)
                Log.i(TAG, "Periodic check cancelled (manual mode)")
                return
            }

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                createPeriodicRequest(frequency)
            )
            Log.i(TAG, "Periodic check scheduled (every ${frequency.minutes} minutes)")
        }

        /**
         * Reschedule with updated frequency. Call when user changes settings.
         */
        fun reschedule(context: Context, frequency: CheckFrequency) {
            schedule(context, frequency)
        }

        /**
         * Trigger immediate check.
         */
        fun triggerNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<IPOCheckWorker>()
                .setConstraints(constraints)
                .addTag("ipo_check_manual")
                .build()
            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "Manual check triggered")
        }
    }
}
