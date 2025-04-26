package com.ecorvi.schmng.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ecorvi.schmng.MainActivity
import com.ecorvi.schmng.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class MessagingService : FirebaseMessagingService() {
    private val TAG = "MessagingService"
    private val notificationId = AtomicInteger(0)

    companion object {
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        updateTokenInFirestore(token)
    }

    private fun updateTokenInFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "No user logged in, token update skipped")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(user.uid)

        // Update token with server timestamp
        val updates = hashMapOf<String, Any>(
            "fcmToken" to token,
            "tokenUpdatedAt" to FieldValue.serverTimestamp(),
            "lastActive" to FieldValue.serverTimestamp()
        )

        userDoc.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token successfully updated in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating FCM token in Firestore", e)
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message notification: ${notification.body}")
            sendNotification(
                title = notification.title ?: getString(R.string.app_name),
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        when (data["type"]) {
            "announcement" -> handleAnnouncement(data)
            "schedule_update" -> handleScheduleUpdate(data)
            "fee_reminder" -> handleFeeReminder(data)
            else -> {
                // Default handling - show as notification
                sendNotification(
                    title = data["title"] ?: getString(R.string.app_name),
                    body = data["message"] ?: "",
                    data = data
                )
            }
        }
    }

    private fun handleAnnouncement(data: Map<String, String>) {
        sendNotification(
            title = "New Announcement",
            body = data["message"] ?: "",
            data = data,
            channelId = "announcements",
            channelName = "Announcements",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    private fun handleScheduleUpdate(data: Map<String, String>) {
        sendNotification(
            title = "Schedule Update",
            body = data["message"] ?: "",
            data = data,
            channelId = "schedule_updates",
            channelName = "Schedule Updates",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun handleFeeReminder(data: Map<String, String>) {
        sendNotification(
            title = "Fee Reminder",
            body = data["message"] ?: "",
            data = data,
            channelId = "fee_reminders",
            channelName = "Fee Reminders",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Default notification channel"
            }

            val announcementsChannel = NotificationChannel(
                "announcements",
                "Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important school announcements"
            }

            val scheduleChannel = NotificationChannel(
                "schedule_updates",
                "Schedule Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates to class schedules"
            }

            val feeChannel = NotificationChannel(
                "fee_reminders",
                "Fee Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fee payment reminders"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(
                defaultChannel,
                announcementsChannel,
                scheduleChannel,
                feeChannel
            ))
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_ID,
        channelName: String = CHANNEL_NAME,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ecorvilogo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        // Create channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId.incrementAndGet(), notificationBuilder.build())

        // Update user's last active timestamp
        updateUserLastActive()
    }

    private fun updateUserLastActive() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .update("lastActive", FieldValue.serverTimestamp())
            } catch (e: Exception) {
                Log.e(TAG, "Error updating last active timestamp", e)
            }
        }
    }
} 