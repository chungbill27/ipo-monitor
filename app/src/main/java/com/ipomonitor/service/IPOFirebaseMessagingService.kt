package com.ipomonitor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ipomonitor.R
import com.ipomonitor.data.repository.IPORepository
import com.ipomonitor.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "IPOFCMService"
private const val CHANNEL_ID = "ipo_updates"
private const val CHANNEL_NAME = "IPO 更新通知"

/**
 * Firebase Cloud Messaging service that handles incoming notifications.
 * 
 * Implements the Tickle-and-Retrieve pattern:
 * 1. Receives lightweight FCM data message (record_id + company_name + status)
 * 2. Fetches full record data from backend API
 * 3. Updates local Room database
 * 4. Shows local notification to user
 * 
 * This approach avoids the 4KB FCM payload limit while ensuring
 * the app always has the latest data.
 */
@AndroidEntryPoint
class IPOFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var repository: IPORepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM token: ${token.take(20)}...")

        // Register new token with backend
        serviceScope.launch {
            repository.registerDeviceToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val recordId = data["record_id"]?.toIntOrNull()
        val companyName = data["company_name"] ?: "未知公司"
        val status = data["status"] ?: "unknown"

        if (recordId == null) {
            Log.w(TAG, "Received FCM without record_id, ignoring")
            return
        }

        Log.i(TAG, "Processing: $companyName (id=$recordId, status=$status)")

        // Fetch full record from backend and update local DB
        serviceScope.launch {
            val result = repository.fetchAndUpdateRecord(recordId)
            result.fold(
                onSuccess = { entity ->
                    Log.i(TAG, "Record updated successfully: ${entity.companyNameZh}")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to fetch record: ${error.message}")
                }
            )
        }

        // Show notification to user
        showNotification(recordId, companyName, status)
    }

    private fun showNotification(recordId: Int, companyName: String, status: String) {
        val (title, body) = when (status) {
            "success" -> "新股解析完成" to "$companyName 的招股書已解析完畢，點擊查看詳情"
            "failed" -> "新股解析失敗" to "$companyName 的招股書解析失敗，請手動重試"
            "timeout" -> "新股解析超時" to "$companyName 的招股書解析超時，請稍後重試"
            else -> "新股入表發現" to "發現新入表公司：$companyName"
        }

        // Create intent to open app with specific record
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("hkex_id", recordId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, recordId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with custom icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(recordId, notification)
    }

    private fun createNotificationChannel() {
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

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
