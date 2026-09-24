package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-register alarms after device restart or app update
            ReminderManager.rescheduleFromSettings(context)
            return
        }

        // Alarm fired: check if notifications are enabled
        val prefs = context.getSharedPreferences("one_photo_day_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminder_enabled", true)
        if (enabled) {
            ReminderManager.showNotification(
                context,
                title = "Time for your daily photo! 📸",
                message = "Capture today's memory before the day ends. Tap to open the camera."
            )
            // Reschedule for next day at configured time
            ReminderManager.rescheduleFromSettings(context)
        }
    }
}

object ReminderManager {
    const val CHANNEL_ID = "one_photo_day_reminders"
    const val NOTIFICATION_ID = 1001
    const val ALARM_REQUEST_CODE = 2001
    private const val TAG = "ReminderManager"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Photo Reminders"
            val descriptionText = "Prompts to capture your one photo of the day"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String = "Your moment is waiting 📸",
        message: String = "Take a photo and keep today's memory."
    ): Boolean {
        createNotificationChannel(context)

        // Notification content tap opens camera
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct action button
        val cameraIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
        }
        val cameraPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID + 1,
            cameraIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_camera, "📸 Snap Photo", cameraPendingIntent)

        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) {
            Log.w(TAG, "Notification permission is disabled.")
            return false
        }

        return try {
            managerCompat.notify(NOTIFICATION_ID, builder.build())
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception while showing notification", e)
            false
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled daily reminder for: ${target.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm, falling back to inexact repeating", e)
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule fallback repeating alarm", e2)
            }
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled reminder alarm")
    }

    fun rescheduleFromSettings(context: Context) {
        val prefs = context.getSharedPreferences("one_photo_day_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminder_enabled", true)
        if (enabled) {
            val hour = prefs.getInt("reminder_hour", 20)
            val minute = prefs.getInt("reminder_minute", 0)
            scheduleDailyReminder(context, hour, minute)
        } else {
            cancelReminder(context)
        }
    }
}
