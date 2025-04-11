package com.ecorvi.schmng.ui.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ecorvi.schmng.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")

        val sharedPref = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        sharedPref.edit().putString("fcm_token", token).apply()
        Log.d("FCM", "Token saved locally: $token")

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message Received: ${remoteMessage.notification?.title}")

        showNotification(
            remoteMessage.notification?.title ?: "No Title",
            remoteMessage.notification?.body ?: "No Message"
        )
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "ecorvi_channel"
        val channelName = "Ecorvi Notifications"

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ecorvilogo) // ✅ Use your actual icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        // Show notification only if permission is granted (for Android 13+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1001, builder.build())
        } else {
            Log.w("FCM", "Notification permission not granted.")
        }
    }
}
