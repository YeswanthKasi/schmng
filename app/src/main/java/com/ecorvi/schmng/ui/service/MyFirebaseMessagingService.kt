package com.ecorvi.schmng.ui.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ecorvi.schmng.MainActivity
import com.ecorvi.schmng.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "ecorvi_channel"
        private const val CHANNEL_NAME = "Ecorvi Notifications"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToPrefs(token)
        updateTokenInFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received: ${remoteMessage.data}")

        try {
            if (remoteMessage.notification != null) {
                showNotification(
                    remoteMessage.notification?.title ?: "No Title",
                    remoteMessage.notification?.body ?: "No Message"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
        }
    }

    private fun showNotification(title: String, message: String) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ecorvilogo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Ecorvi notifications"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveTokenToPrefs(token: String) {
        try {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply()
            Log.d(TAG, "Token saved to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving token to preferences", e)
        }
    }

    private fun updateTokenInFirestore(token: String) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.d(TAG, "No user logged in, skipping token update")
                return
            }

            // Check if user wants to stay signed in
            val staySignedIn = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("stay_signed_in", false)
            
            if (!staySignedIn) {
                Log.d(TAG, "User not staying signed in, skipping token update")
                return
            }

            val tokenData = hashMapOf(
                "fcm_token" to token,
                "last_updated" to System.currentTimeMillis(),
                "platform" to "android",
                "app_version" to getAppVersion()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Token updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating token in Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating token", e)
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
