package com.ipomonitor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ipomonitor.ui.MainActivity

private const val TAG = "IPONotificationService"
private const val CHANNEL_ID = "ipo_updates"
private const val CHANNEL_NAME = "IPO 更新通知"

/**
 * Local notification service for IPO monitoring.
 * Shows notifications when new IPO filings are detected by WorkManager.
 * 
 * Note: Firebase Cloud Messaging is not used in this version.
 * All monitoring is done locally via WorkManager periodic tasks.
 */
class IPONotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val companyName = intent?.getStringExtra("company_name") ?: return START_NOT_STICKY
        val recordId = intent.getIntExtra("record_id", 0)
        val status = intent.getStringExtra("status") ?: "new"

        showNotification(recordId, companyName, status)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        fun showNewIPONotification(context: Context, recordId: Int, companyName: String) {
            createNotificationChannelStatic(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("hkex_id", recordId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, recordId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("新股入表發現")
                .setContentText("發現新入表公司：$companyName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(recordId, notification)
        }

        private fun createNotificationChannelStatic(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "港股 IPO 入表監控通知"
                    enableLights(true)
                    enableVibration(true)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun showNotification(recordId: Int, companyName: String, status: String) {
        showNewIPONotification(this, recordId, companyName)
    }

    private fun createNotificationChannel() {
        createNotificationChannelStatic(this)
    }
}
